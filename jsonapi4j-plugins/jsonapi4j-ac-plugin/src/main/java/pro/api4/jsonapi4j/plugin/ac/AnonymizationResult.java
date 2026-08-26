package pro.api4.jsonapi4j.plugin.ac;

import pro.api4.jsonapi4j.model.document.error.ErrorCode;

import java.util.Map;

/**
 * What anonymizing one object produced: the object to send, and what was hidden on the way.
 *
 * @param targetObject      the object to serialize — a redacted copy when anything was hidden, the original
 *                          when nothing was, and {@code null} when the whole object was denied
 * @param isFullyAnonymized whether the object itself was denied rather than parts of it
 * @param anonymizedFields  paths that were hidden, mapped to the code of the requirement that refused them.
 *                          Paths are relative to the object, and index into containers as
 *                          {@code addresses[0].zip}. When the object itself was denied, the map holds a
 *                          single entry under the object's own path — empty for the root
 */
public record AnonymizationResult<T>(
        T targetObject,
        boolean isFullyAnonymized,
        Map<String, ErrorCode> anonymizedFields
) {

    public AnonymizationResult(T targetObject) {
        this(targetObject, false, Map.of());
    }

    public boolean isNotFullyAnonymized() {
        return !isFullyAnonymized;
    }

    public boolean isAnyAnonymized() {
        return isFullyAnonymized || !anonymizedFields.isEmpty();
    }

    public boolean isNothingAnonymized() {
        return !isAnyAnonymized();
    }

}
