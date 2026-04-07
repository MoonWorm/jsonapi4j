package pro.api4.jsonapi4j.processor.multi.relationship;

import pro.api4.jsonapi4j.processor.IdAndType;
import pro.api4.jsonapi4j.model.document.LinksObject;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.response.PaginationContext;

import java.util.List;

public class ToManyRelationshipsJsonApiMembersResolver<REQUEST, DATA_SOURCE_DTO> {

    private final ToManyRelationshipsJsonApiContext<REQUEST, DATA_SOURCE_DTO> jsonApiContext;

    public ToManyRelationshipsJsonApiMembersResolver(
            ToManyRelationshipsJsonApiContext<REQUEST, DATA_SOURCE_DTO> jsonApiContext
    ) {
        Validate.notNull(jsonApiContext.getResourceTypeAndIdResolver(), "resourceTypeAndIdResolver cannot be null");
        this.jsonApiContext = jsonApiContext;
    }

    public IdAndType resolveResourceTypeAndId(DATA_SOURCE_DTO dataSourceDto) {
        IdAndType idAndType =  jsonApiContext.getResourceTypeAndIdResolver().resolveTypeAndId(dataSourceDto);
        // validate none of these is null
        Validate.notNull(idAndType, "idAndType cannot be null");
        Validate.notNull(idAndType.getId(), "id cannot be null");
        Validate.notNull(idAndType.getType(), "type cannot be null");
        return idAndType;
    }

    public LinksObject resolveDocLinks(REQUEST request,
                                       List<DATA_SOURCE_DTO> dataSourceDtos,
                                       PaginationContext paginationContext) {
        return jsonApiContext.getTopLevelLinksResolver() != null
                ? jsonApiContext.getTopLevelLinksResolver().resolve(request, dataSourceDtos, paginationContext)
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
