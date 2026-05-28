package pro.api4.jsonapi4j.domain;

import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.response.PaginationContext;

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

    String RESOLVE_ATTRIBUTES_METHOD_NAME = "resolveAttributes";
    String RESOLVE_RESOURCE_LINKS_METHOD_NAME = "resolveResourceLinks";
    String RESOLVE_RESOURCE_META_METHOD_NAME = "resolveResourceMeta";

    LinksObject NOT_IMPLEMENTED_LINKS_STUB = LinksObject.builder().build();
    Object NOT_IMPLEMENTED_META_STUB = new Object();

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
     * <a href="https://jsonapi.org/format/#document-links">{@code "links"}</a>
     * member of the
     * <a href="https://jsonapi.org/format/#document-top-level">JSON:API Top Level Object</a>
     * for documents that have a single resource as their primary data ({@code "data"} member).
     * <p>
     * By default, returns {@link #NOT_IMPLEMENTED_LINKS_STUB} which causes the framework to
     * generate a links object with a {@code "self"} member only:
     * {@snippet :
     * "links": {
     *     "self": "/users/5"
     * }
     * }
     *
     * @param request       the original JSON:API request
     * @param dataSourceDto an instance of the corresponding downstream DTO
     * @return a {@link LinksObject} for the top-level document links
     */
    default LinksObject resolveTopLevelLinksForSingleResourceDoc(JsonApiRequest request,
                                                                 RESOURCE_DTO dataSourceDto) {
        return NOT_IMPLEMENTED_LINKS_STUB;
    }

    /**
     * Customization point for the
     * <a href="https://jsonapi.org/format/#document-links">{@code "links"}</a>
     * member of the
     * <a href="https://jsonapi.org/format/#document-top-level">JSON:API Top Level Object</a>
     * for documents that have an array of resources as their primary data ({@code "data"} member).
     * <p>
     * By default, returns {@link #NOT_IMPLEMENTED_LINKS_STUB} which causes the framework to
     * generate a links object with {@code "self"} and {@code "next"} members if applicable:
     * {@snippet :
     * "links": {
     *     "self": "/users?page[cursor]=DoJu",
     *     "next": "/users?page[cursor]=DoJw"
     * }
     * }
     *
     * @param request           the original JSON:API request
     * @param dataSourceDtos    a list of the downstream DTOs for the current page
     * @param paginationContext pagination metadata (cursors, total counts)
     * @return a {@link LinksObject} for the top-level document links
     */
    default LinksObject resolveTopLevelLinksForMultiResourcesDoc(JsonApiRequest request,
                                                                 List<RESOURCE_DTO> dataSourceDtos,
                                                                 PaginationContext paginationContext) {
        return NOT_IMPLEMENTED_LINKS_STUB;
    }

    /**
     * Customization point for the
     * <a href="https://jsonapi.org/format/#document-meta">{@code "meta"}</a>
     * member of the
     * <a href="https://jsonapi.org/format/#document-top-level">JSON:API Top Level Object</a>
     * for documents that have a single resource as their primary data ({@code "data"} member).
     * <p>
     * By default, returns {@code null} (no top-level meta).
     *
     * @param request       the original JSON:API request
     * @param dataSourceDto an instance of the corresponding downstream DTO
     * @return an arbitrary meta object, or {@code null} to omit the top-level {@code "meta"} member
     */
    default Object resolveTopLevelMetaForSingleResourceDoc(JsonApiRequest request,
                                                           RESOURCE_DTO dataSourceDto) {
        return null;
    }

    /**
     * Customization point for the
     * <a href="https://jsonapi.org/format/#document-meta">{@code "meta"}</a>
     * member of the
     * <a href="https://jsonapi.org/format/#document-top-level">JSON:API Top Level Object</a>
     * for documents that have an array of resources as their primary data ({@code "data"} member).
     * <p>
     * By default, returns {@link #NOT_IMPLEMENTED_META_STUB} which causes the framework to
     * omit the top-level {@code "meta"} member.
     *
     * @param request           the original JSON:API request
     * @param dataSourceDtos    a list of the downstream DTOs for the current page
     * @param paginationContext pagination metadata (cursors, total counts)
     * @return an arbitrary meta object, or {@code null} to omit the top-level {@code "meta"} member
     */
    default Object resolveTopLevelMetaForMultiResourcesDoc(JsonApiRequest request,
                                                           List<RESOURCE_DTO> dataSourceDtos,
                                                           PaginationContext paginationContext) {
        return NOT_IMPLEMENTED_META_STUB;
    }

    /**
     * Customization point for the
     * <a href="https://jsonapi.org/format/#document-links">{@code "links"}</a>
     * member of the
     * <a href="https://jsonapi.org/format/#document-resource-objects">JSON:API Resource Object</a>
     * <p>
     * By default, returns {@link #NOT_IMPLEMENTED_LINKS_STUB} which causes the framework to
     * generate a links object with a {@code "self"} member:
     * {@snippet :
     * "links": {
     *    "self": "/users/5"
     * }
     * }
     *
     * @param request       the original JSON:API request
     * @param dataSourceDto an instance of the corresponding downstream DTO
     * @return a {@link LinksObject} for the per-resource links
     */
    default LinksObject resolveResourceLinks(JsonApiRequest request,
                                             RESOURCE_DTO dataSourceDto) {
        return NOT_IMPLEMENTED_LINKS_STUB;
    }

    /**
     * Customization point for the
     * <a href="https://jsonapi.org/format/#document-meta">{@code "meta"}</a>
     * member of the
     * <a href="https://jsonapi.org/format/#document-resource-objects">JSON:API Resource Object</a>
     * <p>
     * By default, returns {@code null} (no per-resource meta).
     *
     * @param request       the original JSON:API request
     * @param dataSourceDto an instance of the corresponding downstream DTO
     * @return an arbitrary meta object, or {@code null} to omit the per-resource {@code "meta"} member
     */
    default Object resolveResourceMeta(JsonApiRequest request,
                                       RESOURCE_DTO dataSourceDto) {
        return null;
    }

}

