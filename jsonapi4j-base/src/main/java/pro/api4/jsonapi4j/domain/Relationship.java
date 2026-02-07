package pro.api4.jsonapi4j.domain;

import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.request.JsonApiRequest;

/**
 * Base interface for {@link ToManyRelationship} and {@link ToOneRelationship}. Encapsulates common logic of any type of
 * <a href="https://jsonapi.org/format/#document-resource-object-relationships">JSON:API Relationship Object</a>.
 * <p>
 * Must not be used directly. Applications must extend either {@link ToManyRelationship} or {@link ToOneRelationship}.
 */
public interface Relationship<RELATIONSHIP_DTO> {

    LinksObject NOT_IMPLEMENTED_LINKS_STUB = LinksObject.builder().build();

    /**
     * Resolves relationship's resource linkage "type" of the "data" member.
     *
     * @param relationshipDto the corresponding {@link RELATIONSHIP_DTO}, can represent multiple resource types
     * @return a String that represents the resource type ("type" member) of the
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
     * public String resolveResourceIdentifierType(UserProperty userPropertyDto) {
     *      if (userPropertyDto.getType() == APARTMENTS) {
     *          return "apartments";
     *      } else if (userPropertyDto.getType() == CARS) {
     *          return "cars";
     *      }
     * }
     * }
     * </pre>
     */
    String resolveResourceIdentifierType(RELATIONSHIP_DTO relationshipDto);

    /**
     * Resolves relationship's resource linkage "id" of the "data" member.
     *
     * @param relationshipDto the corresponding relationship dto
     * @return unique identifier ("id" member) of the relationship's
     * <a href="https://jsonapi.org/format/#document-resource-object-linkage">resource linkage object</a>.
     */
    String resolveResourceIdentifierId(RELATIONSHIP_DTO relationshipDto);

    /**
     * Customization point for the
     * <a href="<a href="https://jsonapi.org/format/#document-meta">'meta'</a>
     * member of the
     * <a href="https://jsonapi.org/format/#document-resource-identifier-objects">JSON:API Resource Identifier Object</a>
     *
     * @param relationshipRequest the corresponding relationship request
     * @param relationshipDto     relationship dto that represents the
     *                            <a href="https://jsonapi.org/format/#document-resource-object-linkage">Resource Linkage</a>
     * @return any custom Java object that represents JSON:API meta object
     */
    Object resolveResourceIdentifierMeta(JsonApiRequest relationshipRequest, RELATIONSHIP_DTO relationshipDto);

}
