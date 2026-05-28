package pro.api4.jsonapi4j.operation.validation;

import pro.api4.jsonapi4j.exception.JsonApiRequestValidationException;
import pro.api4.jsonapi4j.exception.ResourceNotFoundException;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.model.document.error.ErrorCode;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Base class for the fluent validation assertion API.
 *
 * <p>Provides common assertion methods (null checks, equality, membership, type checks) that all
 * type-specific assertion classes inherit. Uses a self-type pattern ({@code SELF}) to allow
 * method chaining to return the concrete subclass.
 *
 * <p>Each assertion method throws {@link pro.api4.jsonapi4j.exception.JsonApiRequestValidationException}
 * when the constraint is violated, carrying the associated {@link ErrorSources.Source} for
 * JSON:API-compliant error reporting.
 *
 * <p>Key features:
 * <ul>
 *   <li>{@link #ifPresent()} — skips all subsequent assertions when the value is {@code null}</li>
 *   <li>{@link #withSource(ErrorSources.Source)} — overrides the error source for the next assertion</li>
 *   <li>{@link #withErrorCode(pro.api4.jsonapi4j.model.document.error.ErrorCode)} — overrides the error code for the next assertion</li>
 *   <li>{@link #withDetail(String)} — overrides the error detail message for the next assertion</li>
 *   <li>{@link #field(String, java.util.function.Function)} — navigates into a nested field, producing a child assertion with an
 *       automatically extended JSON Pointer source</li>
 *   <li>{@link #satisfies(java.util.function.Consumer)} — delegates to a custom validation block</li>
 *   <li>Type narrowing via {@link #asString()}, {@link #asNumber()}, {@link #asCollection()}, {@link #asMap()}</li>
 * </ul>
 *
 * @param <SELF>   the concrete assertion subclass (for fluent chaining)
 * @param <ACTUAL> the type of the value being validated
 * @see Validate
 * @see StringValidationAssert
 * @see NumberValidationAssert
 * @see CollectionValidationAssert
 * @see MapValidationAssert
 */
@SuppressWarnings("unchecked")
public class ObjectValidationAssert<SELF extends ObjectValidationAssert<SELF, ACTUAL>, ACTUAL> {

    protected final ACTUAL actual;
    private ErrorSources.Source source;
    private ErrorCode errorCodeOverride;
    private String detailOverride;
    private boolean skipped;

    protected ObjectValidationAssert(ACTUAL actual, ErrorSources.Source source) {
        this.actual = actual;
        this.source = source;
    }

    // --- Override methods ---

    /**
     * Overrides the error source for the next assertion.
     *
     * @param source the error source to use
     * @return this assertion for chaining
     */
    public SELF withSource(ErrorSources.Source source) {
        this.source = source;
        return (SELF) this;
    }

    /**
     * Overrides the error code for the next assertion.
     *
     * @param code the error code to use
     * @return this assertion for chaining
     */
    public SELF withErrorCode(ErrorCode code) {
        this.errorCodeOverride = code;
        return (SELF) this;
    }

    /**
     * Overrides the error detail message for the next assertion.
     *
     * @param detail the detail message to use
     * @return this assertion for chaining
     */
    public SELF withDetail(String detail) {
        this.detailOverride = detail;
        return (SELF) this;
    }

    // --- Conditional ---

    /**
     * Skips all subsequent assertions if the value is {@code null}.
     *
     * @return this assertion for chaining
     */
    public SELF ifPresent() {
        if (actual == null) {
            this.skipped = true;
        }
        return (SELF) this;
    }

    // --- Common assertions ---

    /**
     * Asserts that the value is {@code null}.
     *
     * @return this assertion for chaining
     */
    public SELF isNull() {
        if (skipped) return (SELF) this;
        if (actual != null) {
            fail(DefaultErrorCodes.VALUE_IS_NOT_ABSENT, "value must be null");
        }
        clearOverrides();
        return (SELF) this;
    }

    /**
     * Asserts that the value is not {@code null}.
     *
     * @return this assertion for chaining
     */
    public SELF isNotNull() {
        if (skipped) return (SELF) this;
        if (actual == null) {
            fail(DefaultErrorCodes.VALUE_IS_ABSENT, "value can't be null");
        }
        clearOverrides();
        return (SELF) this;
    }

    /**
     * Asserts that the value equals the expected object.
     *
     * @param expected the expected value
     * @return this assertion for chaining
     */
    public SELF isEqualTo(Object expected) {
        if (skipped) return (SELF) this;
        if (actual == null || !actual.equals(expected)) {
            fail(DefaultErrorCodes.VALUE_IS_NOT_EQUAL_TO,
                    MessageFormat.format("value should match {0}", expected));
        }
        clearOverrides();
        return (SELF) this;
    }

    /**
     * Asserts that the value does not equal the expected object.
     *
     * @param expected the value that must not match
     * @return this assertion for chaining
     */
    public SELF isNotEqualTo(Object expected) {
        if (skipped) return (SELF) this;
        if (actual != null && actual.equals(expected)) {
            fail(DefaultErrorCodes.VALUE_IS_NOT_EQUAL_TO,
                    MessageFormat.format("value must not equal {0}", expected));
        }
        clearOverrides();
        return (SELF) this;
    }

    /**
     * Asserts that the value is one of the given values.
     *
     * @param values the allowed values
     * @return this assertion for chaining
     */
    public SELF isIn(Object... values) {
        if (skipped) return (SELF) this;
        if (actual == null || !Arrays.asList(values).contains(actual)) {
            fail(DefaultErrorCodes.INVALID_ENUM_VALUE,
                    MessageFormat.format("''{0}'' value is not allowed, available values: [{1}]",
                            actual, Arrays.stream(values).map(String::valueOf).collect(Collectors.joining(", "))));
        }
        clearOverrides();
        return (SELF) this;
    }

    /**
     * Asserts that the value is not one of the given values.
     *
     * @param values the disallowed values
     * @return this assertion for chaining
     */
    public SELF isNotIn(Object... values) {
        if (skipped) return (SELF) this;
        if (actual != null && Arrays.asList(values).contains(actual)) {
            fail(DefaultErrorCodes.INVALID_ENUM_VALUE,
                    MessageFormat.format("''{0}'' value is not allowed", actual));
        }
        clearOverrides();
        return (SELF) this;
    }

    /**
     * Asserts that the value is contained in the given collection.
     *
     * @param values the collection of allowed values
     * @return this assertion for chaining
     */
    public SELF isIn(Collection<?> values) {
        if (skipped) return (SELF) this;
        if (actual == null || !values.contains(actual)) {
            fail(DefaultErrorCodes.INVALID_ENUM_VALUE,
                    MessageFormat.format("''{0}'' value is not allowed, available values: [{1}]",
                            actual, values.stream().map(String::valueOf).collect(Collectors.joining(", "))));
        }
        clearOverrides();
        return (SELF) this;
    }

    /**
     * Asserts that the value is not contained in the given collection.
     *
     * @param values the collection of disallowed values
     * @return this assertion for chaining
     */
    public SELF isNotIn(Collection<?> values) {
        if (skipped) return (SELF) this;
        if (actual != null && values.contains(actual)) {
            fail(DefaultErrorCodes.INVALID_ENUM_VALUE,
                    MessageFormat.format("''{0}'' value is not allowed", actual));
        }
        clearOverrides();
        return (SELF) this;
    }

    /**
     * Asserts that the value is an instance of the given type.
     *
     * @param type the expected type
     * @return this assertion for chaining
     */
    public SELF isInstanceOf(Class<?> type) {
        if (skipped) return (SELF) this;
        if (!type.isInstance(actual)) {
            fail(DefaultErrorCodes.VALUE_INVALID_TYPE,
                    MessageFormat.format("expected type {0}", type.getSimpleName()));
        }
        clearOverrides();
        return (SELF) this;
    }

    /**
     * Delegates to a custom validation block.
     *
     * @param requirement the consumer that performs custom validation on the value
     * @return this assertion for chaining
     */
    public SELF satisfies(Consumer<ACTUAL> requirement) {
        if (skipped) return (SELF) this;
        try {
            requirement.accept(actual);
        } catch (JsonApiRequestValidationException e) {
            if (e.getSource() == null && source != null) {
                throw JsonApiRequestValidationException.withSource(e, source);
            }
            throw e;
        }
        clearOverrides();
        return (SELF) this;
    }

    // --- Existence check ---

    /**
     * Asserts the value exists according to the predicate; throws {@link ResourceNotFoundException} if not.
     *
     * @param predicate the existence check to apply
     * @return this assertion for chaining
     */
    public SELF exists(Predicate<ACTUAL> predicate) {
        if (skipped) return (SELF) this;
        if (actual != null && !predicate.test(actual)) {
            throw new ResourceNotFoundException(MessageFormat.format("''{0}'' not found", actual));
        }
        clearOverrides();
        return (SELF) this;
    }

    /**
     * Asserts the value exists according to the predicate; throws {@link ResourceNotFoundException} with a custom message if not.
     *
     * @param predicate       the existence check to apply
     * @param messageProvider function that produces the error message from the value
     * @return this assertion for chaining
     */
    public SELF exists(Predicate<ACTUAL> predicate, Function<ACTUAL, String> messageProvider) {
        if (skipped) return (SELF) this;
        if (actual != null && !predicate.test(actual)) {
            throw new ResourceNotFoundException(messageProvider.apply(actual));
        }
        clearOverrides();
        return (SELF) this;
    }

    // --- Field navigation ---

    /**
     * Navigates into a nested field, producing a child assertion with an automatically extended JSON Pointer source.
     *
     * @param name      the field name
     * @param extractor function to extract the field value from the current value
     * @param <R>       the type of the nested field
     * @return a new {@link ObjectValidationAssert} for the extracted field value
     */
    public <R> ObjectValidationAssert<?, R> field(String name, Function<ACTUAL, R> extractor) {
        R value = (actual != null && !skipped) ? extractor.apply(actual) : null;
        ErrorSources.Source childSource = appendToSource(source, name);
        ObjectValidationAssert<?, R> child = new ObjectValidationAssert<>(value, childSource);
        if (skipped) {
            child.skipped = true;
        }
        return child;
    }

    // --- Type narrowing ---

    /**
     * Narrows this assertion to a {@link StringValidationAssert}.
     *
     * @return a new {@link StringValidationAssert} for the current value cast to {@link String}
     */
    public StringValidationAssert asString() {
        StringValidationAssert result = new StringValidationAssert((String) actual, source);
        if (skipped) result.setSkipped();
        return result;
    }

    /**
     * Narrows this assertion to a {@link NumberValidationAssert}.
     *
     * @param <T> the numeric type
     * @return a new {@link NumberValidationAssert} for the current value cast to a number
     */
    public <T extends Number & Comparable<T>> NumberValidationAssert<T> asNumber() {
        NumberValidationAssert<T> result = new NumberValidationAssert<>((T) actual, source);
        if (skipped) result.setSkipped();
        return result;
    }

    /**
     * Narrows this assertion to a {@link CollectionValidationAssert}.
     *
     * @param <E> the element type of the collection
     * @return a new {@link CollectionValidationAssert} for the current value cast to a collection
     */
    public <E> CollectionValidationAssert<E> asCollection() {
        CollectionValidationAssert<E> result = new CollectionValidationAssert<>((Collection<E>) actual, source);
        if (skipped) result.setSkipped();
        return result;
    }

    /**
     * Narrows this assertion to a {@link MapValidationAssert}.
     *
     * @param <K> the key type of the map
     * @param <V> the value type of the map
     * @return a new {@link MapValidationAssert} for the current value cast to a map
     */
    public <K, V> MapValidationAssert<K, V> asMap() {
        MapValidationAssert<K, V> result = new MapValidationAssert<>((Map<K, V>) actual, source);
        if (skipped) result.setSkipped();
        return result;
    }

    // --- Failure handling ---

    protected ErrorSources.Source getSource() {
        return source;
    }

    protected void setSkipped() {
        this.skipped = true;
    }

    protected boolean isSkipped() {
        return skipped;
    }

    protected void clearOverrides() {
        errorCodeOverride = null;
        detailOverride = null;
    }

    protected void fail(ErrorCode defaultCode, String defaultDetail) {
        ErrorCode code = errorCodeOverride != null ? errorCodeOverride : defaultCode;
        String detail = detailOverride != null ? detailOverride : defaultDetail;
        clearOverrides();
        throw new JsonApiRequestValidationException(code, detail, source);
    }

    private static ErrorSources.Source appendToSource(ErrorSources.Source current, String fieldName) {
        if (current instanceof ErrorSources.JsonPointer(String pointer)) {
            return new ErrorSources.JsonPointer(pointer + "/" + fieldName);
        }
        return new ErrorSources.JsonPointer("/" + fieldName);
    }

}
