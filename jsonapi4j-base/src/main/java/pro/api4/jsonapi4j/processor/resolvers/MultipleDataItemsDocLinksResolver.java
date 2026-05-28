package pro.api4.jsonapi4j.processor.resolvers;

import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.response.PaginationContext;

import java.util.List;

/**
 * Functional interface that produces the top-level {@code "links"} member for a JSON:API document
 * that carries multiple primary resources or relationship members.
 * <p>
 * Used for multi-resource responses ({@code GET /users}) and to-many relationship responses
 * ({@code GET /users/{id}/relationships/roles}).
 * Pagination metadata is provided via {@link PaginationContext} so that implementations can
 * include {@code "first"}, {@code "prev"}, {@code "next"}, and {@code "last"} pagination links.
 *
 * @param <REQUEST>        the request type
 * @param <DATA_SOURCE_DTO> the downstream resource DTO type
 */
@FunctionalInterface
public interface MultipleDataItemsDocLinksResolver<REQUEST, DATA_SOURCE_DTO> {

    /**
     * Produces the top-level links object for the document.
     *
     * @param request           the current request
     * @param dataSourceDtos    the full list of downstream resource DTOs for the primary data
     * @param paginationContext pagination metadata (cursors, counts, page info)
     * @return a {@link LinksObject} for the document; {@code null} omits the top-level {@code "links"}
     */
    LinksObject resolve(REQUEST request,
                        List<DATA_SOURCE_DTO> dataSourceDtos,
                        PaginationContext paginationContext);

}
