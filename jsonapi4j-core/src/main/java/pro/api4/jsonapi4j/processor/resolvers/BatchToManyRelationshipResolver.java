package pro.api4.jsonapi4j.processor.resolvers;

import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface BatchToManyRelationshipResolver<REQUEST, DATA_SOURCE_DTO> {

    /**
     * @param request
     * @param dataSourceDtos
     * @return map of resource-'id' - {@link ToManyRelationshipsDoc} pairs
     */
    Map<DATA_SOURCE_DTO, ToManyRelationshipsDoc> resolveRequestedData(REQUEST request,
                                                                      List<DATA_SOURCE_DTO> dataSourceDtos);

}
