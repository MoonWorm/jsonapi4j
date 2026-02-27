package pro.api4.jsonapi4j.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.compatibility.JsonApi4jCompatibilityMode;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;
import pro.api4.jsonapi4j.operation.OperationHttpStatusResolver;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.request.JsonApiMediaType;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.request.JsonApiRequestSupplier;
import pro.api4.jsonapi4j.servlet.request.HttpServletRequestJsonApiRequestSupplier;
import pro.api4.jsonapi4j.servlet.request.OperationDetailsResolver;
import pro.api4.jsonapi4j.servlet.response.cache.CacheControlPropagator;
import pro.api4.jsonapi4j.servlet.response.SparseFieldsetsResponseFilter;
import pro.api4.jsonapi4j.servlet.response.errorhandling.ErrorHandlerFactoriesRegistry;
import pro.api4.jsonapi4j.servlet.response.errorhandling.JsonApi4jErrorHandlerFactoriesRegistry;
import pro.api4.jsonapi4j.servlet.response.errorhandling.impl.DefaultErrorHandlerFactory;
import pro.api4.jsonapi4j.servlet.response.errorhandling.impl.Jsr380ErrorHandlers;

import java.io.IOException;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.*;

public class JsonApi4jDispatcherServlet extends HttpServlet {

    private final static Logger LOG = LoggerFactory.getLogger(JsonApi4jDispatcherServlet.class);
    private final static int MAX_DEBUG_BODY_LOG_CHARS = 512;

    private JsonApi4j jsonApi4j;

    private ErrorHandlerFactoriesRegistry errorHandlerFactory;
    private ObjectMapper objectMapper;

    private JsonApiRequestSupplier<HttpServletRequest> jsonApiRequestSupplier;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        jsonApi4j = (JsonApi4j) config.getServletContext().getAttribute(JSONAPI4J_ATT_NAME);
        Validate.notNull(jsonApi4j);
        JsonApi4jProperties properties = resolveProperties(config);
        JsonApi4jCompatibilityMode compatibilityMode = resolveCompatibilityMode(properties);
        Set<String> supportedExtensions = resolveSupportedExtensions(properties);
        Set<String> supportedProfiles = resolveSupportedProfiles(properties);
        jsonApi4j = jsonApi4j.withCompatibilityMode(compatibilityMode);

        errorHandlerFactory = (ErrorHandlerFactoriesRegistry) config.getServletContext().getAttribute(ERROR_HANDLER_FACTORY_ATT_NAME);
        if (errorHandlerFactory == null) {
            LOG.info("AggregatableErrorHandlerFactory not found in servlet context. Setting a default ErrorHandlerFactory.");
            errorHandlerFactory = initDefaultErrorHandlerFactory();
        }

        objectMapper = (ObjectMapper) config.getServletContext().getAttribute(OBJECT_MAPPER_ATT_NAME);
        Validate.notNull(objectMapper);

        OperationDetailsResolver operationDetailsResolver = new OperationDetailsResolver(
                jsonApi4j.getDomainRegistry()
        );
        jsonApiRequestSupplier = new HttpServletRequestJsonApiRequestSupplier(
                objectMapper,
                operationDetailsResolver,
                compatibilityMode,
                supportedExtensions,
                supportedProfiles
        );
    }

    private ErrorHandlerFactoriesRegistry initDefaultErrorHandlerFactory() {
        ErrorHandlerFactoriesRegistry errorHandlerFactoriesRegistry = new JsonApi4jErrorHandlerFactoriesRegistry();
        errorHandlerFactoriesRegistry.registerAll(new DefaultErrorHandlerFactory());
        errorHandlerFactoriesRegistry.registerAll(new Jsr380ErrorHandlers());
        return errorHandlerFactoriesRegistry;
    }

    @Override
    protected void service(HttpServletRequest req,
                           HttpServletResponse resp) {
        long startedAtNanos = System.nanoTime();
        String requestId = resolveRequestId(req);
        String requestMethod = req == null ? "n/a" : req.getMethod();
        String requestPath = resolveRequestPath(req);
        int responseStatusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

        try {
            JsonApiRequest jsonApiRequest = jsonApiRequestSupplier.from(req);
            OperationType targetOperationType = jsonApiRequest.getOperationType();
            ResourceType targetResourceType = jsonApiRequest.getTargetResourceType();

            Object dataDoc = jsonApi4j.execute(jsonApiRequest);

            int status = OperationHttpStatusResolver.resolveSuccessStatus(
                    targetOperationType,
                    jsonApi4j.getCompatibilityMode()
            );
            resp.setStatus(status);
            responseStatusCode = status;

            if (targetOperationType == OperationType.CREATE_RESOURCE) {
                SingleResourceDoc<?> singleResourceDoc = (SingleResourceDoc<?>) dataDoc;
                if (singleResourceDoc != null) {
                    String location = URI.create("/" + targetResourceType.getType() + "/" + singleResourceDoc.getData().getId()).toString();
                    resp.setHeader("Location", location);
                    LOG.debug("Setting HTTP Location header: {}", location);
                }
            }

            Object responseBody = SparseFieldsetsResponseFilter.apply(
                    dataDoc,
                    jsonApiRequest.getSparseFieldsets(),
                    objectMapper
            );
            writeResponseBody(resp, responseBody);

            CacheControlPropagator.propagateCacheControlIfNeeded(resp);
            responseStatusCode = resolveStatusCode(resp, status);

        } catch (Exception e) {
            if (errorHandlerFactory != null) {
                int errorStatusCode = errorHandlerFactory.resolveStatusCode(e);
                ErrorsDoc errorsDoc = errorHandlerFactory.resolveErrorsDoc(e);
                responseStatusCode = errorStatusCode;
                logHandledException(
                        e,
                        requestId,
                        requestMethod,
                        requestPath,
                        errorStatusCode
                );

                resp.setStatus(errorStatusCode);
                writeResponseBody(resp, errorsDoc);
                return;
            }
            LOG.error(
                    "JSON:API request failed without a registered error handler. requestId={}, method={}, path={}",
                    requestId,
                    requestMethod,
                    requestPath,
                    e
            );
            throw e;
        } finally {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
            int resolvedStatusCode = resolveStatusCode(resp, responseStatusCode);
            LOG.info(
                    "Completed JSON:API request. requestId={}, method={}, path={}, status={}, durationMs={}",
                    requestId,
                    requestMethod,
                    requestPath,
                    resolvedStatusCode,
                    durationMs
            );
        }
    }

    private void writeResponseBody(HttpServletResponse resp, Object body) {
        try {
            if (body != null) {
                resp.setHeader("Content-Type", JsonApiMediaType.MEDIA_TYPE);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Writing response body (redacted): {}", redactAndTruncateBody(body));
                }
                objectMapper.writeValue(resp.getOutputStream(), body);
            }
        } catch (IOException e) {
            LOG.error("Error writing JSON into HttpServletResponse. ", e);
        }
    }

    private int resolveStatusCode(HttpServletResponse resp,
                                  int fallbackStatusCode) {
        if (resp == null) {
            return fallbackStatusCode;
        }
        int effectiveStatusCode = resp.getStatus();
        return effectiveStatusCode > 0 ? effectiveStatusCode : fallbackStatusCode;
    }

    private String resolveRequestId(HttpServletRequest request) {
        if (request == null) {
            return "n/a";
        }
        String requestId = request.getHeader("X-Request-Id");
        if (StringUtils.isBlank(requestId)) {
            requestId = request.getHeader("X-Correlation-Id");
        }
        return StringUtils.isBlank(requestId) ? "n/a" : requestId;
    }

    private String resolveRequestPath(HttpServletRequest request) {
        if (request == null) {
            return "n/a";
        }
        String requestPath = request.getRequestURI();
        if (StringUtils.isBlank(requestPath)) {
            return "/";
        }
        return requestPath;
    }

    private String redactAndTruncateBody(Object body) {
        try {
            String serializedBody = objectMapper.writeValueAsString(body);
            String redactedBody = serializedBody.replaceAll(
                    "(?i)(\"(?:password|passwd|secret|token|authorization|apiKey|creditCardNumber|ssn)\"\\s*:\\s*\")[^\"]*(\")",
                    "$1***$2"
            );
            if (redactedBody.length() > MAX_DEBUG_BODY_LOG_CHARS) {
                return redactedBody.substring(0, MAX_DEBUG_BODY_LOG_CHARS) + "...(truncated)";
            }
            return redactedBody;
        } catch (Exception e) {
            return body == null ? "null" : body.getClass().getName();
        }
    }

    private void logHandledException(Exception exception,
                                     String requestId,
                                     String requestMethod,
                                     String requestPath,
                                     int statusCode) {
        if (statusCode >= 500) {
            LOG.error(
                    "Handled JSON:API server error. requestId={}, method={}, path={}, status={}, message={}",
                    requestId,
                    requestMethod,
                    requestPath,
                    statusCode,
                    exception == null ? null : exception.getMessage(),
                    exception
            );
            return;
        }
        LOG.warn(
                "Handled JSON:API client error. requestId={}, method={}, path={}, status={}, message={}",
                requestId,
                requestMethod,
                requestPath,
                statusCode,
                exception == null ? null : exception.getMessage()
        );
    }

    private JsonApi4jProperties resolveProperties(ServletConfig config) {
        return (JsonApi4jProperties) config.getServletContext().getAttribute(JSONAPI4J_PROPERTIES_ATT_NAME);
    }

    private JsonApi4jCompatibilityMode resolveCompatibilityMode(JsonApi4jProperties properties) {
        if (properties == null || properties.getCompatibility() == null) {
            return JsonApi4jCompatibilityMode.STRICT;
        }
        return properties.getCompatibility().resolveMode();
    }

    private Set<String> resolveSupportedExtensions(JsonApi4jProperties properties) {
        if (properties == null || properties.getCompatibility() == null) {
            return Set.of();
        }
        return properties.getCompatibility().resolveSupportedExtensions();
    }

    private Set<String> resolveSupportedProfiles(JsonApi4jProperties properties) {
        if (properties == null || properties.getCompatibility() == null) {
            return Set.of();
        }
        return properties.getCompatibility().resolveSupportedProfiles();
    }

}
