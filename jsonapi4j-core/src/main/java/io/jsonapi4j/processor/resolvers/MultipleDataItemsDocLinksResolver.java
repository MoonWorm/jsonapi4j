package io.jsonapi4j.processor.resolvers;

import io.jsonapi4j.model.document.LinksObject;

import java.util.List;

@FunctionalInterface
public interface MultipleDataItemsDocLinksResolver<REQUEST, DATA_SOURCE_DTO> {

    LinksObject resolve(REQUEST request,
                        List<DATA_SOURCE_DTO> dataSourceDtos,
                        String nextCursor);

}
