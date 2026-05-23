package pro.api4.jsonapi4j.exception;

import pro.api4.jsonapi4j.model.document.error.ErrorCode;
import pro.api4.jsonapi4j.operation.validation.ErrorSources;

public record ValidationError(ErrorCode errorCode, String detail, ErrorSources.Source source) {

}
