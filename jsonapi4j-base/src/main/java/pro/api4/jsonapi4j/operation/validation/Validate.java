package pro.api4.jsonapi4j.operation.validation;

import java.util.Collection;
import java.util.Map;

/**
 * Entry point for the fluent validation API.
 *
 * <p>Provides factory methods that create type-specific assertion objects for validating
 * JSON:API request values. Each {@code assertThat} overload returns an assertion wrapper
 * whose chainable methods throw {@link pro.api4.jsonapi4j.exception.JsonApiRequestValidationException}
 * on constraint violations.
 *
 * <p>Usage example:
 * {@snippet :
 *   Validate.assertThat(request.getResourceId())
 *           .isNotBlank()
 *           .hasLengthLessThanOrEqualTo(64);
 * }
 *
 * @see ObjectValidationAssert
 * @see StringValidationAssert
 * @see NumberValidationAssert
 * @see CollectionValidationAssert
 * @see MapValidationAssert
 */
public final class Validate {

    private Validate() {

    }

    /**
     * Creates a string assertion for the given value.
     *
     * @param value the string value to validate
     * @return a new {@link StringValidationAssert} for chaining string-specific assertions
     */
    public static StringValidationAssert assertThat(String value) {
        return new StringValidationAssert(value, null);
    }

    /**
     * Creates a number assertion for the given integer value.
     *
     * @param value the integer value to validate
     * @return a new {@link NumberValidationAssert} for chaining number-specific assertions
     */
    public static NumberValidationAssert<Integer> assertThat(Integer value) {
        return new NumberValidationAssert<>(value, null);
    }

    /**
     * Creates a number assertion for the given long value.
     *
     * @param value the long value to validate
     * @return a new {@link NumberValidationAssert} for chaining number-specific assertions
     */
    public static NumberValidationAssert<Long> assertThat(Long value) {
        return new NumberValidationAssert<>(value, null);
    }

    /**
     * Creates a collection assertion for the given collection.
     *
     * @param value the collection to validate
     * @param <E>   the element type of the collection
     * @return a new {@link CollectionValidationAssert} for chaining collection-specific assertions
     */
    public static <E> CollectionValidationAssert<E> assertThat(Collection<E> value) {
        return new CollectionValidationAssert<>(value, null);
    }

    /**
     * Creates a map assertion for the given map.
     *
     * @param value the map to validate
     * @param <K>   the key type of the map
     * @param <V>   the value type of the map
     * @return a new {@link MapValidationAssert} for chaining map-specific assertions
     */
    public static <K, V> MapValidationAssert<K, V> assertThat(Map<K, V> value) {
        return new MapValidationAssert<>(value, null);
    }

    /**
     * Creates a generic object assertion for the given value.
     *
     * @param value the value to validate
     * @param <T>   the type of the value
     * @return a new {@link ObjectValidationAssert} for chaining common assertions
     */
    public static <T> ObjectValidationAssert<?, T> assertThat(T value) {
        return new ObjectValidationAssert<>(value, null);
    }

}
