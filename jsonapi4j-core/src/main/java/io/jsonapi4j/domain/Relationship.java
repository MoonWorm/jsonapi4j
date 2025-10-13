package io.jsonapi4j.domain;

import io.jsonapi4j.plugin.RelationshipPluginAware;
import io.jsonapi4j.plugin.ac.DefaultAccessControlEvaluator;
import io.jsonapi4j.plugin.ac.model.AccessControlRequirements;
import io.jsonapi4j.request.JsonApiRequest;
import io.jsonapi4j.plugin.ac.ownership.DefaultOwnerIdExtractor;
import io.jsonapi4j.plugin.ac.ownership.OwnerIdExtractor;

/**
 * Base interface for {@link ToManyRelationship} and {@link ToOneRelationship}. Encapsulates common logic of any type of
 * <a href="https://jsonapi.org/format/#document-resource-object-relationships">JSON:API Relationship Object</a>.
 * <p>
 * Must not be used directly. Applications must extend either {@link ToManyRelationship} or {@link ToOneRelationship}.
 */
public interface Relationship<RESOURCE_DTO, RELATIONSHIP_DTO>
        extends Comparable<Relationship<RESOURCE_DTO, RELATIONSHIP_DTO>>, RelationshipPluginAware {

    /**
     * @return an instance of {@link RelationshipName} that represents the name of the relationship e.g.
     * "userProperties", "userCitizenships", etc.
     */
    RelationshipName relationshipName();


    /**
     * @return an instance of {@link ResourceType} that represents the resource type of the parent resource.
     * <p>
     * For example, for "userCitizenships" relationships this is supposed to return "users".
     */
    ResourceType parentResourceType();

    /**
     * Resolves relationship's resource linkage "type" of the "data" member.
     *
     * @param relationshipDto the corresponding {@link RELATIONSHIP_DTO}, can represent multiple resource types
     * @return an instance of {@link ResourceType} that represents the resource type ("type" member) of the
     * relationship's
     * <a href="https://jsonapi.org/format/#document-resource-object-linkage">resource linkage object</a>.
     * Can return different types, because a resource might have a relationship of the mixed resource types.
     * <p>
     * For example, "users" resource has "userProperty" relationship. This method can return "apartments", "cars", etc.
     * depending on the {@link RELATIONSHIP_DTO} details:
     *
     * <pre>
     * {@code
     * @Override
     * public ResourceType resolveResourceIdentifierType(UserProperty userPropertyDto) {
     *      if (userPropertyDto.getType() == APARTMENTS) {
     *          return () -> "apartments";
     *      } else if (userPropertyDto.getType() == CARS) {
     *          return () -> "cars";
     *      }
     * }
     * }
     * </pre>
     */
    ResourceType resolveResourceIdentifierType(RELATIONSHIP_DTO relationshipDto);

    /**
     * Resolves relationship's resource linkage "id" of the "data" member.
     *
     * @param relationshipDto the corresponding relationship dto
     * @return unique identifier ("id" member) of the relationship's
     * <a href="https://jsonapi.org/format/#document-resource-object-linkage">resource linkage object</a>.
     */
    String resolveResourceIdentifierId(RELATIONSHIP_DTO relationshipDto);

    /**
     * Composes a new instance of {@link JsonApiRequest} that represents a nested request for reading relationship data for the
     * parent resource. That is only used for a Compound Documents scenarios when relationship was requested in 'include' query parameter
     * while dealing with resources.
     * <p/>
     * Defaults to {@link JsonApiRequest#composeRelationshipRequest(String, Relationship)} and resolves resource id
     * based on {@link Resource#resolveResourceId(Object)} implementation of the parent resource.
     * Can be overridden here.
     * <p/>
     * In addition to a data fetching this request can be also used in order to evaluate access to relationship data
     * from an ownership perspective. See {@link DefaultAccessControlEvaluator#evaluateInboundRequirements(Object, AccessControlRequirements)}
     * for more details. Owner id is extracted from the relationship request {@link JsonApiRequest}.
     * It's possible to implement your own {@link OwnerIdExtractor}.
     * (default impl - {@link DefaultOwnerIdExtractor} assuming owner id is coming as a parent resource id).
     *
     * @param originalRequest     original JsonApiRequest
     * @param resourceDto downstream DTO that represents parent resource
     * @return relationship JsonApiRequest
     */
    default JsonApiRequest constructRelationshipRequest(JsonApiRequest originalRequest,
                                                        RESOURCE_DTO resourceDto) {
        return null;
    }

    @Override
    default int compareTo(Relationship o) {
        int result = this.parentResourceType().getType().compareTo(o.parentResourceType().getType());
        if (result != 0) return result;
        return this.relationshipName().getName().compareTo(o.relationshipName().getName());
    }

}
