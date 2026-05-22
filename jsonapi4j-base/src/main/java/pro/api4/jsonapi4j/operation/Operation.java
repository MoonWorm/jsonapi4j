package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;
import pro.api4.jsonapi4j.request.JsonApiRequest;

/**
 * Base interface for all resource operations.
 */
public interface Operation {

    /**
     * It is recommended to complement the main logic implementation with some validation logic by implementing
     * this method. This method invoked before the main logic execution.
     * All checks regarding input parameters, payload and their formats must be done there.
     * Don't bring these checks to the main logic. If some error detected thrown exception will be
     * automatically processed and transformed into a valid {@link ErrorsDoc}.
     * <p>
     * By default, no validation implemented.
     * <p>
     * Must throw an exception if validation failed. Check DefaultErrorHandlerFactory, Jsr380ErrorHandlers,
     * etc. for more details.
     *
     * @param request incoming {@link JsonApiRequest}
     */
    void validate(JsonApiRequest request);

}
