package io.jsonapi4j.processor.single.relationship;

import io.jsonapi4j.processor.RelationshipJsonApiContext;
import io.jsonapi4j.processor.resolvers.SingleDataItemDocLinksResolver;
import io.jsonapi4j.processor.resolvers.SingleDataItemDocMetaResolver;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ToOneRelationshipJsonApiContext<REQUEST, DATA_SOURCE_DTO>
        extends RelationshipJsonApiContext<REQUEST, DATA_SOURCE_DTO> {

    private SingleDataItemDocLinksResolver<REQUEST, DATA_SOURCE_DTO> topLevelLinksResolver;
    private SingleDataItemDocMetaResolver<REQUEST, DATA_SOURCE_DTO> topLevelMetaResolver;

}
