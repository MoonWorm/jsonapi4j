package pro.api4.jsonapi4j.processor.resolvers.links;

import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.request.CursorAwareRequest;
import pro.api4.jsonapi4j.request.CustomQueryParamsAwareRequest;
import pro.api4.jsonapi4j.request.FiltersAwareRequest;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.SortAwareRequest;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
                                   boolean propagateCursor,
                                   boolean propagateFilters,
                                   boolean propagateSortBy,
                                   boolean propagateQueryParams) {
        Map<String, String> selfLinkParams = new LinkedHashMap<>();
        if (propagateIncludes) {
            populateIncludes(selfLinkParams);
        }
        if (propagateCursor) {
            populateCursor(selfLinkParams);
        }
        if (propagateFilters) {
            populateFilters(selfLinkParams);
        }
        if (propagateSortBy) {
            populateSortBy(selfLinkParams);
        }
        if (propagateQueryParams) {
            populateQueryParams(selfLinkParams);
        }
        return basePath + toParamsStr(selfLinkParams);
    }

    public String generateRelatedLink(String basePath,
                                      boolean propagateIncludes,
                                      boolean propagateCursor,
                                      boolean propagateFilters,
                                      boolean propagateSortBy,
                                      boolean propagateQueryParams) {
        Map<String, String> selfLinkParams = new LinkedHashMap<>();
        if (propagateIncludes) {
            populateIncludes(selfLinkParams);
        }
        if (propagateCursor) {
            populateCursor(selfLinkParams);
        }
        if (propagateFilters) {
            populateFilters(selfLinkParams);
        }
        if (propagateSortBy) {
            populateSortBy(selfLinkParams);
        }
        if (propagateQueryParams) {
            populateQueryParams(selfLinkParams);
        }
        return basePath + toParamsStr(selfLinkParams);
    }

    public String generateNextLink(String basePath,
                                   String nextCursor,
                                   boolean propagateIncludes,
                                   boolean propagateCursor,
                                   boolean propagateFilters,
                                   boolean propagateSortBy,
                                   boolean propagateQueryParams) {
        if (StringUtils.isBlank(nextCursor)) {
            return null;
        }
        Map<String, String> nextLinkParams = new LinkedHashMap<>();
        if (propagateIncludes) {
            populateIncludes(nextLinkParams);
        }
        if (propagateCursor) {
            nextLinkParams.put(CursorAwareRequest.CURSOR_PARAM, nextCursor);
        }
        if (propagateFilters) {
            populateFilters(nextLinkParams);
        }
        if (propagateSortBy) {
            populateSortBy(nextLinkParams);
        }
        if (propagateQueryParams) {
            populateQueryParams(nextLinkParams);
        }
        return basePath + toParamsStr(nextLinkParams);
    }

    private void populateIncludes(Map<String, String> linkParams) {
        if (request instanceof IncludeAwareRequest r) {
            Set<String> includesSet = r.getEffectiveIncludes();
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

    private void populateCursor(Map<String, String> linkParams) {
        if (request instanceof CursorAwareRequest r) {
            String currentCursor = r.getCursor();
            if (StringUtils.isNotBlank(currentCursor)) {
                linkParams.put(CursorAwareRequest.CURSOR_PARAM, currentCursor);
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
