package pro.api4.jsonapi4j.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.compatibility.JsonApi4jCompatibilityMode;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.request.JsonApiMediaType;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.request.JsonApiRequestSupplier;
import pro.api4.jsonapi4j.servlet.request.HttpServletRequestJsonApiRequestSupplier;
import pro.api4.jsonapi4j.servlet.request.OperationDetailsResolver;
import pro.api4.jsonapi4j.servlet.response.cache.CacheControlPropagator;
import pro.api4.jsonapi4j.servlet.response.errorhandling.ErrorHandlerFactoriesRegistry;
import pro.api4.jsonapi4j.servlet.response.errorhandling.JsonApi4jErrorHandlerFactoriesRegistry;
import pro.api4.jsonapi4j.servlet.response.errorhandling.impl.DefaultErrorHandlerFactory;
import pro.api4.jsonapi4j.servlet.response.errorhandling.impl.Jsr380ErrorHandlers;

import java.io.IOException;
import java.net.URI;

import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.*;

public class JsonApi4jDispatcherServlet extends HttpServlet {

    private final static Logger LOG = LoggerFactory.getLogger(JsonApi4jDispatcherServlet.class);

    private JsonApi4j jsonApi4j;

    private ErrorHandlerFactoriesRegistry errorHandlerFactory;
    private ObjectMapper objectMapper;

    private JsonApiRequestSupplier<HttpServletRequest> jsonApiRequestSupplier;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        jsonApi4j = (JsonApi4j) config.getServletContext().getAttribute(JSONAPI4J_ATT_NAME);
        Validate.notNull(jsonApi4j);
        JsonApi4jCompatibilityMode compatibilityMode = resolveCompatibilityMode(config);
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
                compatibilityMode
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

        try {
            JsonApiRequest jsonApiRequest = jsonApiRequestSupplier.from(req);
            OperationType targetOperationType = jsonApiRequest.getOperationType();
            ResourceType targetResourceType = jsonApiRequest.getTargetResourceType();

            Object dataDoc = jsonApi4j.execute(jsonApiRequest);

            int status = targetOperationType.getHttpStatus();
            resp.setStatus(status);
            LOG.info("Setting response status code: {}", status);

            if (targetOperationType == OperationType.CREATE_RESOURCE) {
                SingleResourceDoc<?> singleResourceDoc = (SingleResourceDoc<?>) dataDoc;
                if (singleResourceDoc != null) {
                    String location = URI.create("/" + targetResourceType.getType() + "/" + singleResourceDoc.getData().getId()).toString();
                    resp.setHeader("Location", location);
                    LOG.info("Setting HTTP Location header: {}", location);
                }
            }

            writeResponseBody(resp, dataDoc);

            CacheControlPropagator.propagateCacheControlIfNeeded(resp);

        } catch (Exception e) {
            if (errorHandlerFactory != null) {
                int errorStatusCode = errorHandlerFactory.resolveStatusCode(e);
                ErrorsDoc errorsDoc = errorHandlerFactory.resolveErrorsDoc(e);
                LOG.error("{}. Error message: {}", errorStatusCode + " code", e.getMessage());
                LOG.warn("Stacktrace: ", e);

                resp.setStatus(errorStatusCode);
                writeResponseBody(resp, errorsDoc);
            }
            throw e;
        }
    }

    private void writeResponseBody(HttpServletResponse resp, Object body) {
        try {
            if (body != null) {
                resp.setHeader("Content-Type", JsonApiMediaType.MEDIA_TYPE);
                LOG.info("Setting response Content-Type to: {}", JsonApiMediaType.MEDIA_TYPE);
                LOG.info("Writing response body: {}", body);
                objectMapper.writeValue(resp.getOutputStream(), body);
            }
        } catch (IOException e) {
            LOG.error("Error writing JSON into HttpServletResponse. ", e);
        }
    }

    private JsonApi4jCompatibilityMode resolveCompatibilityMode(ServletConfig config) {
        JsonApi4jProperties properties = (JsonApi4jProperties) config.getServletContext().getAttribute(JSONAPI4J_PROPERTIES_ATT_NAME);
        if (properties == null || properties.getCompatibility() == null) {
            return JsonApi4jCompatibilityMode.STRICT;
        }
        return properties.getCompatibility().resolveMode();
    }

}
