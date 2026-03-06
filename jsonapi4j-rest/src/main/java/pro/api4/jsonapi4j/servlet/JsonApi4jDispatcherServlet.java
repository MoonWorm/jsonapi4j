package pro.api4.jsonapi4j.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.JsonApi4j;
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
import java.nio.charset.StandardCharsets;

import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.*;

@Slf4j
public class JsonApi4jDispatcherServlet extends HttpServlet {

    private JsonApi4j jsonApi4j;

    private ErrorHandlerFactoriesRegistry errorHandlerFactory;
    private ObjectMapper objectMapper;

    private JsonApiRequestSupplier<HttpServletRequest> jsonApiRequestSupplier;

    @Override
    public void init(ServletConfig config) throws ServletException {
        log.info("Initializing {} ...", JsonApi4jDispatcherServlet.class.getSimpleName());
        super.init(config);

        jsonApi4j = (JsonApi4j) config.getServletContext().getAttribute(JSONAPI4J_ATT_NAME);
        Validate.notNull(jsonApi4j);
        log.info("Applied {} from Servlet Context under {} attribute", JsonApi4j.class.getSimpleName(), JSONAPI4J_ATT_NAME);

        errorHandlerFactory = (ErrorHandlerFactoriesRegistry) config.getServletContext().getAttribute(ERROR_HANDLER_FACTORIES_REGISTRY_ATT_NAME);
        if (errorHandlerFactory == null) {
            log.info("AggregatableErrorHandlerFactory not found in servlet context. Applying a default ErrorHandlerFactory.");
            errorHandlerFactory = initDefaultErrorHandlerFactory();
        } else {
            log.info("Applied {} from Servlet Context under {} attribute", ErrorHandlerFactoriesRegistry.class.getSimpleName(), ERROR_HANDLER_FACTORIES_REGISTRY_ATT_NAME);
        }

        objectMapper = initObjectMapper(config.getServletContext());
        Validate.notNull(objectMapper);

        OperationDetailsResolver operationDetailsResolver = new OperationDetailsResolver(
                jsonApi4j.getDomainRegistry()
        );
        jsonApiRequestSupplier = new HttpServletRequestJsonApiRequestSupplier(
                objectMapper,
                operationDetailsResolver
        );
        log.info("{} has been successfully composed", OperationDetailsResolver.class.getSimpleName());
        log.info("{} has been initialized", JsonApi4jDispatcherServlet.class.getSimpleName());
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
            log.info("Setting response status code: {}", status);

            if (targetOperationType == OperationType.CREATE_RESOURCE) {
                SingleResourceDoc<?> singleResourceDoc = (SingleResourceDoc<?>) dataDoc;
                if (singleResourceDoc != null) {
                    String location = URI.create("/" + targetResourceType.getType() + "/" + singleResourceDoc.getData().getId()).toString();
                    resp.setHeader("Location", location);
                    log.info("Setting HTTP Location header: {}", location);
                }
            }

            writeResponseBody(resp, dataDoc);

            CacheControlPropagator.propagateCacheControlIfNeeded(resp);

        } catch (Exception e) {
            if (errorHandlerFactory != null) {
                int errorStatusCode = errorHandlerFactory.resolveStatusCode(e);
                ErrorsDoc errorsDoc = errorHandlerFactory.resolveErrorsDoc(e);
                log.error("{}. Error message: {}", errorStatusCode + " code", e.getMessage());
                log.warn("Stacktrace: ", e);

                resp.setStatus(errorStatusCode);
                writeResponseBody(resp, errorsDoc);
            }
            throw e;
        }
    }

    private void writeResponseBody(HttpServletResponse resp, Object body) {
        try {
            if (body != null) {
                resp.setContentType(JsonApiMediaType.MEDIA_TYPE);
                log.info("Setting response Content-Type to: {}", JsonApiMediaType.MEDIA_TYPE);
                resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
                log.info("Writing response body: {}", body);
                objectMapper.writeValue(resp.getOutputStream(), body);
            }
        } catch (IOException e) {
            log.error("Error writing JSON into HttpServletResponse. ", e);
        }
    }

}
