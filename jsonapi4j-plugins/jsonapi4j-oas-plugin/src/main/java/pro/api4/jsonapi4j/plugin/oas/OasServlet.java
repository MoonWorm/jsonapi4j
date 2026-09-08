package pro.api4.jsonapi4j.plugin.oas;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;
import pro.api4.jsonapi4j.plugin.oas.customizer.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static pro.api4.jsonapi4j.plugin.oas.init.JsonApiOasServletContainerInitializer.OAS_PLUGIN_PROPERTIES_ATT_NAME;

@Slf4j
public class OasServlet extends HttpServlet {

    private static final String FORMAT_QUERY_PARAM = "format";
    private static final String YAML_FORMAT = "yaml";
    private static final String JSON_FORMAT = "json";
    private static final String YAML_CONTENT_TYPE = "application/yaml";
    private static final String JSON_CONTENT_TYPE = "application/json";

    private JsonApi4j jsonApi4j;
    private OasProperties oasProperties;

    /**
     * A servlet instance is shared by every request thread, so these need safe publication: without {@code volatile}
     * a thread may keep reading {@code null} after another has filled the cache, and rebuild the document for nothing.
     * Concurrent first requests may still each build one — the result is identical either way, so the cost is wasted
     * work rather than a wrong answer, and that is cheaper than serialising every request behind a lock.
     */
    private volatile String cachedOasJson;
    private volatile String cachedOasYaml;

    @Override
    public void init(ServletConfig config) throws ServletException {
        log.info("Initializing {} ...", OasServlet.class.getSimpleName());

        super.init(config);

        oasProperties = (OasProperties) config.getServletContext().getAttribute(OAS_PLUGIN_PROPERTIES_ATT_NAME);

        if (oasProperties == null || !oasProperties.enabled()) {
            log.info(
                    "{} has not been initialized, oasProperties is null or {} is disabled",
                    OasServlet.class.getSimpleName(),
                    JsonApiOasPlugin.class.getSimpleName()
            );
            return;
        }

        jsonApi4j = JsonApi4jServletContainerInitializer.initJsonApi4j(config.getServletContext());

        log.info("{} has been initialized", OasServlet.class.getSimpleName());
    }

    @Override
    protected void service(HttpServletRequest req,
                           HttpServletResponse resp) throws IOException {
        if (oasProperties == null || !oasProperties.enabled()) {
            log.debug(
                    "{} has not been initialized, {} is null or {} is disabled",
                    OasServlet.class.getSimpleName(),
                    OasProperties.class.getSimpleName(),
                    JsonApiOasPlugin.class.getSimpleName()
            );
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String format = getFormat(req);
        boolean yaml = format.equals(YAML_FORMAT);
        String cached = yaml ? cachedOasYaml : cachedOasJson;
        if (cached != null) {
            writeToResponse(resp, yaml, cached);
            return;
        }

        OpenAPI openAPI = OasDocument.generate(jsonApi4j);
        writeOasToResponse(resp, yaml, openAPI);
    }

    private String getFormat(HttpServletRequest req) {
        String format = req.getParameter(FORMAT_QUERY_PARAM);
        if (format == null) {
            return JSON_FORMAT;
        }
        return format.equalsIgnoreCase(YAML_FORMAT) ? YAML_FORMAT : JSON_FORMAT;

    }

    private void writeToResponse(HttpServletResponse resp,
                                 boolean yaml,
                                 String oasString) throws IOException {
        resp.setContentType(yaml ? YAML_CONTENT_TYPE : JSON_CONTENT_TYPE);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.getWriter().write(oasString);
    }

    /**
     * Renders through swagger's own mappers rather than a plain Jackson one. They carry the mixins that keep the
     * library's bookkeeping fields — {@code exampleSetFlag}, {@code types}, {@code jsonSchema} — out of the document;
     * a plain mapper serializes them as if they were OpenAPI keywords, which they are not.
     */
    private void writeOasToResponse(HttpServletResponse resp,
                                    boolean yaml,
                                    OpenAPI openAPI) throws IOException {
        String oasString = yaml ? Yaml.pretty(openAPI) : Json.pretty(openAPI);
        if (yaml) {
            cachedOasYaml = oasString;
        } else {
            cachedOasJson = oasString;
        }
        writeToResponse(resp, yaml, oasString);
    }

}
