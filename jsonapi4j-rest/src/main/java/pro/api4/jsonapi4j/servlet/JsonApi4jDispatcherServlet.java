package pro.api4.jsonapi4j.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.JsonApi4jReportGenerator;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.http.HttpHeaders;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.request.JsonApiMediaType;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.request.JsonApiRequestSupplier;
import pro.api4.jsonapi4j.servlet.request.HttpServletRequestJsonApiRequestSupplier;
import pro.api4.jsonapi4j.servlet.request.OperationDetailsResolver;
import pro.api4.jsonapi4j.servlet.response.ResponseHeaders;
import pro.api4.jsonapi4j.servlet.response.ResponseStatus;
import pro.api4.jsonapi4j.servlet.response.errorhandling.ErrorHandlerFactoriesRegistry;
import pro.api4.jsonapi4j.servlet.response.errorhandling.JsonApi4jErrorHandlerFactoriesRegistry;
import pro.api4.jsonapi4j.servlet.response.errorhandling.impl.DefaultErrorHandlerFactory;

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

        JsonApi4jProperties properties = (JsonApi4jProperties) config.getServletContext().getAttribute(JSONAPI4J_PROPERTIES_ATT_NAME);
        Validate.notNull(properties, "JsonApi4jProperties can't be null");

        this.jsonApi4j = composeJsonApi4j(config.getServletContext());

        log.info(new JsonApi4jReportGenerator(this.jsonApi4j).generateStateReport());

        this.errorHandlerFactory = composeErrorHandlerFactory(config.getServletContext());

        this.objectMapper = composeObjectMapper(config.getServletContext());

        this.jsonApiRequestSupplier = composeJsonApiRequestSupplier(
                config.getServletContext(),
                objectMapper,
                jsonApi4j.getDomainRegistry()
        );

        log.info("{} has been initialized", JsonApi4jDispatcherServlet.class.getSimpleName());
    }

    private ObjectMapper composeObjectMapper(ServletContext context) {
        ObjectMapper objectMapper = initObjectMapper(context);
        Validate.notNull(objectMapper, "ObjectMapper can't be null");
        return objectMapper;
    }

    private JsonApi4j composeJsonApi4j(ServletContext context) {
        JsonApi4j jsonApi4j = (JsonApi4j) context.getAttribute(JSONAPI4J_ATT_NAME);
        Validate.notNull(jsonApi4j, "JsonApi4j can't be null");
        log.debug("Applied {} from Servlet Context under {} attribute", JsonApi4j.class.getSimpleName(), JSONAPI4J_ATT_NAME);
        return jsonApi4j;
    }

    private ErrorHandlerFactoriesRegistry composeErrorHandlerFactory(ServletContext context) {
        ErrorHandlerFactoriesRegistry errorHandlerFactory = (ErrorHandlerFactoriesRegistry) context.getAttribute(ERROR_HANDLER_FACTORIES_REGISTRY_ATT_NAME);
        if (errorHandlerFactory == null) {
            log.debug("AggregatableErrorHandlerFactory not found in servlet context. Applying a default ErrorHandlerFactory.");
            ErrorHandlerFactoriesRegistry errorHandlerFactoriesRegistry = new JsonApi4jErrorHandlerFactoriesRegistry();
            errorHandlerFactoriesRegistry.registerAll(new DefaultErrorHandlerFactory());
            return errorHandlerFactoriesRegistry;
        }
        log.debug("Applied {} from Servlet Context under {} attribute", ErrorHandlerFactoriesRegistry.class.getSimpleName(), ERROR_HANDLER_FACTORIES_REGISTRY_ATT_NAME);
        return errorHandlerFactory;
    }

    private HttpServletRequestJsonApiRequestSupplier composeJsonApiRequestSupplier(ServletContext context,
                                                                                   ObjectMapper objectMapper,
                                                                                   DomainRegistry domainRegistry) {
        OperationDetailsResolver operationDetailsResolver = new OperationDetailsResolver(domainRegistry);
        HttpServletRequestJsonApiRequestSupplier jsonApiRequestSupplier = new HttpServletRequestJsonApiRequestSupplier(
                objectMapper,
                operationDetailsResolver
        );
        log.debug("{} has been successfully composed", HttpServletRequestJsonApiRequestSupplier.class.getSimpleName());
        return jsonApiRequestSupplier;
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
            // check if status is overridden
            status = ResponseStatus.getOverriddenStatus().orElse(status);

            resp.setStatus(status);
            log.debug("Setting response status code: {}", status);

            if (targetOperationType == OperationType.CREATE_RESOURCE) {
                SingleResourceDoc<?> singleResourceDoc = (SingleResourceDoc<?>) dataDoc;
                if (singleResourceDoc != null) {
                    String location = URI.create("/" + targetResourceType.getType() + "/" + singleResourceDoc.getData().getId()).toString();
                    resp.setHeader(HttpHeaders.LOCATION.getName(), location);
                    log.debug("Setting HTTP Location header: {}", location);
                }
            }

            // populate custom headers
            ResponseHeaders.flush(resp);

            writeResponseBody(resp, dataDoc);

        } catch (Exception e) {
            if (errorHandlerFactory != null) {
                int errorStatusCode = errorHandlerFactory.resolveStatusCode(e);
                ErrorsDoc errorsDoc = errorHandlerFactory.resolveErrorsDoc(e);
                if (errorStatusCode / 100 == 4) {
                    // client-side errors
                    log.warn("{}. Error message: {}", errorStatusCode + " code", e.getMessage());
                } else {
                    // server-side errors
                    log.error("{}. Error message: {}", errorStatusCode + " code", e.getMessage(), e);
                }
                resp.setStatus(errorStatusCode);
                writeResponseBody(resp, errorsDoc);
            } else {
                throw e;
            }
        }
    }

    private void writeResponseBody(HttpServletResponse resp, Object body) {
        try {
            if (body != null) {
                resp.setContentType(JsonApiMediaType.MEDIA_TYPE);
                log.debug("Setting response Content-Type to: {}", JsonApiMediaType.MEDIA_TYPE);
                resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
                log.debug("Writing response body: {}", body);
                objectMapper.writeValue(resp.getOutputStream(), body);
            }
        } catch (IOException e) {
            log.error("Error writing JSON into HttpServletResponse. ", e);
        }
    }

}
