package pro.api4.jsonapi4j.plugin.ac;

import java.util.Collections;
import java.util.Set;

public record AnonymizationResult<T>(
        T targetObject,
        boolean isFullyAnonymized,
        Set<String> anonymizedFields
) {

    public AnonymizationResult(T targetObject) {
        this(targetObject, false, Collections.emptySet());
    }

    public boolean isNotFullyAnonymized() {
        return !isFullyAnonymized;
    }

}
