package pro.api4.jsonapi4j.processor.resolvers;

import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;

@FunctionalInterface
public interface ToManyRelationshipResolver<REQUEST, DATA_SOURCE_DTO> {

    ToManyRelationshipsDoc resolveRequestedData(REQUEST request,
                                                DATA_SOURCE_DTO dataSourceDto);

}
