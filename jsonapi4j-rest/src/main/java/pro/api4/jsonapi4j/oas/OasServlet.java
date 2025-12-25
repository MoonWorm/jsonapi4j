package pro.api4.jsonapi4j.oas;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pro.api4.jsonapi4j.config.JsonApi4jConfigReader;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.oas.customizer.CommonOpenApiCustomizer;
import pro.api4.jsonapi4j.oas.customizer.JsonApiOperationsCustomizer;
import pro.api4.jsonapi4j.oas.customizer.JsonApiRequestBodySchemaCustomizer;
import pro.api4.jsonapi4j.oas.customizer.JsonApiResponseSchemaCustomizer;
import pro.api4.jsonapi4j.operation.OperationsRegistry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class OasServlet extends HttpServlet {

    private static final String FORMAT_QUERY_PARAM = "format";
    private static final String YAML_FORMAT = "yaml";
    private static final String JSON_FORMAT = "json";
    private static final String YAML_CONTENT_TYPE = "application/yaml";
    private static final String JSON_CONTENT_TYPE = "application/json";

    private final DomainRegistry domainRegistry;
    private final OperationsRegistry operationsRegistry;
    private final JsonApi4jProperties properties;

    private String cachedOasJson;
    private String cachedOasYaml;

    public OasServlet(DomainRegistry domainRegistry,
                      OperationsRegistry operationsRegistry,
                      JsonApi4jProperties properties) {
        this.domainRegistry = domainRegistry;
        this.operationsRegistry = operationsRegistry;
        this.properties = properties;
    }

    @Override
    protected void service(HttpServletRequest req,
                           HttpServletResponse resp) throws IOException {

        String format = getFormat(req);
        if (format.equalsIgnoreCase(YAML_FORMAT) && cachedOasYaml != null) {
            writeCachedOasYamlToResponse(resp);
            return;
        }
        if (format.equalsIgnoreCase(JSON_FORMAT) && cachedOasJson != null) {
            writeCachedOasJsonToResponse(resp);
            return;
        }
        OpenAPI openAPI = new OpenAPI();
        new CommonOpenApiCustomizer(properties.getOas(), operationsRegistry).customise(openAPI);
        new JsonApiResponseSchemaCustomizer(domainRegistry, operationsRegistry).customise(openAPI);
        new JsonApiRequestBodySchemaCustomizer(operationsRegistry).customise(openAPI);
        new JsonApiOperationsCustomizer(
                properties.getRootPath(),
                domainRegistry,
                operationsRegistry,
                properties.getOas().getCustomResponseHeaders()
        ).customise(openAPI);
        writeOasToResponse(resp, format, openAPI);
    }

    private String getFormat(HttpServletRequest req) {
        String format = req.getParameter(FORMAT_QUERY_PARAM);
        if (format == null) {
            return JSON_FORMAT;
        }
        return format.equalsIgnoreCase(YAML_FORMAT) ? YAML_FORMAT : JSON_FORMAT;

    }

    private void writeCachedOasYamlToResponse(HttpServletResponse resp) throws IOException {
        resp.setContentType(YAML_CONTENT_TYPE);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.getWriter().write(cachedOasYaml);
    }

    private void writeCachedOasJsonToResponse(HttpServletResponse resp) throws IOException {
        resp.setContentType(JSON_CONTENT_TYPE);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.getWriter().write(cachedOasJson);
    }

    private void writeOasToResponse(HttpServletResponse resp,
                                    String format,
                                    OpenAPI openAPI) throws IOException {
        ObjectMapper objectMapper = format.equals(YAML_FORMAT)
                ? JsonApi4jConfigReader.getYamlObjectMapper()
                : JsonApi4jConfigReader.getJsonObjectMapper();
        String contentType = format.equals(YAML_FORMAT) ? YAML_CONTENT_TYPE : JSON_CONTENT_TYPE;
        resp.setContentType(contentType);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String oasString = objectMapper.writeValueAsString(openAPI);
        if (format.equals(YAML_FORMAT)) {
            cachedOasYaml = oasString;
        } else {
            cachedOasJson = oasString;
        }
        resp.getWriter().write(oasString);
    }

}
