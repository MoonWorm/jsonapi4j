package pro.api4.jsonapi4j.servlet.response.errorhandling;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.errorhandling.ErrorHandlerFactoriesRegistry;
import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Answers a failure with a JSON:API error document, resolved through the {@link ErrorHandlerFactoriesRegistry} - so
 * every error the API returns, from any servlet or filter, shares one format and honours the application's mappings.
 *
 * <p>Client errors ({@code 4xx}) are logged at {@code WARN}, everything else at {@code ERROR} with the stack trace.
 * Response headers other than the content type are left to the caller.
 */
@Slf4j
public class ErrorsDocResponseWriter {

    private final ErrorHandlerFactoriesRegistry errorHandlers;
    private final ObjectMapper objectMapper;

    public ErrorsDocResponseWriter(ErrorHandlerFactoriesRegistry errorHandlers, ObjectMapper objectMapper) {
        this.errorHandlers = errorHandlers;
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, Throwable failure) {
        int status = errorHandlers.resolveStatusCode(failure);
        ErrorsDoc errorsDoc = errorHandlers.resolveErrorsDoc(failure);
        if (status / 100 == 4) {
            log.warn("{} code. Error message: {}", status, failure.getMessage());
        } else {
            log.error("{} code. Error message: {}", status, failure.getMessage(), failure);
        }
        response.setStatus(status);
        response.setContentType(JsonApiMediaType.MEDIA_TYPE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try {
            objectMapper.writeValue(response.getOutputStream(), errorsDoc);
        } catch (IOException e) {
            log.error("Error writing JSON into HttpServletResponse. ", e);
        }
    }

}
