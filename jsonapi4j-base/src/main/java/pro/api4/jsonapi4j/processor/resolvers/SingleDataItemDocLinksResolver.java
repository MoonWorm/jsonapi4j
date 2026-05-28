package pro.api4.jsonapi4j.processor.resolvers;

import pro.api4.jsonapi4j.model.document.LinksObject;

/**
 * Functional interface that produces the top-level {@code "links"} member for a JSON:API document
 * that carries a single primary resource or relationship.
 * <p>
 * Used for single-resource responses ({@code GET /users/1}, {@code POST /users}) and to-one
 * relationship responses ({@code GET /users/{id}/relationships/country}).
 * Differs from {@link ResourceLinksResolver} in that it applies to the document's top-level
 * {@code "links"}, not the individual resource object's {@code "links"}.
 *
 * @param <REQUEST>        the request type
 * @param <DATA_SOURCE_DTO> the downstream resource DTO type
 */
@FunctionalInterface
public interface SingleDataItemDocLinksResolver<REQUEST, DATA_SOURCE_DTO> {

    /**
     * Produces the top-level links object for the document.
     *
     * @param request       the current request
     * @param dataSourceDto the downstream resource DTO for the primary data
     * @return a {@link LinksObject} for the document; {@code null} omits the top-level {@code "links"}
     */
    LinksObject resolve(REQUEST request,
                        DATA_SOURCE_DTO dataSourceDto);

}
