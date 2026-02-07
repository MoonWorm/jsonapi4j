package pro.api4.jsonapi4j.processor.resolvers;

import pro.api4.jsonapi4j.processor.IdAndType;

@FunctionalInterface
public interface ResourceTypeAndIdResolver<DATA_SOURCE_DTO> {

    IdAndType resolveTypeAndId(DATA_SOURCE_DTO dataSourceDto);

}
