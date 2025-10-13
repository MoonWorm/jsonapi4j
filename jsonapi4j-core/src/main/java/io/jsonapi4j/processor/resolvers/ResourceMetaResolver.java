package io.jsonapi4j.processor.resolvers;


@FunctionalInterface
public interface ResourceMetaResolver<REQUEST, DATA_SOURCE_DTO> {

    Object resolve(REQUEST request,
                   DATA_SOURCE_DTO dataSourceDto);

}
