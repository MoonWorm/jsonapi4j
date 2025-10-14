package pro.api4.jsonapi4j.processor.resolvers;

import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.model.document.data.BaseDoc;

@FunctionalInterface
public interface DefaultRelationshipResolver<REQUEST, DATA_SOURCE_DTO> {


    BaseDoc resolveDefaultRelationship(RelationshipName relationshipName,
                                       REQUEST request,
                                       DATA_SOURCE_DTO dataSourceDto);

}
