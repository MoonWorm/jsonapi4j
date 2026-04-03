package pro.api4.jsonapi4j.processor.resolvers;

import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.model.document.BaseDoc;
import pro.api4.jsonapi4j.model.document.data.RelationshipObject;

@FunctionalInterface
public interface DefaultRelationshipResolver<REQUEST, DATA_SOURCE_DTO> {


    RelationshipObject resolveDefaultRelationship(RelationshipName relationshipName,
                                                  REQUEST request,
                                                  DATA_SOURCE_DTO dataSourceDto);

}
