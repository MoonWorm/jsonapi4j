package pro.api4.jsonapi4j.config;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Small, null-safe fluent helper for reading values out of a raw, nested configuration map (e.g. a config file
 * loaded as a {@code Map<String, Object>}). Lets callers traverse sections and read typed leaf values without
 * repetitive {@code instanceof Map} / cast boilerplate.
 * <pre>{@code
 * new RawConfigAccessor(raw)
 *     .section("meta")
 *     .flatMap(m -> m.value("enabled", Boolean.class))
 *     .orElse(false);
 * }</pre>
 */
public class RawConfigAccessor {

    private final Map<String, Object> properties;

    public RawConfigAccessor(Map<String, Object> properties) {
        this.properties = properties == null ? Map.of() : properties;
    }

    /**
     * @param key the child key
     * @return a traversal over the nested map at {@code key}, or empty when absent or not a map
     */
    public Optional<RawConfigAccessor> section(String key) {
        if (!(properties.get(key) instanceof Map<?, ?> map)) {
            return Optional.empty();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> typed = (Map<String, Object>) map;
        return Optional.of(new RawConfigAccessor(typed));
    }

    /**
     * @return the current properties map as an unmodifiable view (escape hatch for callers that need the raw
     * {@code Map}, e.g. to hand to Jackson or store verbatim)
     */
    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(this.properties);
    }

    /**
     * @param key  the leaf key
     * @param type the expected value type
     * @param <T>  the expected value type
     * @return the value at {@code key} when present and an instance of {@code type}, otherwise empty
     */
    public <T> Optional<T> value(String key, Class<T> type) {
        Object value = properties.get(key);
        return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
    }

    /**
     * @param key the leaf key
     * @return the {@link String} value at {@code key}, trimmed; empty when absent, not a string, or blank
     */
    public Optional<String> strValue(String key) {
        return value(key, String.class).map(StringUtils::trimToNull);
    }

    /**
     * @param key the leaf key
     * @return the boolean value at {@code key} — a native {@link Boolean}, or a string parsed via
     * {@link BooleanUtils#toBooleanObject(String)} ({@code true/false/yes/no/on/off}); empty otherwise
     */
    public Optional<Boolean> boolValue(String key) {
        Object value = properties.get(key);
        if (value instanceof Boolean bool) {
            return Optional.of(bool);
        }
        if (value instanceof String str) {
            return Optional.ofNullable(BooleanUtils.toBooleanObject(str.trim()));
        }
        return Optional.empty();
    }

    /**
     * @param key the leaf key
     * @return the value at {@code key} as an {@link Integer} (native {@link Number} or parsed string); empty otherwise
     */
    public Optional<Integer> intValue(String key) {
        return numberValue(key, Number::intValue, Integer::parseInt);
    }

    /**
     * @param key the leaf key
     * @return the value at {@code key} as a {@link Long} (native {@link Number} or parsed string); empty otherwise
     */
    public Optional<Long> longValue(String key) {
        return numberValue(key, Number::longValue, Long::parseLong);
    }

    /**
     * @param key the leaf key
     * @return the value at {@code key} as a {@link Double} (native {@link Number} or parsed string); empty otherwise
     */
    public Optional<Double> doubleValue(String key) {
        return numberValue(key, Number::doubleValue, Double::parseDouble);
    }

    private <T> Optional<T> numberValue(String key, Function<Number, T> fromNumber, Function<String, T> fromString) {
        Object value = properties.get(key);
        if (value instanceof Number number) {
            return Optional.of(fromNumber.apply(number));
        }
        if (value instanceof String str && StringUtils.isNotBlank(str)) {
            try {
                return Optional.of(fromString.apply(str.trim()));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

}
