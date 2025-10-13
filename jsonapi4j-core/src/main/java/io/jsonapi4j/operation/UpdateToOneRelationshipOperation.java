package io.jsonapi4j.operation;

import io.jsonapi4j.request.JsonApiRequest;

/**
 * Implement this interface to let jsonapi4j framework to know how to update or delete this to-one relationship
 * of the given resource.
 * <p>
 * Follows the JSON:API specification regarding <a href="https://jsonapi.org/format/#crud-updating-relationships">relationships updating</a>.
 * Available under PATCH /{resourceType}/{resourceId}/relationships/{relationshipName}.
 * To completely remove the relationship linkage - send <code>null</code> for "data" member.
 * Returns 202 if executed successfully.
 * <p>
 * Main logic is supposed to be implemented in {@link #update(JsonApiRequest)}.
 * <p>
 * It is recommended to complement the main logic implementation with some validation logic by implementing
 * {@link #validate(JsonApiRequest)} method. This method invoked before the main logic execution.
 * All checks regarding input parameters and their formats must be done there.
 * Don't bring these checks to the {@link #update(JsonApiRequest)}. If some error detected thrown exception will be
 * automatically processed and transformed into a valid {@link io.jsonapi4j.model.document.error.ErrorsDoc}.
 */
public interface UpdateToOneRelationshipOperation extends RelationshipOperation {

    /**
     * Updates or deletes the relationship for the given resource.
     * <p>
     * To completely remove the relationship linkage - send <code>null</code> for "data" member.
     *
     * @param request incoming {@link JsonApiRequest}
     */
    void update(JsonApiRequest request);

}
