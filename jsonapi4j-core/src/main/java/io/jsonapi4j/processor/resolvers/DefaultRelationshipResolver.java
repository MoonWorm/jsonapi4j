package io.jsonapi4j.processor.resolvers;

import io.jsonapi4j.domain.RelationshipName;
import io.jsonapi4j.model.document.data.BaseDoc;

@FunctionalInterface
public interface DefaultRelationshipResolver<REQUEST, DATA_SOURCE_DTO> {


    BaseDoc resolveDefaultRelationship(RelationshipName relationshipName,
                                       REQUEST request,
                                       DATA_SOURCE_DTO dataSourceDto);

}
