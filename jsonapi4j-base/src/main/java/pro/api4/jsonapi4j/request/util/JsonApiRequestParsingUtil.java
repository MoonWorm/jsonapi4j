package pro.api4.jsonapi4j.request.util;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.exception.ConstraintViolationException;
import pro.api4.jsonapi4j.operation.validation.JsonApi4jDefaultValidator;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;
import pro.api4.jsonapi4j.request.SortAwareRequest;
import pro.api4.jsonapi4j.util.CustomCollectors;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Stream;

import static pro.api4.jsonapi4j.request.FiltersAwareRequest.extractFilterName;
import static pro.api4.jsonapi4j.request.FiltersAwareRequest.isJsonApiFilterParam;
import static pro.api4.jsonapi4j.request.IncludeAwareRequest.NUMBER_OF_INCLUDES_GLOBAL_CAP;
import static pro.api4.jsonapi4j.request.PaginationAwareRequest.isJsonApiPaginationParam;
import static pro.api4.jsonapi4j.request.SortAwareRequest.NUMBER_OF_SORT_BY_GLOBAL_CAP;
import static pro.api4.jsonapi4j.request.SparseFieldsetsAwareRequest.extractResourceType;
import static pro.api4.jsonapi4j.request.SparseFieldsetsAwareRequest.isJsonApiFieldsParam;

public final class JsonApiRequestParsingUtil {

    private static final Logger log = LoggerFactory.getLogger(JsonApiRequestParsingUtil.class);

    private JsonApiRequestParsingUtil() {

    }

    public static Map<String, List<String>> parseCustomQueryParams(Map<String, List<String>> params) {
        return params.entrySet()
                .stream()
                .filter(e -> isNotJsonApiParam(e.getKey()))
                .filter(e -> !e.getValue().isEmpty())
                .filter(e -> e.getValue().stream().anyMatch(StringUtils::isNotBlank))
                .collect(CustomCollectors.toOrderedMap(
                        Map.Entry::getKey,
                        e -> parseCommaSeparatedParam(e.getValue()).sorted().toList())
                );
    }

    public static String parseResourceIdFromThePath(String requestUri) {
        if (requestUri == null) {
            return null;
        }
        List<String> pathSegments = getPathSegments(requestUri);
        if (pathSegments.size() >= 2) {
            return pathSegments.get(1);
        } else {
            return null;
        }
    }

    public static List<String> parseEffectiveIncludes(List<String> paramValue) {
        List<String> result = parseCommaSeparatedParam(paramValue)
                .map(include -> include.split("\\.")[0])
                .sorted()
                .toList();
        if (result.size() > NUMBER_OF_INCLUDES_GLOBAL_CAP) {
            throw new ConstraintViolationException(
                    DefaultErrorCodes.VALUE_TOO_HIGH,
                    String.format("Filter value shouldn't have more than %d elements", NUMBER_OF_INCLUDES_GLOBAL_CAP),
                    IncludeAwareRequest.INCLUDE_PARAM
            );
        }
        return result;
    }

    public static List<String> parseOriginalIncludes(List<String> paramValue) {
        List<String> result = parseCommaSeparatedParam(paramValue)
                .sorted()
                .toList();
        if (result.size() > NUMBER_OF_INCLUDES_GLOBAL_CAP) {
            throw new ConstraintViolationException(
                    DefaultErrorCodes.VALUE_TOO_HIGH,
                    String.format("Filter value shouldn't have more than %d elements", NUMBER_OF_INCLUDES_GLOBAL_CAP),
                    IncludeAwareRequest.INCLUDE_PARAM
            );
        }
        return result;
    }

    public static Map<String, SortAwareRequest.SortOrder> parseSortBy(List<String> paramValue) {
        Map<String, SortAwareRequest.SortOrder> sortBy = parseCommaSeparatedParam(paramValue)
                .sorted()
                .collect(CustomCollectors.toOrderedMap(
                        SortAwareRequest::extractSortBy,
                        SortAwareRequest::extractSortOrder
                ));
        if (sortBy.size() > SortAwareRequest.NUMBER_OF_SORT_BY_GLOBAL_CAP) {
            throw new ConstraintViolationException(
                    DefaultErrorCodes.VALUE_TOO_HIGH,
                    String.format("Sort value shouldn't have more than %d elements", NUMBER_OF_SORT_BY_GLOBAL_CAP),
                    SortAwareRequest.SORT_PARAM
            );
        }
        return sortBy;
    }

    public static String parseCursor(List<String> cursorParamValue) {
        if (CollectionUtils.isEmpty(cursorParamValue)) {
            return null;
        }
        return cursorParamValue.stream().findFirst().orElse(null);
    }

    public static Long parseLimit(List<String> limitParamValue) {
        if (CollectionUtils.isEmpty(limitParamValue)) {
            return null;
        }
        return limitParamValue.stream().findFirst().map(v -> {
            new JsonApi4jDefaultValidator().validateLimit(v);
            return Long.parseLong(v);
        }).orElse(null);
    }

    public static Long parseOffset(List<String> offsetParamValue) {
        if (CollectionUtils.isEmpty(offsetParamValue)) {
            return null;
        }
        return offsetParamValue.stream().findFirst().map(Long::parseLong).orElse(null);
    }

    public static URI parseExt(String contentType) {
        return parseMediaTypeURIParam(contentType, "ext");
    }

    public static URI parseProfile(String contentType) {
        return parseMediaTypeURIParam(contentType, "profile");
    }

    private static URI parseMediaTypeURIParam(String mediaType, String paramName) {
        String extStr = JsonApiMediaType.getParam(mediaType, paramName);
        extStr = unwrapDoubleQuotes(extStr);
        try {
            return URI.create(extStr);
        } catch (Exception e) {
            return null;
        }
    }

    private static String unwrapDoubleQuotes(String input) {
        if (input == null || input.length() < 2) {
            return input;
        }
        int start = 0;
        int end = input.length();
        if (input.startsWith("\"")) {
            start = 1;
        }
        if (input.endsWith("\"")) {
            end -= 1;
        }
        return input.substring(start, end);
    }

    public static Map<String, List<String>> parseFilter(Map<String, List<String>> params) {
        return params.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .filter(e -> isJsonApiFilterParam(e.getKey()))
                .collect(CustomCollectors.toOrderedMap(
                        e -> extractFilterName(e.getKey()),
                        e -> parseCommaSeparatedParam(e.getValue()).sorted().toList()
                ));
    }

    public static Map<String, List<String>> parseFieldSets(Map<String, List<String>> params) {
        return params.entrySet()
                .stream()
                .filter(e -> isJsonApiFieldsParam(e.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .collect(CustomCollectors.toOrderedMap(
                        e -> extractResourceType(e.getKey()),
                        e -> parseCommaSeparatedParam(e.getValue()).sorted().toList()
                ));
    }

    private static Stream<String> parseCommaSeparatedParam(List<String> paramValue) {
        if (paramValue != null) {
            return paramValue.stream()
                    .filter(Objects::nonNull)
                    .filter(v -> !v.isBlank())
                    .map(String::trim)
                    .flatMap(v -> Arrays.stream(v.split(",")))
                    .filter(v -> !v.isBlank())
                    .map(String::trim)
                    .distinct();
        }
        return Stream.empty();

    }

    private static boolean isNotJsonApiParam(String paramName) {
        return !isJsonApiPaginationParam(paramName) &&
                !paramName.equals(IncludeAwareRequest.INCLUDE_PARAM) &&
                !isJsonApiFieldsParam(paramName) &&
                !isJsonApiFilterParam(paramName) &&
                !paramName.equals(SortAwareRequest.SORT_PARAM);
    }


    private static List<String> getPathSegments(String requestUri) {
        try {
            URI uri = new URI(requestUri);
            String path = uri.getPath();
            if (path != null) {
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                return Arrays.asList(path.split("/"));
            }
        } catch (URISyntaxException e) {
            log.warn("Invalid URI. {}", e.getMessage());
        }
        return Collections.emptyList();
    }


}
