package io.jsonapi4j.operation;

import io.jsonapi4j.request.JsonApiRequest;

/**
 * Implement this interface to let jsonapi4j framework to know how to resolve this to-one relationship of the given resource.
 * <p>
 * Follows the JSON:API specification regarding <a href="https://jsonapi.org/format/#fetching-relationships">relationships fetching</a>.
 * Available under GET /{resourceType}/{resourceId}/relationships/{relationshipName}. Returns 200 if executed successfully.
 * <p>
 * The framework also uses this implementation for scenarios when the client request a single or multiple
 * primary resources but also requests (via include="foo,bar") to resolve this relationship.
 * <p>
 * Main logic is supposed to be implemented in {@link #read(JsonApiRequest)}.
 * <p>
 * It is recommended to complement the main logic implementation with some validation logic by implementing
 * {@link #validate(JsonApiRequest)} method. This method invoked before the main logic execution.
 * All checks regarding input parameters and their formats must be done there.
 * Don't bring these checks to the {@link #read(JsonApiRequest)}. If some error detected thrown exception will be
 * automatically processed and transformed into a valid {@link io.jsonapi4j.model.document.error.ErrorsDoc}.
 *
 * @param <RESOURCE_DTO>     a downstream object type that encapsulates internal model implementation and of the parent
 *                           JSON:API resource, e.g. Hibernate's Entity, JOOQ Record, or third-party service DTO
 * @param <RELATIONSHIP_DTO> downstream dto object type that represents type of relationship's resource identifiers
 */
public interface ReadToOneRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO> extends RelationshipOperation {

    /**
     * Triggered when the standalone request is sent for this relationship.
     * (e.g. /{resourceType}/relationships/{relationshipName}).
     * <p>
     * Reads a resource linkage object that relates to this relationship.
     *
     * @param relationshipRequest incoming {@link JsonApiRequest}
     * @return {@link RELATIONSHIP_DTO} item
     */
    RELATIONSHIP_DTO read(JsonApiRequest relationshipRequest);


    /**
     * Triggered when querying the primary resource(s) as part of its relationship resolution
     * (e.g. /{resourceType}?include={relationshipName}).
     * <p>
     * This method can be overridden if relationship information is 'nested' into parent {@link RESOURCE_DTO} due to specifics
     * of the internal model implementation. This enables 'in-house' logic that builds 'data' member of the
     * <a href="https://jsonapi.org/format/#document-resource-object-relationships">JSON:API Relationship Object</a>
     * and effectively means that no operations will be triggered in order to resolve these JSON:API linkages.
     * <p>
     * By default, falling back to {@link #read(JsonApiRequest)} invocation that triggers an external operation.
     *
     * @param relationshipRequest relationship {@link JsonApiRequest} that is composed during primary resource request
     *                            processing from the original {@link JsonApiRequest} and {@link RESOURCE_DTO}
     * @param resourceDto         parent resource DTO
     * @return {@link RELATIONSHIP_DTO} item
     */
    default RELATIONSHIP_DTO readForResource(JsonApiRequest relationshipRequest,
                                             RESOURCE_DTO resourceDto) {
        return read(relationshipRequest);
    }

}
