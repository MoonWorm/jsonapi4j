package pro.api4.jsonapi4j.processor.resolvers;

import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipObject;

@FunctionalInterface
public interface ToOneRelationshipResolver<REQUEST, DATA_SOURCE_DTO> {

    ToOneRelationshipObject resolveRequestedData(REQUEST request,
                                                 DATA_SOURCE_DTO dataSourceDto);

}
