package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;
import pro.api4.jsonapi4j.request.JsonApiRequest;

/**
 * Implement this interface to let jsonapi4j framework to know how to delete a single resource by id.
 * <p>
 * Follows the JSON:API specification regarding <a href="https://jsonapi.org/format/#crud-deleting">resource deletion</a>.
 * Available under DELETE /{resourceType}/{resourceId}. No payload expected. Returns 202 if executed successfully.
 * <p>
 * Main logic is supposed to be implemented in {@link #delete(JsonApiRequest)}.
 * <p>
 * It is recommended to complement the main logic implementation with some validation logic by implementing
 * {@link #validate(JsonApiRequest)} method. This method invoked before the main logic execution.
 * All checks regarding input parameters and their formats must be done there.
 * Don't bring these checks to the {@link #delete(JsonApiRequest)}. If some error detected thrown exception will be
 * automatically processed and transformed into a valid {@link ErrorsDoc}.
 */
public interface DeleteResourceOperation extends ResourceOperation {

    String DELETE_METHOD_NAME = "delete";

    /**
     * Deletes a single resource.
     *
     * @param request incoming {@link JsonApiRequest}
     */
    void delete(JsonApiRequest request);

}
