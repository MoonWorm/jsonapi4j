package pro.api4.jsonapi4j.request;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public interface SparseFieldsetsAwareRequest {

    String FIELDS_PARAM = "fields";
    String FIELDS_PARAM_PREFIX = FIELDS_PARAM + "[";
    String FIELDS_PARAM_SUFFIX = "]";

    Map<String, Set<String>> getSparseFieldsets();

    default boolean hasSparseFieldsets() {
        return getSparseFieldsets() != null && !getSparseFieldsets().isEmpty();
    }

    default Set<String> getSparseFieldset(String resourceType) {
        if (getSparseFieldsets() == null) {
            return Collections.emptySet();
        }
        return getSparseFieldsets().getOrDefault(resourceType, Collections.emptySet());
    }

    default Map<String, String> asSingleValueMapOfSparseFieldsets() {
        if (getSparseFieldsets() == null) {
            return Collections.emptyMap();
        }
        return getSparseFieldsets()
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(
                        Collectors.toMap(
                                e -> toFieldsetParamName(e.getKey()),
                                e -> e.getValue().stream().sorted().collect(Collectors.joining(",")),
                                (a, b) -> a,
                                LinkedHashMap::new
                        )
                );
    }

    static boolean isSparseFieldsetParam(String paramName) {
        return paramName != null
                && paramName.startsWith(FIELDS_PARAM_PREFIX)
                && paramName.endsWith(FIELDS_PARAM_SUFFIX)
                && paramName.length() > FIELDS_PARAM_PREFIX.length() + FIELDS_PARAM_SUFFIX.length();
    }

    static String extractResourceType(String paramName) {
        if (!isSparseFieldsetParam(paramName)) {
            return null;
        }
        return paramName.substring(
                FIELDS_PARAM_PREFIX.length(),
                paramName.length() - FIELDS_PARAM_SUFFIX.length()
        );
    }

    static String toFieldsetParamName(String resourceType) {
        return FIELDS_PARAM_PREFIX + resourceType + FIELDS_PARAM_SUFFIX;
    }
}
