package pro.api4.jsonapi4j.processor.resolvers.links.toplevel;

import pro.api4.jsonapi4j.processor.resolvers.MultipleDataItemsDocMetaResolver;
import pro.api4.jsonapi4j.request.CursorAwareRequest;
import pro.api4.jsonapi4j.request.LimitOffsetAwareRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class MultiResourcesDocMetaDefaultResolvers {

    private MultiResourcesDocMetaDefaultResolvers() {

    }

    public static <REQUEST, DATA_SOURCE_DTO> MultipleDataItemsDocMetaResolver<REQUEST, DATA_SOURCE_DTO> defaultTopLevelMetaResolver() {
        return (request, dataSourceDtos, paginationContext) -> {
            if (paginationContext != null) {
                Map<String, Object> meta = new HashMap<>();
                if (request instanceof CursorAwareRequest && paginationContext.getNextCursor() != null) {
                    meta.put("pagination.nextCursor", paginationContext.getNextCursor());
                }
                if (request instanceof LimitOffsetAwareRequest && paginationContext.getTotalItems() != null) {
                    meta.put("pagination.totalItems", paginationContext.getTotalItems());
                }
                return Collections.unmodifiableMap(meta);
            }
            return null;
        };
    }

}
