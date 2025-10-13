package io.jsonapi4j.oas.customizer;

import io.jsonapi4j.http.HttpStatusCodes;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ErrorExamplesCustomizer {

    public static final String BAD_REQUEST_ERRORS_DOC = "Bad_Request_Errors_Doc";
    public static final String RESOURCE_NOT_FOUND_ERRORS_DOC = "Resource_Not_Found_Errors_Doc";
    public static final String METHOD_NOT_SUPPORTED_ERRORS_DOC = "Method_Not_Supported_Errors_Doc";
    public static final String NOT_ACCEPTABLE_ERRORS_DOC = "Not_Acceptable_Errors_Doc";
    public static final String UNSUPPORTED_MEDIA_TYPE_ERRORS_DOC = "Unsupported_Media_Type_Errors_Doc";
    public static final String INTERNAL_SERVER_ERRORS_DOC = "Internal_Server_Errors_Doc";

    public static final Map<HttpStatusCodes, String> CODES_TO_EXAMPLE_NAME = Map.of(
            HttpStatusCodes.SC_400_BAD_REQUEST, BAD_REQUEST_ERRORS_DOC,
            HttpStatusCodes.SC_404_RESOURCE_NOT_FOUND, RESOURCE_NOT_FOUND_ERRORS_DOC,
            HttpStatusCodes.SC_405_METHOD_NOT_SUPPORTED, METHOD_NOT_SUPPORTED_ERRORS_DOC,
            HttpStatusCodes.SC_406_NOT_ACCEPTABLE, NOT_ACCEPTABLE_ERRORS_DOC,
            HttpStatusCodes.SC_415_UNSUPPORTED_MEDIA_TYPE, UNSUPPORTED_MEDIA_TYPE_ERRORS_DOC,
            HttpStatusCodes.SC_500_INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERRORS_DOC
    );

    public void customise(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        openApi.getComponents()
                .addExamples(UNSUPPORTED_MEDIA_TYPE_ERRORS_DOC, new Example().value(readExampleFromResource("unsupportedMediaTypeErrorsDoc.json")))
                .addExamples(BAD_REQUEST_ERRORS_DOC, new Example().value(readExampleFromResource("badRequestErrorsDoc.json")))
                .addExamples(INTERNAL_SERVER_ERRORS_DOC, new Example().value(readExampleFromResource("internalServerErrorsDoc.json")))
                .addExamples(NOT_ACCEPTABLE_ERRORS_DOC, new Example().value(readExampleFromResource("notAcceptableErrorsDoc.json")))
                .addExamples(METHOD_NOT_SUPPORTED_ERRORS_DOC, new Example().value(readExampleFromResource("methodNotSupportedErrorsDoc.json")))
                .addExamples(RESOURCE_NOT_FOUND_ERRORS_DOC, new Example().value(readExampleFromResource("resourceNotFoundErrorsDoc.json")));
    }

    private String readExampleFromResource(String fileName) {
        try {
            String path = "/oas/errorExamples/" + fileName;
            InputStream is = new ClassPathResource(path).getInputStream();
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

}
