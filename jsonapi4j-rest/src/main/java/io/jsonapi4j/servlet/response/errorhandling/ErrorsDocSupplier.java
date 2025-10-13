package io.jsonapi4j.servlet.response.errorhandling;

import io.jsonapi4j.model.document.error.ErrorsDoc;

public interface ErrorsDocSupplier<T extends Throwable> {

    ErrorsDoc getErrorResponse(T ex);

    int getHttpStatus(T ex);

}
