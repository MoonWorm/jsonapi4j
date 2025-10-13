package io.jsonapi4j.processor.resolvers;

import io.jsonapi4j.model.document.data.ToOneRelationshipDoc;

@FunctionalInterface
public interface ToOneRelationshipResolver<REQUEST, DATA_SOURCE_DTO> {

    ToOneRelationshipDoc resolveRequestedData(REQUEST request,
                                              DATA_SOURCE_DTO dataSourceDto);

}
