package pro.api4.jsonapi4j.processor.resolvers;

import pro.api4.jsonapi4j.model.document.LinksObject;

import java.util.List;

@FunctionalInterface
public interface MultipleDataItemsDocLinksResolver<REQUEST, DATA_SOURCE_DTO> {

    LinksObject resolve(REQUEST request,
                        List<DATA_SOURCE_DTO> dataSourceDtos,
                        String nextCursor);

}
