package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;
import pro.api4.jsonapi4j.request.JsonApiRequest;

/**
 * Implement this interface to let jsonapi4j framework to know how to add members to this to-many relationship
 * of the given resource.
 * <p>
 * Follows the JSON:API specification regarding
 * <a href="https://jsonapi.org/format/#crud-updating-to-many-relationships">updating to-many relationships</a>.
 * Available under POST /{resourceType}/{resourceId}/relationships/{relationshipName}.
 * Based on the specification the server must add the specified members to the relationship unless they already exist.
 * If a given type and id is already in the relationship, the server must not add it again.
 * Returns 204 No Content if executed successfully.
 * <p>
 * Main logic is supposed to be implemented in {@link #add(JsonApiRequest)}.
 * <p>
 * It is recommended to complement the main logic implementation with some validation logic by implementing
 * {@link #validate(JsonApiRequest)} method. This method invoked before the main logic execution.
 * All checks regarding input parameters and their formats must be done there.
 * Don't bring these checks to the {@link #add(JsonApiRequest)}. If some error detected thrown exception will be
 * automatically processed and transformed into a valid {@link ErrorsDoc}.
 */
public interface AddToManyRelationshipOperation extends RelationshipOperation {

    String ADD_MANY_METHOD_NAME = "add";

    /**
     * Adds members to the to-many relationship for the given resource.
     * <p>
     * Based on the specification the server must add the specified members to the relationship unless they already exist.
     * If a given type and id is already in the relationship, the server must not add it again.
     *
     * @param request incoming {@link JsonApiRequest}
     */
    void add(JsonApiRequest request);

}
