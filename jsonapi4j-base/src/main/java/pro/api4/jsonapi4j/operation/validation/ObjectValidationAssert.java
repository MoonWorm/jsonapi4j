package pro.api4.jsonapi4j.operation.validation;

import pro.api4.jsonapi4j.exception.JsonApiRequestValidationException;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.model.document.error.ErrorCode;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class ObjectValidationAssert<SELF extends ObjectValidationAssert<SELF, ACTUAL>, ACTUAL> {

    protected final ACTUAL actual;
    private ErrorSources.Source source;
    private ErrorCode errorCodeOverride;
    private String detailOverride;

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

    // --- Common assertions ---

    public SELF isNull() {
        if (actual != null) {
            fail(DefaultErrorCodes.VALUE_IS_NOT_ABSENT, "value must be null");
        }
        clearOverrides();
        return (SELF) this;
    }

    public SELF isNotNull() {
        if (actual == null) {
            fail(DefaultErrorCodes.VALUE_IS_ABSENT, "value can't be null");
        }
        clearOverrides();
        return (SELF) this;
    }

    public SELF isEqualTo(Object expected) {
        if (actual == null || !actual.equals(expected)) {
            fail(DefaultErrorCodes.VALUE_IS_NOT_EQUAL_TO,
                    MessageFormat.format("value should match {0}", expected));
        }
        clearOverrides();
        return (SELF) this;
    }

    public SELF isNotEqualTo(Object expected) {
        if (actual != null && actual.equals(expected)) {
            fail(DefaultErrorCodes.VALUE_IS_NOT_EQUAL_TO,
                    MessageFormat.format("value must not equal {0}", expected));
        }
        clearOverrides();
        return (SELF) this;
    }

    public SELF isIn(Object... values) {
        if (actual == null || !Arrays.asList(values).contains(actual)) {
            fail(DefaultErrorCodes.INVALID_ENUM_VALUE,
                    MessageFormat.format("''{0}'' value is not allowed, available values: [{1}]",
                            actual, Arrays.stream(values).map(String::valueOf).collect(Collectors.joining(", "))));
        }
        clearOverrides();
        return (SELF) this;
    }

    public SELF isNotIn(Object... values) {
        if (actual != null && Arrays.asList(values).contains(actual)) {
            fail(DefaultErrorCodes.INVALID_ENUM_VALUE,
                    MessageFormat.format("''{0}'' value is not allowed", actual));
        }
        clearOverrides();
        return (SELF) this;
    }

    public SELF isIn(Collection<?> values) {
        if (actual == null || !values.contains(actual)) {
            fail(DefaultErrorCodes.INVALID_ENUM_VALUE,
                    MessageFormat.format("''{0}'' value is not allowed, available values: [{1}]",
                            actual, values.stream().map(String::valueOf).collect(Collectors.joining(", "))));
        }
        clearOverrides();
        return (SELF) this;
    }

    public SELF isNotIn(Collection<?> values) {
        if (actual != null && values.contains(actual)) {
            fail(DefaultErrorCodes.INVALID_ENUM_VALUE,
                    MessageFormat.format("''{0}'' value is not allowed", actual));
        }
        clearOverrides();
        return (SELF) this;
    }

    public SELF isInstanceOf(Class<?> type) {
        if (actual == null || !type.isInstance(actual)) {
            fail(DefaultErrorCodes.VALUE_INVALID_TYPE,
                    MessageFormat.format("expected type {0}", type.getSimpleName()));
        }
        clearOverrides();
        return (SELF) this;
    }

    public SELF satisfies(Consumer<ACTUAL> requirement) {
        requirement.accept(actual);
        clearOverrides();
        return (SELF) this;
    }

    // --- Failure handling ---

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

}
