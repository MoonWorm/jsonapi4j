package pro.api4.jsonapi4j.request.util;

import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.request.CursorAwareRequest;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.SortAwareRequest;
import pro.api4.jsonapi4j.request.exception.BadJsonApiRequestException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pro.api4.jsonapi4j.request.FiltersAwareRequest.extractFilterName;
import static pro.api4.jsonapi4j.request.FiltersAwareRequest.isJsonApiFilterParam;
import static pro.api4.jsonapi4j.request.IncludeAwareRequest.NUMBER_OF_INCLUDES_GLOBAL_CAP;
import static pro.api4.jsonapi4j.request.SortAwareRequest.NUMBER_OF_SORT_BY_GLOBAL_CAP;
import static java.util.stream.Collectors.toMap;

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
                .collect(toMap(Map.Entry::getKey, e -> parseCommaSeparatedParam(e.getValue()).toList()));
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

    public static Set<String> parseEffectiveIncludes(List<String> paramValue) {
        Set<String> result = parseCommaSeparatedParam(paramValue)
                .map(include -> include.split("\\.")[0])
                .collect(Collectors.toSet());
        if (result.size() > NUMBER_OF_INCLUDES_GLOBAL_CAP) {
            throw new BadJsonApiRequestException(
                    DefaultErrorCodes.VALUE_TOO_HIGH,
                    IncludeAwareRequest.INCLUDE_PARAM,
                    String.format("Filter value shouldn't have more than %d elements", NUMBER_OF_INCLUDES_GLOBAL_CAP)
            );
        }
        return result;
    }

    public static Set<String> parseOriginalIncludes(List<String> paramValue) {
        Set<String> result = parseCommaSeparatedParam(paramValue).collect(Collectors.toSet());
        if (result.size() > NUMBER_OF_INCLUDES_GLOBAL_CAP) {
            throw new BadJsonApiRequestException(
                    DefaultErrorCodes.VALUE_TOO_HIGH,
                    IncludeAwareRequest.INCLUDE_PARAM,
                    String.format("Filter value shouldn't have more than %d elements", NUMBER_OF_INCLUDES_GLOBAL_CAP)
            );
        }
        return result;
    }

    public static Map<String, SortAwareRequest.SortOrder> parseSortBy(List<String> paramValue) {
        Map<String, SortAwareRequest.SortOrder> sortBy = parseCommaSeparatedParam(paramValue)
                .distinct()
                .collect(Collectors.toMap(
                        SortAwareRequest::extractSortBy,
                        SortAwareRequest::extractSortOrder,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
        if (sortBy.size() > SortAwareRequest.NUMBER_OF_SORT_BY_GLOBAL_CAP) {
            throw new BadJsonApiRequestException(
                    DefaultErrorCodes.VALUE_TOO_HIGH,
                    SortAwareRequest.SORT_PARAM,
                    String.format("Sort value shouldn't have more than %d elements", NUMBER_OF_SORT_BY_GLOBAL_CAP)
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

    public static Map<String, List<String>> parseFilter(Map<String, List<String>> params) {
        return params.entrySet()
                .stream()
                .filter(e -> isJsonApiFilterParam(e.getKey()))
                .collect(toMap(
                        e -> extractFilterName(e.getKey()),
                        e -> parseCommaSeparatedParam(e.getValue()).toList()
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
        return !paramName.equals(CursorAwareRequest.CURSOR_PARAM) &&
                !paramName.equals(IncludeAwareRequest.INCLUDE_PARAM) &&
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
