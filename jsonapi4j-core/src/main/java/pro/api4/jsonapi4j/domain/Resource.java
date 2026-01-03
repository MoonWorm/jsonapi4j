package pro.api4.jsonapi4j.domain;

import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.request.JsonApiRequest;

import java.util.List;

/**
 * This interface is used for designing the JSON:API domain. Represents a
 * <a href="https://jsonapi.org/format/#document-resource-objects">JSON:API Resource Object</a>
 * <p>
 * Usually, the classes that are implementing this interface are put into beans context for better convenience.
 * Then, they should be automatically registered in {@link DomainRegistry}. If not - can be manually registered
 * in {@link DomainRegistry}.
 * <p>
 * This interface declares a number of methods that can be overridden if you need to customize how
 * members like 'meta', 'links' are generated for the given JSON:API resource. Usually, you can rely on the
 * default behaviour.
 *
 * @param <RESOURCE_DTO> a downstream object type that encapsulates internal model implementation and of this
 *                       JSON:API resource, e.g. Hibernate's Entity, JOOQ Record, or third-party service DTO
 */
public interface Resource<RESOURCE_DTO> {

    LinksObject NOT_IMPLEMENTED_LINKS_STUB = LinksObject.builder().build();

    /**
     * Resolves the resource unique identifier ("id" member) based on the corresponding downstream {@link RESOURCE_DTO}.
     * <p>
     * Must be unique across the current resource type.
     *
     * @param dataSourceDto the corresponding downstream {@link RESOURCE_DTO}
     * @return unique resource's id
     */
    String resolveResourceId(RESOURCE_DTO dataSourceDto);

    /**
     * Maps a downstream {@link RESOURCE_DTO} object into an API-facing attributes object ("attributes" member). Read more
     * information in the spec: <a href="https://jsonapi.org/format/#document-resource-object-attributes">JSON:API Attributes Object</a>
     * <p>
     * Refer <a href="https://jsonapi.org/format/#document-resource-object-attributes">JSON:API Attributes Object</a> for more details.
     *
     * @param dataSourceDto the corresponding downstream {@link RESOURCE_DTO} object
     *
     * @return API-facing custom attributes object
     */
    default Object resolveAttributes(RESOURCE_DTO dataSourceDto) {
        return null;
    }

    /**
     * Customization point for the
     * <a href="<a href="https://jsonapi.org/format/#document-links">'links'</a>
     * member of the
     * <a href="https://jsonapi.org/format/#document-top-level">JSON:API Top Level Object</a>
     * for a documents that have a single resource as their primary data ("data" member).
     * <p>
     * By default, generates a links object with "self" member only:
     * {@snippet :
     * "links": {
     *     "self": "/users/5"
     * }
     * }
     *
     * @param request       the original JSON:API request
     * @param dataSourceDto an instance of the corresponding {@link RESOURCE_DTO}
     * @return an instance of {@link LinksObject}
     */
    default LinksObject resolveTopLevelLinksForSingleResourceDoc(JsonApiRequest request,
                                                                 RESOURCE_DTO dataSourceDto) {
        return NOT_IMPLEMENTED_LINKS_STUB;
    }

    /**
     * Customization point for the
     * <a href="<a href="https://jsonapi.org/format/#document-links">'links'</a>
     * member of the
     * <a href="https://jsonapi.org/format/#document-top-level">JSON:API Top Level Object</a>
     * for a documents that have an array of resources as their primary data ("data" member).
     * <p>
     * By default, generates a links object with "self" and "next" members if applicable:
     * {@snippet :
     * "links": {
     *     "self": "/users?page[cursor]=DoJu",
     *     "next: "/users?page[cursor]=DoJw"
     * }
     * }
     *
     * @param request        the original JSON:API request
     * @param dataSourceDtos a list of the corresponding {@link RESOURCE_DTO}
     * @param nextCursor     next cursor string
     * @return an instance of {@link LinksObject}
     */
    default LinksObject resolveTopLevelLinksForMultiResourcesDoc(JsonApiRequest request,
                                                                 List<RESOURCE_DTO> dataSourceDtos,
                                                                 String nextCursor) {
        return NOT_IMPLEMENTED_LINKS_STUB;
    }

    /**
     * Customization point for the
     * <a href="<a href="https://jsonapi.org/format/#document-meta">'meta'</a>
     * member of the
     * <a href="https://jsonapi.org/format/#document-top-level">JSON:API Top Level Object</a>
     * for a documents that have a single resource as their primary data ("data" member).
     * <p>
     * By default, generates a <code>null</code> meta.
     *
     * @param request       the original JSON:API request
     * @param dataSourceDto an instance of the corresponding {@link RESOURCE_DTO}
     * @return an instance of {@link LinksObject}
     */
    default Object resolveTopLevelMetaForSingleResourceDoc(JsonApiRequest request,
                                                           RESOURCE_DTO dataSourceDto) {
        return null;
    }

    /**
     * Customization point for the
     * <a href="<a href="https://jsonapi.org/format/#document-meta">'meta'</a>
     * member of the
     * <a href="https://jsonapi.org/format/#document-top-level">JSON:API Top Level Object</a>
     * for a documents that have an array of resources as their primary data ("data" member).
     * <p>
     * By default, generates a <code>null</code> meta.
     *
     * @param request        the original JSON:API request
     * @param dataSourceDtos a list of the corresponding {@link RESOURCE_DTO}
     * @return an instance of {@link LinksObject}
     */
    default Object resolveTopLevelMetaForMultiResourcesDoc(JsonApiRequest request,
                                                           List<RESOURCE_DTO> dataSourceDtos) {
        return null;
    }

    /**
     * Customization point for the
     * <a href="<a href="https://jsonapi.org/format/#document-links">'links'</a>
     * member of the
     * <a href="https://jsonapi.org/format/#document-resource-objects">JSON:API Resource Object</a>
     * <p>
     * By default, generates a links object with a "self" member:
     * {@snippet :
     * "links": {
     *    "self": "/users/5"
     * }
     * }
     *
     * @param request       the original JSON:API request
     * @param dataSourceDto an instance of the corresponding {@link RESOURCE_DTO}
     * @return an instance of {@link LinksObject}
     */
    default LinksObject resolveResourceLinks(JsonApiRequest request,
                                             RESOURCE_DTO dataSourceDto) {
        return NOT_IMPLEMENTED_LINKS_STUB;
    }

    /**
     * Customization point for the
     * <a href="<a href="https://jsonapi.org/format/#document-meta">'meta'</a>
     * member of the
     * <a href="https://jsonapi.org/format/#document-resource-objects">JSON:API Resource Object</a>
     * <p>
     * By default, generates a <code>null</code> meta.
     *
     * @param request       the original JSON:API request
     * @param dataSourceDto an instance of the corresponding {@link RESOURCE_DTO}
     * @return an instance of {@link LinksObject}
     */
    default Object resolveResourceMeta(JsonApiRequest request,
                                       RESOURCE_DTO dataSourceDto) {
        return null;
    }

}

