package pro.api4.jsonapi4j.processor.resolvers;


/**
 * Functional interface that produces the {@code "meta"} member for an individual resource object.
 * <p>
 * Implementations are provided as lambdas in the processor's fluent builder chain and are
 * invoked once per resource when building the per-resource {@code "meta"} member.
 *
 * @param <REQUEST>        the request type
 * @param <DATA_SOURCE_DTO> the downstream resource DTO type
 */
@FunctionalInterface
public interface ResourceMetaResolver<REQUEST, DATA_SOURCE_DTO> {

    /**
     * Produces the meta object for the given resource.
     *
     * @param request       the current request
     * @param dataSourceDto the downstream resource DTO
     * @return an arbitrary meta object; {@code null} omits the {@code "meta"} member
     */
    Object resolve(REQUEST request,
                   DATA_SOURCE_DTO dataSourceDto);

}
