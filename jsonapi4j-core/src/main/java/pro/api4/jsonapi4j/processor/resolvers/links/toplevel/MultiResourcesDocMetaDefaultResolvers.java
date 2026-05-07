package pro.api4.jsonapi4j.processor.resolvers.links.toplevel;

import pro.api4.jsonapi4j.processor.resolvers.MultipleDataItemsDocMetaResolver;
import pro.api4.jsonapi4j.request.CursorAwareRequest;
import pro.api4.jsonapi4j.request.LimitOffsetAwareRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class MultiResourcesDocMetaDefaultResolvers {

    public static final String PAGINATION_NEXT_CURSOR_KEY_META = "pagination.nextCursor";
    public static final String PAGINATION_TOTAL_ITEMS_KEY_META = "pagination.totalItems";

    private MultiResourcesDocMetaDefaultResolvers() {

    }

    public static <REQUEST, DATA_SOURCE_DTO> MultipleDataItemsDocMetaResolver<REQUEST, DATA_SOURCE_DTO> defaultTopLevelMetaResolver() {
        return (request, dataSourceDtos, paginationContext) -> {
            if (paginationContext != null) {
                Map<String, Object> meta = new HashMap<>();
                if (request instanceof CursorAwareRequest && paginationContext.getNextCursor() != null) {
                    meta.put(PAGINATION_NEXT_CURSOR_KEY_META, paginationContext.getNextCursor());
                }
                if (request instanceof LimitOffsetAwareRequest && paginationContext.getTotalItems() != null) {
                    meta.put(PAGINATION_TOTAL_ITEMS_KEY_META, paginationContext.getTotalItems());
                }
                return Collections.unmodifiableMap(meta);
            }
            return null;
        };
    }

}
