package pro.api4.jsonapi4j.plugin.oas.customizer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pro.api4.jsonapi4j.http.HttpStatusCodes;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class ErrorExamplesCustomizer implements OasCustomizer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final String BAD_REQUEST_ERRORS_DOC = "Bad_Request_Errors_Doc";
    public static final String UNAUTHORIZED_ERRORS_DOC = "Unauthorized_Errors_Doc";
    public static final String FORBIDDEN_ERRORS_DOC = "Forbidden_Errors_Doc";
    public static final String RESOURCE_NOT_FOUND_ERRORS_DOC = "Resource_Not_Found_Errors_Doc";
    public static final String METHOD_NOT_SUPPORTED_ERRORS_DOC = "Method_Not_Supported_Errors_Doc";
    public static final String NOT_ACCEPTABLE_ERRORS_DOC = "Not_Acceptable_Errors_Doc";
    public static final String CONFLICT_ERRORS_DOC = "Conflict_Errors_Doc";
    public static final String UNSUPPORTED_MEDIA_TYPE_ERRORS_DOC = "Unsupported_Media_Type_Errors_Doc";
    public static final String TOO_MANY_REQUESTS_ERRORS_DOC = "Too_Many_Requests_Errors_Doc";
    public static final String INTERNAL_SERVER_ERRORS_DOC = "Internal_Server_Errors_Doc";

    /**
     * The canned example each documented status code publishes, paired with the resource it is read from - one map
     * rather than a map plus a parallel list of registrations, so a status code cannot be half-added. Backed by an
     * {@link EnumMap}, whose iteration order is the ascending status code order {@link HttpStatusCodes} declares,
     * so the published document does not reshuffle between restarts.
     */
    private static final Map<HttpStatusCodes, ErrorExample> CODES_TO_EXAMPLE;

    /**
     * The example name published for each status code the plugin can document. A code absent from this map is still
     * documented, it simply carries no example.
     */
    public static final Map<HttpStatusCodes, String> CODES_TO_EXAMPLE_NAME;

    static {
        Map<HttpStatusCodes, ErrorExample> codesToExample = new EnumMap<>(HttpStatusCodes.class);
        codesToExample.put(HttpStatusCodes.SC_400_BAD_REQUEST, new ErrorExample(BAD_REQUEST_ERRORS_DOC, "badRequestErrorsDoc.json"));
        codesToExample.put(HttpStatusCodes.SC_401_UNAUTHORIZED, new ErrorExample(UNAUTHORIZED_ERRORS_DOC, "unauthorizedErrorsDoc.json"));
        codesToExample.put(HttpStatusCodes.SC_403_FORBIDDEN, new ErrorExample(FORBIDDEN_ERRORS_DOC, "forbiddenErrorsDoc.json"));
        codesToExample.put(HttpStatusCodes.SC_404_RESOURCE_NOT_FOUND, new ErrorExample(RESOURCE_NOT_FOUND_ERRORS_DOC, "resourceNotFoundErrorsDoc.json"));
        codesToExample.put(HttpStatusCodes.SC_405_METHOD_NOT_SUPPORTED, new ErrorExample(METHOD_NOT_SUPPORTED_ERRORS_DOC, "methodNotSupportedErrorsDoc.json"));
        codesToExample.put(HttpStatusCodes.SC_406_NOT_ACCEPTABLE, new ErrorExample(NOT_ACCEPTABLE_ERRORS_DOC, "notAcceptableErrorsDoc.json"));
        codesToExample.put(HttpStatusCodes.SC_409_CONFLICT, new ErrorExample(CONFLICT_ERRORS_DOC, "conflictErrorsDoc.json"));
        codesToExample.put(HttpStatusCodes.SC_415_UNSUPPORTED_MEDIA_TYPE, new ErrorExample(UNSUPPORTED_MEDIA_TYPE_ERRORS_DOC, "unsupportedMediaTypeErrorsDoc.json"));
        codesToExample.put(HttpStatusCodes.SC_429_TOO_MANY_REQUESTS, new ErrorExample(TOO_MANY_REQUESTS_ERRORS_DOC, "tooManyRequestsErrorsDoc.json"));
        codesToExample.put(HttpStatusCodes.SC_500_INTERNAL_SERVER_ERROR, new ErrorExample(INTERNAL_SERVER_ERRORS_DOC, "internalServerErrorsDoc.json"));
        CODES_TO_EXAMPLE = Collections.unmodifiableMap(codesToExample);

        Map<HttpStatusCodes, String> codesToExampleName = new EnumMap<>(HttpStatusCodes.class);
        codesToExample.forEach((code, example) -> codesToExampleName.put(code, example.name()));
        CODES_TO_EXAMPLE_NAME = Collections.unmodifiableMap(codesToExampleName);
    }

    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        CODES_TO_EXAMPLE.forEach((code, example) -> openApi.getComponents()
                .addExamples(example.name(), new Example().value(readExampleFromResource(example.fileName()))));
    }

    /**
     * Parses the example rather than embedding its source text. Handing the raw {@code String} to
     * {@link Example#value(Object)} publishes the example as a JSON string literal, which Swagger UI happens to render
     * as if it were JSON but no machine reads that way: the example then contradicts the media type's schema, and
     * tools that build mock responses from the document return the escaped text instead of an error document.
     */
    private JsonNode readExampleFromResource(String fileName) {
        String path = "oas/errorExamples/" + fileName;

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("wrong path");
            }
            return OBJECT_MAPPER.readTree(is);
        } catch (IOException e) {
            return null;
        }
    }

    private record ErrorExample(String name, String fileName) {
    }

}
