package pro.api4.jsonapi4j.plugin.oas;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.config.JsonApi4jConfigReader;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;
import pro.api4.jsonapi4j.plugin.oas.customizer.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.*;
import static pro.api4.jsonapi4j.plugin.oas.init.JsonApiOasServletContainerInitializer.OAS_PLUGIN_PROPERTIES_ATT_NAME;
import static pro.api4.jsonapi4j.plugin.oas.init.JsonApiOasServletContainerInitializer.OAS_PLUGIN_ROOT_PATH_ATT_NAME;

public class OasServlet extends HttpServlet {

    private static final String FORMAT_QUERY_PARAM = "format";
    private static final String YAML_FORMAT = "yaml";
    private static final String JSON_FORMAT = "json";
    private static final String YAML_CONTENT_TYPE = "application/yaml";
    private static final String JSON_CONTENT_TYPE = "application/json";

    private DomainRegistry domainRegistry;
    private OperationsRegistry operationsRegistry;
    private String rootPath;
    private OasProperties oasProperties;

    private String cachedOasJson;
    private String cachedOasYaml;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        rootPath = (String) config.getServletContext().getAttribute(OAS_PLUGIN_ROOT_PATH_ATT_NAME);
        Validate.notNull(rootPath, "rootPath property can't be null");

        oasProperties = (OasProperties) config.getServletContext().getAttribute(OAS_PLUGIN_PROPERTIES_ATT_NAME);
        Validate.notNull(oasProperties, "oas property section can't be null");

        JsonApi4j jsonApi4j = (JsonApi4j) config.getServletContext().getAttribute(JSONAPI4J_ATT_NAME);
        Validate.notNull(jsonApi4j, "JsonApi4j must be assembled before the OAS servlet initializes");
        domainRegistry = jsonApi4j.getDomainRegistry();
        operationsRegistry = jsonApi4j.getOperationsRegistry();
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
        new CommonOpenApiCustomizer(oasProperties, domainRegistry, operationsRegistry).customise(openAPI);
        new JsonApiResponseSchemaCustomizer(domainRegistry, operationsRegistry).customise(openAPI);
        new JsonApiRequestBodySchemaCustomizer(domainRegistry, operationsRegistry).customise(openAPI);
        new JsonApiOperationsCustomizer(
                rootPath,
                domainRegistry,
                operationsRegistry,
                oasProperties != null ? oasProperties.customResponseHeaders() : Collections.emptyList()
        ).customise(openAPI);
        new ErrorExamplesCustomizer().customise(openAPI);
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
