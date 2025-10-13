package io.jsonapi4j.processor.multi.relationship;

import io.jsonapi4j.processor.RelationshipJsonApiContext;
import io.jsonapi4j.processor.resolvers.MultipleDataItemsDocLinksResolver;
import io.jsonapi4j.processor.resolvers.MultipleDataItemsDocMetaResolver;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ToManyRelationshipsJsonApiContext<REQUEST, DATA_SOURCE_DTO>
        extends RelationshipJsonApiContext<REQUEST, DATA_SOURCE_DTO> {

    private MultipleDataItemsDocLinksResolver<REQUEST, DATA_SOURCE_DTO> topLevelLinksResolver;
    private MultipleDataItemsDocMetaResolver<REQUEST, DATA_SOURCE_DTO> topLevelMetaResolver;

}
