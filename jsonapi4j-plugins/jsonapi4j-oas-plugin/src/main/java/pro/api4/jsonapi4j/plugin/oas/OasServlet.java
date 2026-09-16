package pro.api4.jsonapi4j.plugin.oas;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.http.HttpHeaders;
import pro.api4.jsonapi4j.http.HttpStatusCodes;
import pro.api4.jsonapi4j.model.document.error.ErrorObject;
import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;
import pro.api4.jsonapi4j.plugin.oas.config.DiagnosticsMode;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;
import pro.api4.jsonapi4j.request.JsonApiMediaType;
import pro.api4.jsonapi4j.plugin.oas.customizer.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static pro.api4.jsonapi4j.plugin.oas.init.JsonApiOasServletContainerInitializer.OAS_PLUGIN_PROPERTIES_ATT_NAME;

@Slf4j
public class OasServlet extends HttpServlet {

    private static final String FORMAT_QUERY_PARAM = "format";
    private static final String YAML_FORMAT = "yaml";
    private static final String JSON_FORMAT = "json";
    private static final String YAML_CONTENT_TYPE = "application/yaml";
    private static final String JSON_CONTENT_TYPE = "application/json";

    private static final String GET_METHOD = "GET";
    private static final String HEAD_METHOD = "HEAD";
    private static final String ALLOWED_METHODS = GET_METHOD + ", " + HEAD_METHOD;

    private JsonApi4j jsonApi4j;
    private OasProperties oasProperties;

    /**
     * The framework's own mapper, so a generation failure is rendered exactly like every other error document the
     * application emits rather than by a second mapper configured slightly differently.
     */
    private ObjectMapper objectMapper;

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
        objectMapper = (ObjectMapper) config.getServletContext()
                .getAttribute(JsonApi4jServletContainerInitializer.OBJECT_MAPPER_ATT_NAME);

        if (oasProperties.diagnostics().generatesAtStartup()) {
            generateAtStartup();
        }

        log.info("{} has been initialized", OasServlet.class.getSimpleName());
    }

    /**
     * Builds the document while the application is still starting, so a document that cannot be vouched for stops
     * the boot rather than surfacing on whichever request happens to ask for it first. Every integration maps this
     * servlet with a load-on-startup order, so {@code init} runs during deployment.
     * <p>
     * The result is kept, which is also the only reason a first request is not the one paying for generation.
     */
    private void generateAtStartup() throws ServletException {
        log.info(
                "'{}.{}.{}' is {}, generating the OpenAPI document during startup",
                JsonApi4jProperties.CONFIG_PREFIX,
                OasProperties.OAS_PROPERTY,
                OasProperties.DIAGNOSTICS_PROPERTY,
                DiagnosticsMode.FAIL_ON_STARTUP
        );
        try {
            cachedOasJson = Json.pretty(OasDocument.generate(jsonApi4j));
        } catch (RuntimeException e) {
            throw new ServletException(String.format(
                    "The OpenAPI document could not be generated, and '%s.%s.%s' is %s: %s",
                    JsonApi4jProperties.CONFIG_PREFIX,
                    OasProperties.OAS_PROPERTY,
                    OasProperties.DIAGNOSTICS_PROPERTY,
                    DiagnosticsMode.FAIL_ON_STARTUP,
                    e.getMessage()
            ), e);
        }
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
        if (!GET_METHOD.equals(req.getMethod()) && !HEAD_METHOD.equals(req.getMethod())) {
            resp.setHeader(HttpHeaders.ALLOW.getName(), ALLOWED_METHODS);
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        String format = getFormat(req);
        boolean yaml = format.equals(YAML_FORMAT);
        String cached = yaml ? cachedOasYaml : cachedOasJson;
        if (cached != null) {
            writeToResponse(req, resp, yaml, cached);
            return;
        }

        OpenAPI openAPI;
        try {
            openAPI = OasDocument.generate(jsonApi4j);
        } catch (RuntimeException e) {
            writeGenerationFailure(resp, e);
            return;
        }
        writeOasToResponse(req, resp, yaml, openAPI);
    }

    /**
     * Answers a document that could not be generated with a JSON:API error document. Letting the exception escape
     * hands the container's own error page to whoever asked - HTML, with a stack trace in it - from an endpoint
     * whose every other answer is a JSON:API document.
     */
    private void writeGenerationFailure(HttpServletResponse resp,
                                        RuntimeException cause) throws IOException {
        log.error("The OpenAPI document could not be generated", cause);
        ErrorsDoc errorsDoc = new ErrorsDoc(
                List.of(ErrorObject.builder()
                        .status(String.valueOf(HttpStatusCodes.SC_500_INTERNAL_SERVER_ERROR.getCode()))
                        .title("The OpenAPI document could not be generated")
                        .detail(cause.getMessage())
                        .build()),
                null
        );
        resp.setStatus(HttpStatusCodes.SC_500_INTERNAL_SERVER_ERROR.getCode());
        resp.setContentType(JsonApiMediaType.MEDIA_TYPE);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.getWriter().write(objectMapper.writeValueAsString(errorsDoc));
    }

    /**
     * An explicit {@code ?format=} wins, since it is the caller being specific. With none, the {@code Accept} header
     * decides - a tool that asks for YAML and gets JSON has to guess what happened - and JSON is the answer when
     * neither says anything.
     */
    private String getFormat(HttpServletRequest req) {
        String format = req.getParameter(FORMAT_QUERY_PARAM);
        if (format != null) {
            return format.equalsIgnoreCase(YAML_FORMAT) ? YAML_FORMAT : JSON_FORMAT;
        }
        String accept = req.getHeader(HttpHeaders.ACCEPT.getName());
        return accept != null && accept.toLowerCase().contains(YAML_CONTENT_TYPE) ? YAML_FORMAT : JSON_FORMAT;
    }

    /**
     * The document is rebuilt only when the cache is empty, so the same bytes are served for the life of the
     * process. An {@code ETag} over them lets a caller that already has it skip the download - Swagger UI and CI
     * jobs poll this endpoint - while staying correct if a restart produces a different document.
     */
    private void writeToResponse(HttpServletRequest req,
                                 HttpServletResponse resp,
                                 boolean yaml,
                                 String oasString) throws IOException {
        String eTag = eTagOf(oasString);
        resp.setHeader(HttpHeaders.ETAG.getName(), eTag);
        resp.setContentType(yaml ? YAML_CONTENT_TYPE : JSON_CONTENT_TYPE);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (eTag.equals(req.getHeader(HttpHeaders.IF_NONE_MATCH.getName()))) {
            resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }
        resp.getWriter().write(oasString);
    }

    private String eTagOf(String oasString) {
        return "\"" + Integer.toHexString(oasString.hashCode()) + "\"";
    }

    /**
     * Renders through swagger's own mappers rather than a plain Jackson one. They carry the mixins that keep the
     * library's bookkeeping fields — {@code exampleSetFlag}, {@code types}, {@code jsonSchema} — out of the document;
     * a plain mapper serializes them as if they were OpenAPI keywords, which they are not.
     */
    private void writeOasToResponse(HttpServletRequest req,
                                    HttpServletResponse resp,
                                    boolean yaml,
                                    OpenAPI openAPI) throws IOException {
        String oasString = yaml ? Yaml.pretty(openAPI) : Json.pretty(openAPI);
        if (yaml) {
            cachedOasYaml = oasString;
        } else {
            cachedOasJson = oasString;
        }
        writeToResponse(req, resp, yaml, oasString);
    }

}
