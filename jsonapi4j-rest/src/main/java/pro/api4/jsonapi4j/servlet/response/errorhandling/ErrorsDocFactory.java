package pro.api4.jsonapi4j.servlet.response.errorhandling;

import pro.api4.jsonapi4j.http.HttpStatusCodes;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.model.document.error.ErrorCode;
import pro.api4.jsonapi4j.model.document.error.ErrorObject;
import pro.api4.jsonapi4j.model.document.error.ErrorSourceObject;
import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;

import java.util.List;
import java.util.UUID;

import static java.util.Collections.singletonList;

public final class ErrorsDocFactory {

    private ErrorsDocFactory() {
    }

    public static ErrorsDoc genericErrorsDoc(int status,
                                             ErrorCode code,
                                             String detail,
                                             String parameter) {
        return new ErrorsDoc(
                singletonList(
                        errorObject(status, code, detail, parameter)
                )
        );
    }

    public static ErrorsDoc genericErrorsDoc(int status,
                                             ErrorCode code,
                                             String detail) {
        return new ErrorsDoc(
                singletonList(
                        errorObject(status, code, detail, null)
                )
        );
    }

    public static ErrorsDoc badRequestErrorsDoc(ErrorCode code,
                                                String detail,
                                                String parameter) {
        return genericErrorsDoc(HttpStatusCodes.SC_400_BAD_REQUEST.getCode(), code, detail, parameter);
    }

    public static ErrorsDoc badRequestInvalidCursorErrorsDoc(String cursor) {
        return badRequestErrorsDoc(
                DefaultErrorCodes.INVALID_CURSOR,
                "Invalid cursor value: " + cursor,
                "page[cursor]"
        );
    }

    public static ErrorsDoc badRequestInvalidPayloadErrorsDoc(String detail) {
        return badRequestErrorsDoc(
                DefaultErrorCodes.INVALID_PAYLOAD,
                detail,
                "requestBody"
        );
    }

    public static ErrorsDoc unsupportedMediaTypeErrorsDoc(String mediaType,
                                                          String expectedMediaType) {
        List<ErrorObject> errors = singletonList(
                errorObject(
                        HttpStatusCodes.SC_415_UNSUPPORTED_MEDIA_TYPE.getCode(),
                        DefaultErrorCodes.UNSUPPORTED_MEDIA_TYPE,
                        "Unsupported request media type: " + mediaType + ". Expected media type format: " + expectedMediaType
                )
        );
        return new ErrorsDoc(errors);
    }

    public static ErrorsDoc notAcceptableErrorsDoc(List<String> supportedMediaTypes) {
        List<ErrorObject> errors = singletonList(
                errorObject(
                        HttpStatusCodes.SC_406_NOT_ACCEPTABLE.getCode(),
                        DefaultErrorCodes.NOT_ACCEPTABLE,
                        "Not acceptable response media type on client. Supported media types on server: " + String.join(", ", supportedMediaTypes)
                )
        );
        return new ErrorsDoc(errors);
    }

    public static ErrorsDoc internalServerErrorsDoc() {
        List<ErrorObject> errors = singletonList(
                errorObject(
                        HttpStatusCodes.SC_500_INTERNAL_SERVER_ERROR.getCode(),
                        DefaultErrorCodes.INTERNAL_SERVER_ERROR,
                        "The service encountered an error"
                )
        );
        return new ErrorsDoc(errors);
    }

    public static ErrorsDoc internalServerErrorsDoc(String detail) {
        List<ErrorObject> errors = singletonList(
                errorObject(
                        HttpStatusCodes.SC_500_INTERNAL_SERVER_ERROR.getCode(),
                        DefaultErrorCodes.INTERNAL_SERVER_ERROR,
                        detail
                )
        );
        return new ErrorsDoc(errors);
    }

    public static ErrorsDoc badGatewayErrorsDoc(String detail) {
        List<ErrorObject> errors = singletonList(
                errorObject(
                        HttpStatusCodes.SC_502_BAD_GATEWAY_ERROR.getCode(),
                        DefaultErrorCodes.BAD_GATEWAY,
                        detail
                )
        );
        return new ErrorsDoc(errors);
    }

    public static ErrorsDoc conflictErrorsDoc(String detail) {
        List<ErrorObject> errors = singletonList(
                errorObject(
                        HttpStatusCodes.SC_409_CONFLICT.getCode(),
                        DefaultErrorCodes.CONFLICT,
                        detail
                )
        );
        return new ErrorsDoc(errors);
    }

    public static ErrorsDoc resourceNotFoundErrorsDoc(String detail) {
        List<ErrorObject> errors = singletonList(
                errorObject(
                        HttpStatusCodes.SC_404_RESOURCE_NOT_FOUND.getCode(),
                        DefaultErrorCodes.NOT_FOUND,
                        detail
                )
        );
        return new ErrorsDoc(errors);
    }

    public static ErrorsDoc resourceNotFoundErrorsDoc(String resourceName,
                                                      String resourceId) {
        StringBuilder message = new StringBuilder()
                .append("The requested resource (")
                .append(resourceName)
                .append(") ");
        if (resourceId != null && !resourceId.isEmpty()) {
            message.append("with id (").append(resourceId).append(") ");
        }
        message.append("could not be found");
        return resourceNotFoundErrorsDoc(message.toString());
    }

    public static ErrorsDoc methodNotSupportedErrorsDoc(String method) {
        List<ErrorObject> errors = singletonList(
                errorObject(
                        HttpStatusCodes.SC_405_METHOD_NOT_SUPPORTED.getCode(),
                        DefaultErrorCodes.METHOD_NOT_SUPPORTED,
                        "The resource doesnt support the requested HTTP method: " + method
                )
        );
        return new ErrorsDoc(errors);
    }

    public static ErrorObject errorObject(int status,
                                          ErrorCode code,
                                          String detail,
                                          String parameter) {
        return ErrorObject.builder()
                .id(UUID.randomUUID().toString())
                .status(String.valueOf(status))
                .code(code == null ? DefaultErrorCodes.GENERIC_REQUEST_ERROR.toCode() : code.toCode())
                .detail(detail)
                .source(parameter == null ? null : ErrorSourceObject.builder().parameter(parameter).build())
                .build();
    }

    public static ErrorObject errorObject(int status,
                                          ErrorCode code,
                                          String detail) {
        return errorObject(status, code, detail, null);
    }

}
