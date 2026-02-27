package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;
import pro.api4.jsonapi4j.request.JsonApiRequest;

import java.util.function.Consumer;

/**
 * Implement this interface to let jsonapi4j framework know how to remove one or many linkage objects
 * from this to-many relationship of the given resource.
 * <p>
 * Follows the JSON:API specification regarding
 * <a href="https://jsonapi.org/format/#crud-updating-to-many-relationships">to-many relationships update</a>.
 * Available under DELETE /{resourceType}/{resourceId}/relationships/{relationshipName}.
 * Returns 202 in legacy mode and 204 in strict mode if executed successfully.
 * <p>
 * Main logic is supposed to be implemented in {@link #remove(JsonApiRequest)}.
 * <p>
 * It is recommended to complement the main logic implementation with some validation logic by implementing
 * {@link #validate(JsonApiRequest)} method. This method is invoked before the main logic execution.
 * All checks regarding input parameters and their formats must be done there.
 * Don't bring these checks to the {@link #remove(JsonApiRequest)}. If some error is detected, thrown exception will be
 * automatically processed and transformed into a valid {@link ErrorsDoc}.
 */
public interface RemoveFromManyRelationshipOperation extends RelationshipOperation {

    Consumer<JsonApiRequest> DEFAULT_VALIDATOR = RelationshipOperation.DEFAULT_VALIDATOR;

    /**
     * Removes one or many linkage objects from this to-many relationship.
     *
     * @param request incoming {@link JsonApiRequest}
     */
    void remove(JsonApiRequest request);

}
