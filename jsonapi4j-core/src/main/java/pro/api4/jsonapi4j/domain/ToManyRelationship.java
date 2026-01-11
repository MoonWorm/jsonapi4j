package pro.api4.jsonapi4j.domain;

import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.model.document.LinksObject;

import java.util.List;

/**
 * This interface is used for designing the JSON:API domain. Represents a
 * <a href="https://jsonapi.org/format/#document-resource-object-relationships">JSON:API Relationship Object</a>
 * with a 'to-many' linkages in a 'data' member.
 * <p>
 * Usually, the classes that are implementing this interface are put into beans context for better convenience.
 * Then, they should be automatically registered in {@link DomainRegistry}. If not - can be manually registered
 *  * in {@link DomainRegistry}.
 * <p>
 * This interface declares a number of methods that can be overridden if you need to customize how
 * members like 'meta', 'links' are generated for the given JSON:API relationship. Usually, you can rely on the
 * default behaviour.
 *
 * @param <RELATIONSHIP_DTO> represents downstream object type, but for a given JSON:API relationship
 */
public interface ToManyRelationship<RELATIONSHIP_DTO> extends Relationship<RELATIONSHIP_DTO> {

    /**
     * Customization point for the
     * <a href="<a href="https://jsonapi.org/format/#document-links">'links'</a>
     * member of the
     * <a href="https://jsonapi.org/format/#document-resource-object-relationships">JSON:API Relationship Object</a>
     * <p>
     * By default, generates 'self' and 'related' members:
     * {@snippet :
     * "links": {
     *     "self": "/countries/NO/relationships/currencies",
     *     "related": {
     *        "currencies": {
     *           "href": "/currencies?filter[id]=NOK",
     *           "describedby": "https://api4.pro/oas-schema-to-many-relationships-related-link.yaml",
     *           "meta": {
     *              "ids": ["NOK"]
     *           }
     *        }
     *    }
     * }
     * }
     *
     * @param relationshipRequest the corresponding relationship request
     * @param relationshipDtos    to-many relationship dtos
     * @param nextCursor          next cursor string
     * @return an instance of {@link LinksObject}
     */
    default LinksObject resolveRelationshipLinks(JsonApiRequest relationshipRequest,
                                                 List<RELATIONSHIP_DTO> relationshipDtos,
                                                 String nextCursor) {
        return NOT_IMPLEMENTED_LINKS_STUB;
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
     * @param relationshipDtos    to-many relationship dtos
     * @return any custom Java object that represents JSON:API meta object
     */
    default Object resolveRelationshipMeta(JsonApiRequest relationshipRequest,
                                           List<RELATIONSHIP_DTO> relationshipDtos) {
        return null;
    }

    /**
     * Customization point for the
     * <a href="<a href="https://jsonapi.org/format/#document-meta">'meta'</a>
     * member of the
     * <a href="https://jsonapi.org/format/#document-resource-identifier-objects">JSON:API Resource Identifier Object</a>
     * - an array of which forms to-many relationship's 'data' member.
     * <p>
     * By default, generates <code>null</code> 'meta' object.
     *
     * @param relationshipRequest the corresponding relationship request
     * @param relationshipDto     relationship dto that represents the corresponding element of the
     *                            <a href="https://jsonapi.org/format/#document-resource-object-linkage">Resource Linkage</a>
     * @return any custom Java object that represents JSON:API meta object
     */
    @Override
    default Object resolveResourceIdentifierMeta(JsonApiRequest relationshipRequest,
                                                 RELATIONSHIP_DTO relationshipDto) {
        return null;
    }

}
