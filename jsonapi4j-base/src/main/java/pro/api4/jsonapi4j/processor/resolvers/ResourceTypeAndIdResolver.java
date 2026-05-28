package pro.api4.jsonapi4j.processor.resolvers;

import pro.api4.jsonapi4j.processor.IdAndType;

/**
 * Functional interface that maps a downstream resource DTO to its JSON:API
 * {@code "type"} and {@code "id"} members.
 * <p>
 * Implementations are provided as lambdas in the processor's fluent builder chain and are
 * invoked once per resource when building resource objects or resource linkage.
 *
 * @param <DATA_SOURCE_DTO> the downstream resource DTO type
 */
@FunctionalInterface
public interface ResourceTypeAndIdResolver<DATA_SOURCE_DTO> {

    /**
     * Resolves the JSON:API type and id for the given resource DTO.
     *
     * @param dataSourceDto the downstream resource DTO
     * @return an {@link IdAndType} containing the resource's type and id; must not be {@code null}
     */
    IdAndType resolveTypeAndId(DATA_SOURCE_DTO dataSourceDto);

}
