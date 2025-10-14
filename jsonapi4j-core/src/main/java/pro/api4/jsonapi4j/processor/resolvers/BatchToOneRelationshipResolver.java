package pro.api4.jsonapi4j.processor.resolvers;

import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface BatchToOneRelationshipResolver<REQUEST, DATA_SOURCE_DTO> {

    /**
     * @param request
     * @param dataSourceDtos
     * @return map of resource-'id' - {@link ToOneRelationshipDoc} pairs
     */
    Map<DATA_SOURCE_DTO, ToOneRelationshipDoc> resolveRequestedData(REQUEST request,
                                                                    List<DATA_SOURCE_DTO> dataSourceDtos);

}
