package pro.api4.jsonapi4j.processor.single.relationship;

import pro.api4.jsonapi4j.processor.IdAndType;
import pro.api4.jsonapi4j.model.document.LinksObject;
import org.apache.commons.lang3.Validate;

public class ToOneRelationshipJsonApiMembersResolver<REQUEST, DATA_SOURCE_DTO> {

    private final ToOneRelationshipJsonApiContext<REQUEST, DATA_SOURCE_DTO> jsonApiContext;

    public ToOneRelationshipJsonApiMembersResolver(
            ToOneRelationshipJsonApiContext<REQUEST, DATA_SOURCE_DTO> jsonApiContext
    ) {
        Validate.notNull(jsonApiContext.getResourceTypeAndIdResolver());
        this.jsonApiContext = jsonApiContext;
    }

    public IdAndType resolveResourceTypeAndId(DATA_SOURCE_DTO dataSourceDto) {
        IdAndType idAndType = jsonApiContext.getResourceTypeAndIdResolver().resolveTypeAndId(dataSourceDto);
        // validate none of these is null
        Validate.notNull(idAndType);
        Validate.notNull(idAndType.getId());
        Validate.notNull(idAndType.getType());
        return idAndType;
    }

    public LinksObject resolveDocLinks(REQUEST request, DATA_SOURCE_DTO dataSourceDto) {
        return jsonApiContext.getTopLevelLinksResolver() != null
                ? jsonApiContext.getTopLevelLinksResolver().resolve(request, dataSourceDto)
                : null;
    }

    public Object resolveDocMeta(REQUEST request, DATA_SOURCE_DTO dataSourceDto) {
        return jsonApiContext.getTopLevelMetaResolver() != null
                ? jsonApiContext.getTopLevelMetaResolver().resolve(request, dataSourceDto)
                : null;
    }

    public Object resolveResourceMeta(REQUEST request, DATA_SOURCE_DTO dataSourceDto) {
        return jsonApiContext.getResourceMetaResolver() != null
                ? jsonApiContext.getResourceMetaResolver().resolve(request, dataSourceDto)
                : null;
    }

}
