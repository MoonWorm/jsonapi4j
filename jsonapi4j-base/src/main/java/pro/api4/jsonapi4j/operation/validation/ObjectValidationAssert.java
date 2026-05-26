package pro.api4.jsonapi4j.operation.validation;

import pro.api4.jsonapi4j.exception.JsonApiRequestValidationException;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.model.document.error.ErrorCode;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public SELF withSource(ErrorSources.Source source) {
        this.source = source;
        return (SELF) this;
    }

    public SELF withErrorCode(ErrorCode code) {
        this.errorCodeOverride = code;
        return (SELF) this;
    }

    public SELF withDetail(String detail) {
        this.detailOverride = detail;
        return (SELF) this;
    }

    // --- Conditional ---

    public SELF ifPresent() {
        if (actual == null) {
            this.skipped = true;
        }
        return (SELF) this;
    }

    // --- Common assertions ---

    public SELF isNull() {
        if (skipped) return (SELF) this;
        if (actual != null) {
            fail(DefaultErrorCodes.VALUE_IS_NOT_ABSENT, "value must be null");
        }
        clearOverrides();
        return (SELF) this;
    }

    public SELF isNotNull() {
        if (skipped) return (SELF) this;
        if (actual == null) {
            fail(DefaultErrorCodes.VALUE_IS_ABSENT, "value can't be null");
        }
        clearOverrides();
        return (SELF) this;
    }

    public SELF isEqualTo(Object expected) {
        if (skipped) return (SELF) this;
        if (actual == null || !actual.equals(expected)) {
            fail(DefaultErrorCodes.VALUE_IS_NOT_EQUAL_TO,
                    MessageFormat.format("value should match {0}", expected));
        }
        clearOverrides();
        return (SELF) this;
    }

    public SELF isNotEqualTo(Object expected) {
        if (skipped) return (SELF) this;
        if (actual != null && actual.equals(expected)) {
            fail(DefaultErrorCodes.VALUE_IS_NOT_EQUAL_TO,
                    MessageFormat.format("value must not equal {0}", expected));
        }
        clearOverrides();
        return (SELF) this;
    }

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

    public SELF isNotIn(Object... values) {
        if (skipped) return (SELF) this;
        if (actual != null && Arrays.asList(values).contains(actual)) {
            fail(DefaultErrorCodes.INVALID_ENUM_VALUE,
                    MessageFormat.format("''{0}'' value is not allowed", actual));
        }
        clearOverrides();
        return (SELF) this;
    }

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

    public SELF isNotIn(Collection<?> values) {
        if (skipped) return (SELF) this;
        if (actual != null && values.contains(actual)) {
            fail(DefaultErrorCodes.INVALID_ENUM_VALUE,
                    MessageFormat.format("''{0}'' value is not allowed", actual));
        }
        clearOverrides();
        return (SELF) this;
    }

    public SELF isInstanceOf(Class<?> type) {
        if (skipped) return (SELF) this;
        if (actual == null || !type.isInstance(actual)) {
            fail(DefaultErrorCodes.VALUE_INVALID_TYPE,
                    MessageFormat.format("expected type {0}", type.getSimpleName()));
        }
        clearOverrides();
        return (SELF) this;
    }

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

    // --- Field navigation ---

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

    public StringValidationAssert asString() {
        StringValidationAssert result = new StringValidationAssert((String) actual, source);
        if (skipped) result.setSkipped();
        return result;
    }

    public <T extends Number & Comparable<T>> NumberValidationAssert<T> asNumber() {
        NumberValidationAssert<T> result = new NumberValidationAssert<>((T) actual, source);
        if (skipped) result.setSkipped();
        return result;
    }

    public <E> CollectionValidationAssert<E> asCollection() {
        CollectionValidationAssert<E> result = new CollectionValidationAssert<>((Collection<E>) actual, source);
        if (skipped) result.setSkipped();
        return result;
    }

    public <K, V> MapValidationAssert<K, V> asMap() {
        MapValidationAssert<K, V> result = new MapValidationAssert<>((Map<K, V>) actual, source);
        if (skipped) result.setSkipped();
        return result;
    }

    // --- Failure handling ---

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
        if (current instanceof ErrorSources.JsonPointer jp) {
            return new ErrorSources.JsonPointer(jp.pointer() + "/" + fieldName);
        }
        return new ErrorSources.JsonPointer("/" + fieldName);
    }

}
