package pro.api4.jsonapi4j.request;

import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import pro.api4.jsonapi4j.processor.exception.InvalidPayloadException;

import java.util.LinkedHashMap;

public interface PayloadAwareRequest {

    @SuppressWarnings("rawtypes")
    default SingleResourceDoc<ResourceObject<LinkedHashMap, LinkedHashMap>> getSingleResourceDocPayload() {
        return getSingleResourceDocPayload(LinkedHashMap.class, LinkedHashMap.class);
    }

    /**
     * Relevant for all POST/PATCH single resource operations, e.g.<code>POST /users/123</code>.
     * For the example above this method will return a payload data serialized into {@link SingleResourceDoc}.
     *
     * @param attType - java type of the 'attributes' member
     * @param relType - java type of the 'relationships' member
     * @return {@link SingleResourceDoc} object
     * @throws InvalidPayloadException if payload is invalid {@link SingleResourceDoc}
     */
    <A, R> SingleResourceDoc<ResourceObject<A, R>> getSingleResourceDocPayload(Class<A> attType, Class<R> relType);

    /**
     * Relevant for PATCH to-one relationship operations, e.g.<code>PATCH /users/123/relationships/placeOfBirth</code>.
     * For the example above this method will return a payload data serialized into {@link ToOneRelationshipDoc}.
     *
     * @return {@link ToOneRelationshipDoc} object
     * @throws InvalidPayloadException if payload is invalid {@link ToOneRelationshipDoc}
     */
    ToOneRelationshipDoc getToOneRelationshipDocPayload();

    /**
     * Relevant for PATCH to-many relationships operations, e.g.<code>PATCH /users/123/relationships/citizenships</code>.
     * For the example above this method will return a payload data serialized into {@link ToManyRelationshipsDoc}.
     *
     * @return {@link ToManyRelationshipsDoc} object
     * @throws InvalidPayloadException if payload is invalid {@link ToManyRelationshipsDoc}
     */
    ToManyRelationshipsDoc getToManyRelationshipDocPayload();

}
