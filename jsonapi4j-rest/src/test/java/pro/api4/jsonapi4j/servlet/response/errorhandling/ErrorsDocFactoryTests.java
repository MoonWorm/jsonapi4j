package pro.api4.jsonapi4j.servlet.response.errorhandling;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.model.document.error.ErrorObject;
import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorsDocFactoryTests {

    // --- internalServerErrorsDoc ---

    @Test
    void internalServerErrorsDoc_producesGenericError() {
        // when
        ErrorsDoc doc = ErrorsDocFactory.internalServerErrorsDoc();

        // then
        assertSingleError(doc, "500", "INTERNAL_SERVER_ERROR", "The service encountered an error");
    }

    @Test
    void internalServerErrorsDoc_withDetail_usesProvidedDetail() {
        // when
        ErrorsDoc doc = ErrorsDocFactory.internalServerErrorsDoc("custom detail");

        // then
        assertSingleError(doc, "500", "INTERNAL_SERVER_ERROR", "custom detail");
    }

    // --- badRequestErrorsDoc ---

    @Test
    void badRequestErrorsDoc_produces400ErrorWithParameter() {
        // when
        ErrorsDoc doc = ErrorsDocFactory.badRequestErrorsDoc(
                DefaultErrorCodes.VALUE_IS_ABSENT, "must not be null", "name"
        );

        // then
        assertSingleError(doc, "400", "VALUE_IS_ABSENT", "must not be null");
        assertThat(doc.getErrors().getFirst().getSource().getParameter()).isEqualTo("name");
    }

    // --- badRequestInvalidCursorErrorsDoc ---

    @Test
    void badRequestInvalidCursorErrorsDoc_producesInvalidCursorError() {
        // when
        ErrorsDoc doc = ErrorsDocFactory.badRequestInvalidCursorErrorsDoc("abc123");

        // then
        assertSingleError(doc, "400", "INVALID_CURSOR", "Invalid cursor value: abc123");
        assertThat(doc.getErrors().getFirst().getSource().getParameter()).isEqualTo("page[cursor]");
    }

    // --- badRequestInvalidPayloadErrorsDoc ---

    @Test
    void badRequestInvalidPayloadErrorsDoc_producesInvalidPayloadError() {
        // when
        ErrorsDoc doc = ErrorsDocFactory.badRequestInvalidPayloadErrorsDoc("body is not valid JSON");

        // then
        assertSingleError(doc, "400", "INVALID_PAYLOAD", "body is not valid JSON");
        assertThat(doc.getErrors().getFirst().getSource().getParameter()).isEqualTo("requestBody");
    }

    // --- resourceNotFoundErrorsDoc ---

    @Test
    void resourceNotFoundErrorsDoc_withDetail_producesNotFoundError() {
        // when
        ErrorsDoc doc = ErrorsDocFactory.resourceNotFoundErrorsDoc("resource not found");

        // then
        assertSingleError(doc, "404", "NOT_FOUND", "resource not found");
    }

    @Test
    void resourceNotFoundErrorsDoc_withNameAndId_producesFormattedMessage() {
        // when
        ErrorsDoc doc = ErrorsDocFactory.resourceNotFoundErrorsDoc("users", "123");

        // then
        assertSingleError(doc, "404", "NOT_FOUND",
                "The requested resource (users) with id (123) could not be found");
    }

    @Test
    void resourceNotFoundErrorsDoc_withNameAndNullId_omitsIdFromMessage() {
        // when
        ErrorsDoc doc = ErrorsDocFactory.resourceNotFoundErrorsDoc("users", null);

        // then
        assertSingleError(doc, "404", "NOT_FOUND",
                "The requested resource (users) could not be found");
    }

    // --- badGatewayErrorsDoc ---

    @Test
    void badGatewayErrorsDoc_produces502Error() {
        // when
        ErrorsDoc doc = ErrorsDocFactory.badGatewayErrorsDoc("upstream timeout");

        // then
        assertSingleError(doc, "502", "BAD_GATEWAY", "upstream timeout");
    }

    // --- conflictErrorsDoc ---

    @Test
    void conflictErrorsDoc_produces409Error() {
        // when
        ErrorsDoc doc = ErrorsDocFactory.conflictErrorsDoc("duplicate entry");

        // then
        assertSingleError(doc, "409", "CONFLICT", "duplicate entry");
    }

    // --- unsupportedMediaTypeErrorsDoc ---

    @Test
    void unsupportedMediaTypeErrorsDoc_produces415Error() {
        // when
        ErrorsDoc doc = ErrorsDocFactory.unsupportedMediaTypeErrorsDoc("text/plain", "application/vnd.api+json");

        // then
        assertSingleError(doc, "415", "UNSUPPORTED_MEDIA_TYPE",
                "Unsupported request media type: text/plain. Expected media type format: application/vnd.api+json");
    }

    // --- notAcceptableErrorsDoc ---

    @Test
    void notAcceptableErrorsDoc_produces406Error() {
        // when
        ErrorsDoc doc = ErrorsDocFactory.notAcceptableErrorsDoc(
                java.util.List.of("application/vnd.api+json", "application/json")
        );

        // then
        assertSingleError(doc, "406", "NOT_ACCEPTABLE",
                "Not acceptable response media type on client. Supported media types on server: application/vnd.api+json, application/json");
    }

    // --- methodNotSupportedErrorsDoc ---

    @Test
    void methodNotSupportedErrorsDoc_produces405Error() {
        // when
        ErrorsDoc doc = ErrorsDocFactory.methodNotSupportedErrorsDoc("PUT");

        // then
        assertSingleError(doc, "405", "METHOD_NOT_SUPPORTED",
                "The resource doesnt support the requested HTTP method: PUT");
    }

    // --- genericErrorsDoc ---

    @Test
    void genericErrorsDoc_producesErrorWithGivenStatusAndCode() {
        // when
        ErrorsDoc doc = ErrorsDocFactory.genericErrorsDoc(
                422, DefaultErrorCodes.CONFLICTING_PARAMETERS, "a and b conflict"
        );

        // then
        assertSingleError(doc, "422", "CONFLICTING_PARAMETERS", "a and b conflict");
    }

    @Test
    void genericErrorsDoc_withParameter_includesSourceParameter() {
        // when
        ErrorsDoc doc = ErrorsDocFactory.genericErrorsDoc(
                400, DefaultErrorCodes.INVALID_ENUM_VALUE, "invalid value", "status"
        );

        // then
        assertSingleError(doc, "400", "INVALID_ENUM_VALUE", "invalid value");
        assertThat(doc.getErrors().getFirst().getSource().getParameter()).isEqualTo("status");
    }

    @Test
    void genericErrorsDoc_withNullCode_fallsBackToGenericRequestError() {
        // when
        ErrorsDoc doc = ErrorsDocFactory.genericErrorsDoc(400, null, "something went wrong");

        // then
        assertSingleError(doc, "400", "GENERIC_REQUEST_ERROR", "something went wrong");
    }

    // --- errorObject ---

    @Test
    void errorObject_generatesUniqueId() {
        // when
        ErrorObject error1 = ErrorsDocFactory.errorObject(400, DefaultErrorCodes.VALUE_IS_ABSENT, "msg1");
        ErrorObject error2 = ErrorsDocFactory.errorObject(400, DefaultErrorCodes.VALUE_IS_ABSENT, "msg2");

        // then
        assertThat(error1.getId()).isNotBlank();
        assertThat(error2.getId()).isNotBlank();
        assertThat(error1.getId()).isNotEqualTo(error2.getId());
    }

    @Test
    void errorObject_withoutParameter_hasNullSource() {
        // when
        ErrorObject error = ErrorsDocFactory.errorObject(500, DefaultErrorCodes.INTERNAL_SERVER_ERROR, "oops");

        // then
        assertThat(error.getSource()).isNull();
    }

    @Test
    void errorObject_withParameter_hasSourceWithParameter() {
        // when
        ErrorObject error = ErrorsDocFactory.errorObject(400, DefaultErrorCodes.VALUE_IS_ABSENT, "missing", "field1");

        // then
        assertThat(error.getSource()).isNotNull();
        assertThat(error.getSource().getParameter()).isEqualTo("field1");
    }

    // --- Helpers ---

    private void assertSingleError(ErrorsDoc doc, String expectedStatus, String expectedCode, String expectedDetail) {
        assertThat(doc.getErrors()).hasSize(1);
        ErrorObject error = doc.getErrors().getFirst();
        assertThat(error.getStatus()).isEqualTo(expectedStatus);
        assertThat(error.getCode()).isEqualTo(expectedCode);
        assertThat(error.getDetail()).isEqualTo(expectedDetail);
        assertThat(error.getId()).isNotBlank();
    }

}
