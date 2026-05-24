package pro.api4.jsonapi4j.operation.validation;

import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;

public class MapValidationAssert<K, V>
        extends ObjectValidationAssert<MapValidationAssert<K, V>, Map<K, V>> {

    MapValidationAssert(Map<K, V> actual, ErrorSources.Source source) {
        super(actual, source);
    }

    public MapValidationAssert<K, V> isEmpty() {
        if (actual != null && !actual.isEmpty()) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR, "map must be empty");
        }
        return this;
    }

    public MapValidationAssert<K, V> isNotEmpty() {
        if (actual == null || actual.isEmpty()) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR, "map can't be empty");
        }
        return this;
    }

    public MapValidationAssert<K, V> hasSize(int expected) {
        if (actual == null || actual.size() != expected) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                    MessageFormat.format("expected size {0}, got {1}", expected, actual == null ? "null" : actual.size()));
        }
        return this;
    }

    public MapValidationAssert<K, V> hasSizeLessThan(int max) {
        if (actual != null && actual.size() >= max) {
            fail(DefaultErrorCodes.ARRAY_LENGTH_TOO_LONG,
                    MessageFormat.format("size must be less than {0}", max));
        }
        return this;
    }

    public MapValidationAssert<K, V> hasSizeGreaterThan(int min) {
        if (actual == null || actual.size() <= min) {
            fail(DefaultErrorCodes.ARRAY_LENGTH_TOO_SHORT,
                    MessageFormat.format("size must be greater than {0}", min));
        }
        return this;
    }

    public MapValidationAssert<K, V> containsKey(K key) {
        if (actual == null || !actual.containsKey(key)) {
            fail(DefaultErrorCodes.MISSING_REQUIRED_PARAMETER,
                    MessageFormat.format("required key ''{0}'' is missing", key));
        }
        return this;
    }

    public MapValidationAssert<K, V> doesNotContainKey(K key) {
        if (actual != null && actual.containsKey(key)) {
            fail(DefaultErrorCodes.UNEXPECTED_PARAMETER,
                    MessageFormat.format("unexpected key ''{0}''", key));
        }
        return this;
    }

    @SafeVarargs
    public final MapValidationAssert<K, V> containsKeys(K... keys) {
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

    public MapValidationAssert<K, V> containsValue(V value) {
        if (actual == null || !actual.containsValue(value)) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                    MessageFormat.format("map must contain value ''{0}''", value));
        }
        return this;
    }

    public MapValidationAssert<K, V> containsEntry(K key, V value) {
        if (actual == null || !actual.containsKey(key) || !value.equals(actual.get(key))) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                    MessageFormat.format("map must contain entry ''{0}''=''{1}''", key, value));
        }
        return this;
    }

    public MapValidationAssert<K, V> doesNotContainEntry(K key, V value) {
        if (actual != null && actual.containsKey(key) && value.equals(actual.get(key))) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                    MessageFormat.format("map must not contain entry ''{0}''=''{1}''", key, value));
        }
        return this;
    }

}
