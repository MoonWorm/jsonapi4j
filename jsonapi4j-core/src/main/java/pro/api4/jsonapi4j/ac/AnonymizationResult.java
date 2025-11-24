package pro.api4.jsonapi4j.ac;

import java.util.Set;

public record AnonymizationResult<T>(
        T targetObject,
        boolean isFullyAnonymized,
        Set<String> anonymizedFields
) {
}
