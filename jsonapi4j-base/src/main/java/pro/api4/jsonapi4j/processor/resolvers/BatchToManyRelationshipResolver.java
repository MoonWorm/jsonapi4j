package pro.api4.jsonapi4j.processor.resolvers;

import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipObject;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface BatchToManyRelationshipResolver<REQUEST, DATA_SOURCE_DTO> {

    /**
     * @param request
     * @param dataSourceDtos
     * @return map of resource-'id' - {@link ToManyRelationshipObject} pairs
     */
    Map<DATA_SOURCE_DTO, ToManyRelationshipObject> resolveRequestedData(REQUEST request,
                                                                        List<DATA_SOURCE_DTO> dataSourceDtos);

}
