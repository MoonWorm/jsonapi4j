package io.jsonapi4j.processor.resolvers;

import io.jsonapi4j.processor.IdAndType;

@FunctionalInterface
public interface ResourceTypeAndIdResolver<DATA_SOURCE_DTO> {

    IdAndType resolveTypeAndId(DATA_SOURCE_DTO dataSourceDto);

}
