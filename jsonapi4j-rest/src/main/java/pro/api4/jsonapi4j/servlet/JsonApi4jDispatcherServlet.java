package pro.api4.jsonapi4j.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.JsonApi4jReportGenerator;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.http.HttpHeaders;
import pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

        this.jsonApi4j = JsonApi4jServletContainerInitializer.initJsonApi4j(config.getServletContext());
        this.errorHandlerFactory = (ErrorHandlerFactoriesRegistry) config.getServletContext().getAttribute(ERROR_HANDLER_FACTORIES_REGISTRY_ATT_NAME);
        this.objectMapper = (ObjectMapper) config.getServletContext().getAttribute(OBJECT_MAPPER_ATT_NAME);

        List<String> missingComponents = missingMandatoryComponents();
        if (!missingComponents.isEmpty()) {
            throw new UnavailableException(String.format(
                    "%s can't be initialized. Mandatory components are missing from the servlet context: %s. "
                            + "Ensure the JsonApi4j container initializer or auto-configuration has run.",
                    JsonApi4jDispatcherServlet.class.getSimpleName(),
                    String.join(", ", missingComponents)
            ));
        }

        log.info(new JsonApi4jReportGenerator(this.jsonApi4j).generateStateReport());

        this.jsonApiRequestSupplier = composeJsonApiRequestSupplier(
                objectMapper,
                jsonApi4j.getDomainRegistry()
        );

        log.info("{} has been initialized", JsonApi4jDispatcherServlet.class.getSimpleName());
    }

    private List<String> missingMandatoryComponents() {
        List<String> missing = new ArrayList<>();
        if (jsonApi4j == null) {
            missing.add(JsonApi4j.class.getSimpleName());
        }
        if (errorHandlerFactory == null) {
            missing.add(ErrorHandlerFactoriesRegistry.class.getSimpleName());
        }
        if (objectMapper == null) {
            missing.add(ObjectMapper.class.getSimpleName());
        }
        return Collections.unmodifiableList(missing);
    }

    private HttpServletRequestJsonApiRequestSupplier composeJsonApiRequestSupplier(ObjectMapper objectMapper,
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
