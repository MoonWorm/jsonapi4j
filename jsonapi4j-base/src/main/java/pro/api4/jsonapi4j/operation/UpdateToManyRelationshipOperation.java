package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;

/**
 * Implement this interface to let jsonapi4j framework to know how to update or delete this to-many relationship
 * of the given resource.
 * <p>
 * Follows the JSON:API specification regarding <a href="https://jsonapi.org/format/#crud-updating-relationships">relationships updating</a>.
 * Available under PATCH /{resourceType}/{resourceId}/relationships/{relationshipName}.
 * Based on the specification the server must either perform a complete replacement of the relationship resource
 * linkage object(s) or return 403 Forbidden. To completely remove the relationship linkage - send <code>null</code> for
 * "data" member.
 * Returns 202 if executed successfully.
 * <p>
 * Main logic is supposed to be implemented in {@link #update(JsonApiRequest)}.
 * <p>
 * It is recommended to complement the main logic implementation with some validation logic by implementing
 * {@link #validate(JsonApiRequest)} method. This method invoked before the main logic execution.
 * All checks regarding input parameters and their formats must be done there.
 * Don't bring these checks to the {@link #update(JsonApiRequest)}. If some error detected thrown exception will be
 * automatically processed and transformed into a valid {@link ErrorsDoc}.
 */
public interface UpdateToManyRelationshipOperation extends RelationshipOperation {

    /**
     * Updates or deletes the relationship for the given resource.
     * <p>
     * Based on the specification the server must either perform a complete replacement of the relationship resource
     * linkage object(s) or return 403 Forbidden. To completely remove the relationship linkage - send <code>null</code> for
     * "data" member.
     *
     * @param request incoming {@link JsonApiRequest}
     */
    void update(JsonApiRequest request);

}
