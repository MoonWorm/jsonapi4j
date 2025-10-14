package pro.api4.jsonapi4j.processor.resolvers;

import java.util.List;

@FunctionalInterface
public interface MultipleDataItemsDocMetaResolver<REQUEST, DATA_SOURCE_DTO> {

    Object resolve(REQUEST request,
                   List<DATA_SOURCE_DTO> dataSourceDtos);

}
