package pro.api4.jsonapi4j.operation.validation;

import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;

import java.text.MessageFormat;
import java.util.Map;

/**
 * Fluent assertion class for validating {@link Map} values in JSON:API requests.
 *
 * <p>Extends {@link ObjectValidationAssert} with map-specific constraints: empty/non-empty checks,
 * size bounds, key/value presence, and entry-level assertions.
 *
 * <p>Usage example:
 * {@snippet :
 *   Validate.assertThat(request.getFilters())
 *           .isNotEmpty()
 *           .hasSizeLessThanOrEqualTo(5)
 *           .containsKey("status");
 * }
 *
 * @param <K> the key type
 * @param <V> the value type
 * @see Validate
 * @see ObjectValidationAssert
 */
public class MapValidationAssert<K, V>
        extends ObjectValidationAssert<MapValidationAssert<K, V>, Map<K, V>> {

    MapValidationAssert(Map<K, V> actual, ErrorSources.Source source) {
        super(actual, source);
    }

    /** Asserts the map is empty. */
    public MapValidationAssert<K, V> isEmpty() {
        if (isSkipped()) return this;
        if (actual != null && !actual.isEmpty()) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR, "map must be empty");
        }
        return this;
    }

    /** Asserts the map is not empty. */
    public MapValidationAssert<K, V> isNotEmpty() {
        if (isSkipped()) return this;
        if (actual == null || actual.isEmpty()) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR, "map can't be empty");
        }
        return this;
    }

    /** Asserts the map has exactly the given size. */
    public MapValidationAssert<K, V> hasSize(int expected) {
        if (isSkipped()) return this;
        if (actual == null || actual.size() != expected) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                    MessageFormat.format("expected size {0}, got {1}", expected, actual == null ? "null" : actual.size()));
        }
        return this;
    }

    /** Asserts the map size is at most {@code max}. */
    public MapValidationAssert<K, V> hasSizeLessThanOrEqualTo(int max) {
        if (isSkipped()) return this;
        if (actual != null && actual.size() > max) {
            fail(DefaultErrorCodes.ARRAY_LENGTH_TOO_LONG,
                    MessageFormat.format("size can''t be more than {0}", max));
        }
        return this;
    }

    /** Asserts the map size is strictly less than {@code max}. */
    public MapValidationAssert<K, V> hasSizeLessThan(int max) {
        if (isSkipped()) return this;
        if (actual != null && actual.size() >= max) {
            fail(DefaultErrorCodes.ARRAY_LENGTH_TOO_LONG,
                    MessageFormat.format("size must be less than {0}", max));
        }
        return this;
    }

    /** Asserts the map size is strictly greater than {@code min}. */
    public MapValidationAssert<K, V> hasSizeGreaterThan(int min) {
        if (isSkipped()) return this;
        if (actual == null || actual.size() <= min) {
            fail(DefaultErrorCodes.ARRAY_LENGTH_TOO_SHORT,
                    MessageFormat.format("size must be greater than {0}", min));
        }
        return this;
    }

    /** Asserts the map contains the given key. */
    public MapValidationAssert<K, V> containsKey(K key) {
        if (isSkipped()) return this;
        if (actual == null || !actual.containsKey(key)) {
            fail(DefaultErrorCodes.MISSING_REQUIRED_PARAMETER,
                    MessageFormat.format("required key ''{0}'' is missing", key));
        }
        return this;
    }

    /** Asserts the map does not contain the given key. */
    public MapValidationAssert<K, V> doesNotContainKey(K key) {
        if (isSkipped()) return this;
        if (actual != null && actual.containsKey(key)) {
            fail(DefaultErrorCodes.UNEXPECTED_PARAMETER,
                    MessageFormat.format("unexpected key ''{0}''", key));
        }
        return this;
    }

    /** Asserts the map contains all given keys. */
    @SafeVarargs
    public final MapValidationAssert<K, V> containsKeys(K... keys) {
        if (isSkipped()) return this;
        if (actual == null) {
            fail(DefaultErrorCodes.MISSING_REQUIRED_PARAMETER, "map is null, expected keys");
            return this;
        }
        for (K key : keys) {
            if (!actual.containsKey(key)) {
                fail(DefaultErrorCodes.MISSING_REQUIRED_PARAMETER,
                        MessageFormat.format("required key ''{0}'' is missing", key));
                return this;
            }
        }
        return this;
    }

    /** Asserts the map contains the given value. */
    public MapValidationAssert<K, V> containsValue(V value) {
        if (isSkipped()) return this;
        if (actual == null || !actual.containsValue(value)) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                    MessageFormat.format("map must contain value ''{0}''", value));
        }
        return this;
    }

    /** Asserts the map contains the given key-value pair. */
    public MapValidationAssert<K, V> containsEntry(K key, V value) {
        if (isSkipped()) return this;
        if (actual == null || !actual.containsKey(key) || !value.equals(actual.get(key))) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                    MessageFormat.format("map must contain entry ''{0}''=''{1}''", key, value));
        }
        return this;
    }

    /** Asserts the map does not contain the given key-value pair. */
    public MapValidationAssert<K, V> doesNotContainEntry(K key, V value) {
        if (isSkipped()) return this;
        if (actual != null && actual.containsKey(key) && value.equals(actual.get(key))) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                    MessageFormat.format("map must not contain entry ''{0}''=''{1}''", key, value));
        }
        return this;
    }

}
