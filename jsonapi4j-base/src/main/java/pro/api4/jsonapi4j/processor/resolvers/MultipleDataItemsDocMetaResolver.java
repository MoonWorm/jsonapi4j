package pro.api4.jsonapi4j.processor.resolvers;

import pro.api4.jsonapi4j.response.PaginationContext;

import java.util.List;

@FunctionalInterface
public interface MultipleDataItemsDocMetaResolver<REQUEST, DATA_SOURCE_DTO> {

    Object resolve(REQUEST request,
                   List<DATA_SOURCE_DTO> dataSourceDtos,
                   PaginationContext paginationContext);

}
