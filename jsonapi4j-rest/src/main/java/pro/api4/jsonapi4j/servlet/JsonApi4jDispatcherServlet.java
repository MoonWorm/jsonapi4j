package pro.api4.jsonapi4j.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.servlet.request.OperationDetailsResolver;
import pro.api4.jsonapi4j.servlet.response.errorhandling.ErrorHandlerFactoriesRegistry;
import pro.api4.jsonapi4j.ac.AccessControlEvaluator;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.request.JsonApiMediaType;
import pro.api4.jsonapi4j.servlet.request.HttpServletRequestJsonApiRequestSupplier;
import pro.api4.jsonapi4j.request.JsonApiRequestSupplier;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.servlet.response.cache.CacheControlPropagator;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;

public class JsonApi4jDispatcherServlet extends HttpServlet {

    private final static Logger LOG = LoggerFactory.getLogger(JsonApi4jDispatcherServlet.class);

    private final JsonApi4j jsonApi4j;

    private final ErrorHandlerFactoriesRegistry errorHandlerFactory;
    private final ObjectMapper objectMapper;

    private final JsonApiRequestSupplier<HttpServletRequest> jsonApiRequestSupplier;

    public JsonApi4jDispatcherServlet(DomainRegistry domainRegistry,
                                      OperationsRegistry operationsRegistry,
                                      AccessControlEvaluator accessControlEvaluator,
                                      ExecutorService executorService,
                                      ErrorHandlerFactoriesRegistry errorHandlerFactory,
                                      ObjectMapper objectMapper) {
        this.jsonApi4j = new JsonApi4j(domainRegistry, operationsRegistry);
        if (executorService != null) {
            jsonApi4j.setExecutor(executorService);
        }
        if (accessControlEvaluator != null) {
            jsonApi4j.setAccessControlEvaluator(accessControlEvaluator);
        }
        this.errorHandlerFactory = errorHandlerFactory;
        this.objectMapper = objectMapper;
        OperationDetailsResolver operationDetailsResolver = new OperationDetailsResolver(domainRegistry);
        this.jsonApiRequestSupplier = new HttpServletRequestJsonApiRequestSupplier(
                this.objectMapper,
                operationDetailsResolver
        );
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
            int errorStatusCode = errorHandlerFactory.resolveStatusCode(e);
            ErrorsDoc errorsDoc = errorHandlerFactory.resolveErrorsDoc(e);
            LOG.error("{}. Error message: {}", errorStatusCode + " code", e.getMessage());
            LOG.warn("Stacktrace: ", e);

            resp.setStatus(errorStatusCode);
            writeResponseBody(resp, errorsDoc);
        }
    }

    private void writeResponseBody(HttpServletResponse resp, Object body) {
        try {
            if (body != null) {
                resp.setContentType(JsonApiMediaType.MEDIA_TYPE);
                LOG.info("Setting response Content-Type to: {}", JsonApiMediaType.MEDIA_TYPE);
                resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
                LOG.info("Writing response body: {}", body);
                objectMapper.writeValue(resp.getOutputStream(), body);
            }
        } catch (IOException e) {
            LOG.error("Error writing JSON into HttpServletResponse. ", e);
        }
    }

}
