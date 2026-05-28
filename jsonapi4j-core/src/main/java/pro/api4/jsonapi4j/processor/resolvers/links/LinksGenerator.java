package pro.api4.jsonapi4j.processor.resolvers.links;

import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.request.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.response.PaginationContext;
import pro.api4.jsonapi4j.response.PaginationMode;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static pro.api4.jsonapi4j.request.LimitOffsetAwareRequest.DEFAULT_LIMIT;
import static pro.api4.jsonapi4j.request.LimitOffsetAwareRequest.DEFAULT_OFFSET;

public final class LinksGenerator {

    private final Object request;

    public LinksGenerator(Object request) {
        this.request = request;
    }

    public static String resourcesBasePath(ResourceType resourceType) {
        return "/" + resourceType.getType();
    }

    public static String resourceBasePath(ResourceType resourceType,
                                          Supplier<String> idSupplier) {
        return "/" + resourceType.getType() + "/" + idSupplier.get();
    }

    public static String relationshipBasePath(ResourceType resourceType,
                                              String resourceId,
                                              RelationshipName relationshipName) {
        return "/" + resourceType.getType() + "/" + resourceId + "/relationships/" + relationshipName.getName();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public String generateSelfLink(String basePath,
                                   boolean propagateIncludes,
                                   boolean propagatePagination,
                                   boolean propagateFilters,
                                   boolean propagateSortBy,
                                   boolean propagateFields,
                                   boolean propagateQueryParams) {
        Map<String, String> selfLinkParams = new LinkedHashMap<>();
        if (propagateIncludes) {
            populateIncludes(selfLinkParams);
        }
        if (propagatePagination) {
            populatePagination(selfLinkParams);
        }
        if (propagateFilters) {
            populateFilters(selfLinkParams);
        }
        if (propagateSortBy) {
            populateSortBy(selfLinkParams);
        }
        if (propagateFields) {
            populateFields(selfLinkParams);
        }
        if (propagateQueryParams) {
            populateQueryParams(selfLinkParams);
        }
        return basePath + toParamsStr(selfLinkParams);
    }

    public String generateNextLink(String basePath,
                                   PaginationContext paginationContext,
                                   boolean propagateIncludes,
                                   boolean propagateFilters,
                                   boolean propagateSortBy,
                                   boolean propagateFields,
                                   boolean propagateQueryParams) {
        if (isNextLinkAvailable(paginationContext)) {
            Map<String, String> nextLinkParams = new LinkedHashMap<>();
            if (propagateIncludes) {
                populateIncludes(nextLinkParams);
            }
            populatePagination(nextLinkParams, paginationContext);
            if (propagateFilters) {
                populateFilters(nextLinkParams);
            }
            if (propagateSortBy) {
                populateSortBy(nextLinkParams);
            }
            if (propagateFields) {
                populateFields(nextLinkParams);
            }
            if (propagateQueryParams) {
                populateQueryParams(nextLinkParams);
            }
            return basePath + toParamsStr(nextLinkParams);
        }
        return null;
    }

    public String generateFirstLink(String basePath,
                                    PaginationContext paginationContext,
                                    boolean propagateIncludes,
                                    boolean propagateFilters,
                                    boolean propagateSortBy,
                                    boolean propagateFields,
                                    boolean propagateQueryParams) {
        if (paginationContext == null) {
            return null;
        }
        Map<String, String> firstLinkParams = new LinkedHashMap<>();
        if (propagateIncludes) {
            populateIncludes(firstLinkParams);
        }
        populateFirstPagePagination(firstLinkParams, paginationContext);
        if (propagateFilters) {
            populateFilters(firstLinkParams);
        }
        if (propagateSortBy) {
            populateSortBy(firstLinkParams);
        }
        if (propagateFields) {
            populateFields(firstLinkParams);
        }
        if (propagateQueryParams) {
            populateQueryParams(firstLinkParams);
        }
        return basePath + toParamsStr(firstLinkParams);
    }

    public String generatePrevLink(String basePath,
                                   PaginationContext paginationContext,
                                   boolean propagateIncludes,
                                   boolean propagateFilters,
                                   boolean propagateSortBy,
                                   boolean propagateFields,
                                   boolean propagateQueryParams) {
        if (!isPrevLinkAvailable(paginationContext)) {
            return null;
        }
        Map<String, String> prevLinkParams = new LinkedHashMap<>();
        if (propagateIncludes) {
            populateIncludes(prevLinkParams);
        }
        populatePrevPagePagination(prevLinkParams, paginationContext);
        if (propagateFilters) {
            populateFilters(prevLinkParams);
        }
        if (propagateSortBy) {
            populateSortBy(prevLinkParams);
        }
        if (propagateFields) {
            populateFields(prevLinkParams);
        }
        if (propagateQueryParams) {
            populateQueryParams(prevLinkParams);
        }
        return basePath + toParamsStr(prevLinkParams);
    }

    public String generateLastLink(String basePath,
                                   PaginationContext paginationContext,
                                   boolean propagateIncludes,
                                   boolean propagateFilters,
                                   boolean propagateSortBy,
                                   boolean propagateFields,
                                   boolean propagateQueryParams) {
        if (!isLastLinkAvailable(paginationContext)) {
            return null;
        }
        Map<String, String> lastLinkParams = new LinkedHashMap<>();
        if (propagateIncludes) {
            populateIncludes(lastLinkParams);
        }
        populateLastPagePagination(lastLinkParams, paginationContext);
        if (propagateFilters) {
            populateFilters(lastLinkParams);
        }
        if (propagateSortBy) {
            populateSortBy(lastLinkParams);
        }
        if (propagateFields) {
            populateFields(lastLinkParams);
        }
        if (propagateQueryParams) {
            populateQueryParams(lastLinkParams);
        }
        return basePath + toParamsStr(lastLinkParams);
    }

    private boolean isNextLinkAvailable(PaginationContext paginationContext) {
        return paginationContext != null
                && (isNextLinkAvailableInCursorMode(paginationContext) || isNextLinkAvailableInLimitOffsetMode(paginationContext));
    }

    private boolean isNextLinkAvailableInCursorMode(PaginationContext paginationContext) {
        return paginationContext.getMode() == PaginationMode.CURSOR
                && StringUtils.isNotBlank(paginationContext.getNextCursor());
    }

    private boolean isNextLinkAvailableInLimitOffsetMode(PaginationContext paginationContext) {
        if (request instanceof LimitOffsetAwareRequest loar) {
            return paginationContext.getMode() == PaginationMode.LIMIT_OFFSET
                    && paginationContext.getTotalItems() != null
                    && loar.getOffset() != null
                    && loar.getOffset() + (loar.getLimit() == null ? DEFAULT_LIMIT : loar.getLimit() )< paginationContext.getTotalItems();
        }
        return false;
    }

    private boolean isPrevLinkAvailable(PaginationContext paginationContext) {
        if (paginationContext == null || paginationContext.getMode() != PaginationMode.LIMIT_OFFSET) {
            return false;
        }
        if (request instanceof LimitOffsetAwareRequest loar) {
            Long offset = loar.getOffset();
            return offset != null && offset > 0;
        }
        return false;
    }

    private boolean isLastLinkAvailable(PaginationContext paginationContext) {
        return paginationContext != null
                && paginationContext.getMode() == PaginationMode.LIMIT_OFFSET
                && paginationContext.getTotalItems() != null;
    }

    private void populateFirstPagePagination(Map<String, String> linkParams, PaginationContext paginationContext) {
        if (paginationContext.getMode() == PaginationMode.CURSOR) {
            // First page in cursor mode = no cursor param at all
        } else if (paginationContext.getMode() == PaginationMode.LIMIT_OFFSET) {
            if (request instanceof LimitOffsetAwareRequest loar) {
                long limit = loar.getLimit() == null ? DEFAULT_LIMIT : loar.getLimit();
                linkParams.put(LimitOffsetAwareRequest.LIMIT_PARAM, String.valueOf(limit));
                linkParams.put(LimitOffsetAwareRequest.OFFSET_PARAM, String.valueOf(DEFAULT_OFFSET));
            }
        }
    }

    private void populatePrevPagePagination(Map<String, String> linkParams, PaginationContext paginationContext) {
        if (request instanceof LimitOffsetAwareRequest loar) {
            long limit = loar.getLimit() == null ? DEFAULT_LIMIT : loar.getLimit();
            long offset = loar.getOffset() == null ? DEFAULT_OFFSET : loar.getOffset();
            long prevOffset = Math.max(0, offset - limit);
            linkParams.put(LimitOffsetAwareRequest.LIMIT_PARAM, String.valueOf(limit));
            linkParams.put(LimitOffsetAwareRequest.OFFSET_PARAM, String.valueOf(prevOffset));
        }
    }

    private void populateLastPagePagination(Map<String, String> linkParams, PaginationContext paginationContext) {
        if (request instanceof LimitOffsetAwareRequest loar) {
            long limit = loar.getLimit() == null ? DEFAULT_LIMIT : loar.getLimit();
            long totalItems = paginationContext.getTotalItems();
            long lastOffset = totalItems <= 0 ? 0 : (long) (Math.ceil((double) totalItems / limit) - 1) * limit;
            linkParams.put(LimitOffsetAwareRequest.LIMIT_PARAM, String.valueOf(limit));
            linkParams.put(LimitOffsetAwareRequest.OFFSET_PARAM, String.valueOf(lastOffset));
        }
    }

    public String generateRelatedLink(String basePath,
                                      boolean propagateFields,
                                      boolean propagateQueryParams) {
        Map<String, String> relatedLinkParams = new LinkedHashMap<>();
        if (propagateFields) {
            populateFields(relatedLinkParams);
        }
        if (propagateQueryParams) {
            populateQueryParams(relatedLinkParams);
        }
        return basePath + toParamsStr(relatedLinkParams);
    }

    private boolean isCursorAwarePagination(PaginationContext paginationContext) {
        return paginationContext.getMode() == PaginationMode.CURSOR && StringUtils.isNotBlank(paginationContext.getNextCursor());
    }

    private boolean isLimitOffsetAwarePagination(PaginationContext paginationContext) {
        return paginationContext.getMode() == PaginationMode.LIMIT_OFFSET;
    }

    private void populatePagination(Map<String, String> selfLinkParams) {
        if (request instanceof CursorAwareRequest car) {
            String currentCursor = car.getCursor();
            if (StringUtils.isNotBlank(currentCursor)) {
                selfLinkParams.put(CursorAwareRequest.CURSOR_PARAM, currentCursor);
            }
        }
        if (request instanceof LimitOffsetAwareRequest loar) {
            Long limit = loar.getLimit();
            if (limit != null) {
                selfLinkParams.put(LimitOffsetAwareRequest.LIMIT_PARAM, String.valueOf(limit));
            }
            Long offset = loar.getOffset();
            if (offset != null) {
                selfLinkParams.put(LimitOffsetAwareRequest.OFFSET_PARAM, String.valueOf(offset));
            }
        }
    }

    private void populatePagination(Map<String, String> nextLinkParams, PaginationContext paginationContext) {
        if (isCursorAwarePagination(paginationContext)) {
            nextLinkParams.put(CursorAwareRequest.CURSOR_PARAM, paginationContext.getNextCursor());
        } else if (isLimitOffsetAwarePagination(paginationContext)) {
            Long totalItems = paginationContext.getTotalItems();
            if (request instanceof LimitOffsetAwareRequest r) {
                Long limit = r.getLimit();
                if (limit == null) {
                    limit = DEFAULT_LIMIT;
                }
                Long offset = r.getOffset();
                if (offset == null) {
                    offset = DEFAULT_OFFSET;
                }
                long nextOffset = Math.min(offset + limit, totalItems != null ? totalItems : Long.MAX_VALUE);
                nextLinkParams.put(LimitOffsetAwareRequest.LIMIT_PARAM, String.valueOf(limit));
                nextLinkParams.put(LimitOffsetAwareRequest.OFFSET_PARAM, String.valueOf(nextOffset));
            }
        }
    }

    private void populateIncludes(Map<String, String> linkParams) {
        if (request instanceof IncludeAwareRequest r) {
            List<String> includesSet = r.getEffectiveIncludes();
            if (CollectionUtils.isNotEmpty(includesSet)) {
                String includesStr = String.join(",", includesSet);
                linkParams.put(IncludeAwareRequest.INCLUDE_PARAM, includesStr);
            }
        }
    }

    private void populateQueryParams(Map<String, String> linkParams) {
        if (request instanceof CustomQueryParamsAwareRequest r) {
            if (MapUtils.isNotEmpty(r.getCustomQueryParams())) {
                linkParams.putAll(r.asSingleValueMap());
            }
        }
    }

    private void populateFilters(Map<String, String> linkParams) {
        if (request instanceof FiltersAwareRequest r) {
            if (MapUtils.isNotEmpty(r.getFilters())) {
                r.getFilters().forEach((filterName, value) -> linkParams.put(
                        FiltersAwareRequest.getFilterParam(filterName),
                        String.join(",", value)
                ));

            }
        }
    }

    private void populateSortBy(Map<String, String> linkParams) {
        if (request instanceof SortAwareRequest r) {
            if (MapUtils.isNotEmpty(r.getSortBy())) {
                List<String> sortParamValues = r.getSortBy()
                        .entrySet()
                        .stream()
                        .map(e -> e.getValue() == SortAwareRequest.SortOrder.DESC ? "-" + e.getKey() : e.getKey())
                        .toList();
                linkParams.put(SortAwareRequest.SORT_PARAM, String.join(",", sortParamValues));
            }
        }
    }

    private void populateFields(Map<String, String> linkParams) {
        if (request instanceof SparseFieldsetsAwareRequest r) {
            if (MapUtils.isNotEmpty(r.getFieldSets())) {
                r.getFieldSets().forEach((resourceType, fields) -> linkParams.put(
                        SparseFieldsetsAwareRequest.getFieldsParam(resourceType),
                        String.join(",", fields)
                ));
            }
        }
    }

    private String toParamsStr(Map<String, String> params) {
        StringBuilder paramsStr = new StringBuilder();
        if (MapUtils.isNotEmpty(params)) {
            paramsStr.append("?");
            paramsStr.append(
                    params.entrySet()
                            .stream()
                            .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                            .collect(Collectors.joining("&"))
            );
        }
        return paramsStr.toString();
    }
}
