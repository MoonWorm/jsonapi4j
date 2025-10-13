package io.jsonapi4j.processor.multi.relationship;

import io.jsonapi4j.processor.IdAndType;
import io.jsonapi4j.model.document.LinksObject;
import org.apache.commons.lang3.Validate;

import java.util.List;

public class ToManyRelationshipsJsonApiMembersResolver<REQUEST, DATA_SOURCE_DTO> {

    private final ToManyRelationshipsJsonApiContext<REQUEST, DATA_SOURCE_DTO> jsonApiContext;

    public ToManyRelationshipsJsonApiMembersResolver(
            ToManyRelationshipsJsonApiContext<REQUEST, DATA_SOURCE_DTO> jsonApiContext
    ) {
        Validate.notNull(jsonApiContext.getResourceTypeAndIdResolver());
        this.jsonApiContext = jsonApiContext;
    }

    public IdAndType resolveResourceTypeAndId(DATA_SOURCE_DTO dataSourceDto) {
        return jsonApiContext.getResourceTypeAndIdResolver().resolveTypeAndId(dataSourceDto);
    }

    public LinksObject resolveDocLinks(REQUEST request,
                                       List<DATA_SOURCE_DTO> dataSourceDtos,
                                       String nextCursor) {
        return jsonApiContext.getTopLevelLinksResolver() != null
                ? jsonApiContext.getTopLevelLinksResolver().resolve(request, dataSourceDtos, nextCursor)
                : null;
    }

    public Object resolveDocMeta(REQUEST request, List<DATA_SOURCE_DTO> dataSourceDtos) {
        return jsonApiContext.getTopLevelMetaResolver() != null
                ? jsonApiContext.getTopLevelMetaResolver().resolve(request, dataSourceDtos)
                : null;
    }

    public Object resolveResourceMeta(REQUEST request, DATA_SOURCE_DTO dataSourceDto) {
        return jsonApiContext.getResourceMetaResolver() != null
                ? jsonApiContext.getResourceMetaResolver().resolve(request, dataSourceDto)
                : null;
    }

}
