package pro.api4.jsonapi4j.servlet.response.errorhandling;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import pro.api4.jsonapi4j.errorhandling.ErrorHandlerFactoriesRegistry;
import pro.api4.jsonapi4j.model.document.error.ErrorObject;
import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Answers a failure with a JSON:API error document, resolved through the {@link ErrorHandlerFactoriesRegistry} - so
 * every error the API returns, from any servlet or filter, shares one format and honours the application's mappings.
 *
 * <p>Client errors ({@code 4xx}) are logged at {@code WARN}, everything else at {@code ERROR} with the stack trace.
 * Response headers other than the content type are left to the caller.
 *
 * <p>The log line names the {@code id} of every error object in the response, and the same ids are put in the
 * {@link MDC} under {@value #ERROR_ID_MDC_KEY} while it is logged - so an error a client reports by its id can be
 * matched to the server-side log record, and through it to the request's trace.
 */
@Slf4j
public class ErrorsDocResponseWriter {

    public static final String ERROR_ID_MDC_KEY = "jsonapi4j.errorId";

    private final ErrorHandlerFactoriesRegistry errorHandlers;
    private final ObjectMapper objectMapper;

    public ErrorsDocResponseWriter(ErrorHandlerFactoriesRegistry errorHandlers, ObjectMapper objectMapper) {
        this.errorHandlers = errorHandlers;
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, Throwable failure) {
        int status = errorHandlers.resolveStatusCode(failure);
        ErrorsDoc errorsDoc = errorHandlers.resolveErrorsDoc(failure);
        String errorIds = errorIds(errorsDoc);
        if (errorIds.isEmpty()) {
            logFailure(status, errorIds, failure);
        } else {
            try (MDC.MDCCloseable ignored = MDC.putCloseable(ERROR_ID_MDC_KEY, errorIds)) {
                logFailure(status, errorIds, failure);
            }
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

    private static void logFailure(int status, String errorIds, Throwable failure) {
        if (status / 100 == 4) {
            log.warn("{} code. Error id(s): {}. Error message: {}", status, errorIds, failure.getMessage());
        } else {
            log.error("{} code. Error id(s): {}. Error message: {}", status, errorIds, failure.getMessage(), failure);
        }
    }

    static String errorIds(ErrorsDoc errorsDoc) {
        if (errorsDoc == null || errorsDoc.getErrors() == null) {
            return "";
        }
        return errorsDoc.getErrors().stream()
                .filter(Objects::nonNull)
                .map(ErrorObject::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(","));
    }

}
