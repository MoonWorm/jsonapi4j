package io.jsonapi4j.processor.resolvers;

import io.jsonapi4j.model.document.LinksObject;

@FunctionalInterface
public interface ResourceLinksResolver<REQUEST, DATA_SOURCE_DTO> {

    LinksObject resolve(REQUEST request,
                        DATA_SOURCE_DTO dataSourceDto);

}
