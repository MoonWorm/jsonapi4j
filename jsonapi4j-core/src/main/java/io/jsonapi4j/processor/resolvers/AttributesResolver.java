package io.jsonapi4j.processor.resolvers;

@FunctionalInterface
public interface AttributesResolver<DATA_SOURCE_DTO, ATTRIBUTES> {

    ATTRIBUTES resolveAttributes(DATA_SOURCE_DTO dto);

}
