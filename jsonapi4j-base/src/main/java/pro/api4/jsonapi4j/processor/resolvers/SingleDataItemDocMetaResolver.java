package pro.api4.jsonapi4j.processor.resolvers;

/**
 * Functional interface that produces the top-level {@code "meta"} member for a JSON:API document
 * that carries a single primary resource or relationship.
 * <p>
 * Used for single-resource responses ({@code GET /users/1}, {@code POST /users}) and to-one
 * relationship responses ({@code GET /users/{id}/relationships/country}).
 * Differs from {@link ResourceMetaResolver} in that it applies to the document's top-level
 * {@code "meta"}, not the individual resource object's {@code "meta"}.
 *
 * @param <REQUEST>        the request type
 * @param <DATA_SOURCE_DTO> the downstream resource DTO type
 */
@FunctionalInterface
public interface SingleDataItemDocMetaResolver<REQUEST, DATA_SOURCE_DTO> {

    /**
     * Produces the top-level meta object for the document.
     *
     * @param request       the current request
     * @param dataSourceDto the downstream resource DTO for the primary data
     * @return an arbitrary meta object; {@code null} omits the top-level {@code "meta"}
     */
    Object resolve(REQUEST request,
                   DATA_SOURCE_DTO dataSourceDto);

}
