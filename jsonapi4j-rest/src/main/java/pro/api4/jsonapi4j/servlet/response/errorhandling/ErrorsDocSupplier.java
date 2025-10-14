package pro.api4.jsonapi4j.servlet.response.errorhandling;

import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;

public interface ErrorsDocSupplier<T extends Throwable> {

    ErrorsDoc getErrorResponse(T ex);

    int getHttpStatus(T ex);

}
