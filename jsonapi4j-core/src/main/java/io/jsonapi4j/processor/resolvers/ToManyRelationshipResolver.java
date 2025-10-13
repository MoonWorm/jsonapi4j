package io.jsonapi4j.processor.resolvers;

import io.jsonapi4j.model.document.data.ToManyRelationshipsDoc;

@FunctionalInterface
public interface ToManyRelationshipResolver<REQUEST, DATA_SOURCE_DTO> {

    ToManyRelationshipsDoc resolveRequestedData(REQUEST request,
                                                DATA_SOURCE_DTO dataSourceDto);

}
