package pro.api4.jsonapi4j.processor.single.relationship;

import pro.api4.jsonapi4j.processor.RelationshipJsonApiContext;
import pro.api4.jsonapi4j.processor.resolvers.SingleDataItemDocLinksResolver;
import pro.api4.jsonapi4j.processor.resolvers.SingleDataItemDocMetaResolver;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ToOneRelationshipJsonApiContext<REQUEST, DATA_SOURCE_DTO>
        extends RelationshipJsonApiContext<REQUEST, DATA_SOURCE_DTO> {

    private SingleDataItemDocLinksResolver<REQUEST, DATA_SOURCE_DTO> topLevelLinksResolver;
    private SingleDataItemDocMetaResolver<REQUEST, DATA_SOURCE_DTO> topLevelMetaResolver;

}
