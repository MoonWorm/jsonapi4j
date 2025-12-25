package pro.api4.jsonapi4j.plugin.ac.impl;

import java.util.Set;

public record AnonymizationResult<T>(
        T targetObject,
        boolean isFullyAnonymized,
        Set<String> anonymizedFields
) {
}
