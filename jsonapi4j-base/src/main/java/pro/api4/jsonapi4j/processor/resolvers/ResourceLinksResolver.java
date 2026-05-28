package pro.api4.jsonapi4j.processor.resolvers;

import pro.api4.jsonapi4j.model.document.LinksObject;

/**
 * Functional interface that produces the {@code "links"} member for an individual resource object.
 * <p>
 * Implementations are provided as lambdas in the processor's fluent builder chain and are
 * invoked once per resource when building the per-resource {@code "links"} member.
 *
 * @param <REQUEST>        the request type
 * @param <DATA_SOURCE_DTO> the downstream resource DTO type
 */
@FunctionalInterface
public interface ResourceLinksResolver<REQUEST, DATA_SOURCE_DTO> {

    /**
     * Produces the links object for the given resource.
     *
     * @param request       the current request
     * @param dataSourceDto the downstream resource DTO
     * @return a {@link LinksObject} for the resource; {@code null} omits the {@code "links"} member
     */
    LinksObject resolve(REQUEST request,
                        DATA_SOURCE_DTO dataSourceDto);

}
