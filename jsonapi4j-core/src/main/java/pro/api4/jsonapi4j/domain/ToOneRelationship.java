package pro.api4.jsonapi4j.domain;

import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.processor.resolvers.links.toplevel.ToOneRelationshipLinksDefaultResolvers;

/**
 * This interface is used for designing the JSON:API domain. Represents a
 * <a href="https://jsonapi.org/format/#document-resource-object-relationships">JSON:API Relationship Object</a>
 * with a 'to-one' linkage in a 'data' member.
 * <p>
 * Usually, the classes that are implementing this interface are put into beans context for better convenience.
 * Then, they should be automatically registered in {@link DomainRegistry}. If not - can be manually registered
 *  * in {@link DomainRegistry}.
 * <p>
 * This interface declares a number of methods that can be overridden if you need to customize how
 * members like 'meta', 'links' are generated for the given JSON:API relationship. Usually, you can rely on the
 * default behaviour.
 *
 * @param <RESOURCE_DTO>     a downstream object type that encapsulates internal model implementation and of the parent
 *                           JSON:API resource, e.g. Hibernate's Entity, JOOQ Record, or third-party service DTO
 * @param <RELATIONSHIP_DTO> similarly to a {@link RESOURCE_DTO} represents downstream object type, but for a given
 *                           JSON:API relationship
 */
public interface ToOneRelationship<RESOURCE_DTO, RELATIONSHIP_DTO>
        extends Relationship<RESOURCE_DTO, RELATIONSHIP_DTO> {

    /**
     * Customization point for the
     * <a href="<a href="https://jsonapi.org/format/#document-links">'links'</a>
     * member of the
     * <a href="https://jsonapi.org/format/#document-resource-object-relationships">JSON:API Relationship Object</a>
     * <p>
     * By default, generates 'self' and 'related' members of the
     * <a href="https://jsonapi.org/format/#document-links">JSON:API Links Object</a>.
     *
     * @param relationshipRequest the corresponding relationship request
     * @param relationshipDto     to-one relationship dto
     * @return an instance of {@link LinksObject}
     */
    default LinksObject resolveRelationshipLinks(JsonApiRequest relationshipRequest,
                                                 RELATIONSHIP_DTO relationshipDto) {
        return ToOneRelationshipLinksDefaultResolvers.defaultLinksResolver(
                resourceType(),
                relationshipRequest.getResourceId(),
                relationshipName(),
                this::resolveResourceIdentifierType,
                this::resolveResourceIdentifierId
        ).resolve(relationshipRequest, relationshipDto);
    }

    /**
     * Customization point for the
     * <a href="<a href="https://jsonapi.org/format/#document-meta">'meta'</a>
     * member of the
     * <a href="https://jsonapi.org/format/#document-resource-object-relationships">JSON:API Relationship Object</a>
     * <p>
     * By default, generates <code>null</code> 'meta' object.
     *
     * @param relationshipRequest the corresponding relationship request
     * @param relationshipDto     to-one relationship dto
     * @return any custom Java object that represents JSON:API meta object
     */
    default Object resolveRelationshipMeta(JsonApiRequest relationshipRequest,
                                           RELATIONSHIP_DTO relationshipDto) {
        return null;
    }

    /**
     * Customization point for the
     * <a href="<a href="https://jsonapi.org/format/#document-meta">'meta'</a>
     * member of the
     * <a href="https://jsonapi.org/format/#document-resource-identifier-objects">JSON:API Resource Identifier Object</a>
     * - a single element which forms to-one relationship's 'data' member.
     * <p>
     * By default, generates <code>null</code> 'meta' object.
     *
     * @param relationshipRequest the corresponding relationship request
     * @param relationshipDto     relationship dto that represents the
     *                            <a href="https://jsonapi.org/format/#document-resource-object-linkage">Resource Linkage</a>
     * @return any custom Java object that represents JSON:API meta object
     */
    @Override
    default Object resolveResourceIdentifierMeta(JsonApiRequest relationshipRequest,
                                                 RELATIONSHIP_DTO relationshipDto) {
        return null;
    }

}
