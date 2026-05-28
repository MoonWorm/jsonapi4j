package pro.api4.jsonapi4j.processor.resolvers;

/**
 * Functional interface that maps a downstream resource DTO to its JSON:API attributes object.
 * <p>
 * Implementations are provided as lambdas in the processor's fluent builder chain and are
 * invoked once per resource when building the {@code "attributes"} member of a resource object.
 *
 * @param <DATA_SOURCE_DTO> the downstream resource DTO type (e.g. a Hibernate entity)
 * @param <ATTRIBUTES>      the attributes object type exposed via the API
 */
@FunctionalInterface
public interface AttributesResolver<DATA_SOURCE_DTO, ATTRIBUTES> {

    /**
     * Produces the JSON:API attributes object for the given resource DTO.
     *
     * @param dto the downstream resource DTO
     * @return the attributes object; must not be {@code null}
     */
    ATTRIBUTES resolveAttributes(DATA_SOURCE_DTO dto);

}
