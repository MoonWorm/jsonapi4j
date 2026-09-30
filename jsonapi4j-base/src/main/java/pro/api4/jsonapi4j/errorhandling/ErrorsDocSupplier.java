package pro.api4.jsonapi4j.errorhandling;

import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;

/**
 * Turns an exception of type {@code T} into a JSON:API error document and the HTTP status it is answered with.
 */
public interface ErrorsDocSupplier<T extends Throwable> {

    ErrorsDoc getErrorResponse(T ex);

    int getHttpStatus(T ex);

}
