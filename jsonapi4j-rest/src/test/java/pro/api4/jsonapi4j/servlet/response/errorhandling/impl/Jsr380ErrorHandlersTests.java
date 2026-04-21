package pro.api4.jsonapi4j.servlet.response.errorhandling.impl;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.metadata.ConstraintDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.model.document.error.ErrorObject;
import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;
import pro.api4.jsonapi4j.servlet.response.errorhandling.ErrorsDocSupplier;

import java.lang.annotation.Annotation;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Jsr380ErrorHandlersTests {

    private Jsr380ErrorHandlers handlers;

    @BeforeEach
    void setUp() {
        handlers = new Jsr380ErrorHandlers();
    }

    // --- Registration ---

    @Test
    void registersHandlerForJakartaConstraintViolationException() {
        // given/when
        var mappers = handlers.getErrorResponseMappers();

        // then
        assertThat(mappers).containsKey(ConstraintViolationException.class);
    }

    // --- HTTP status ---

    @Test
    void constraintViolationException_returns400() {
        // given
        var exception = createException(mock(jakarta.validation.constraints.NotNull.class), "must not be null", "name");
        ErrorsDocSupplier<ConstraintViolationException> supplier = getSupplier();

        // when/then
        assertThat(supplier.getHttpStatus(exception)).isEqualTo(400);
    }

    // --- Annotation-to-code mapping ---

    @Test
    void notNull_mapsToValueIsAbsent() {
        // given
        var exception = createException(mock(jakarta.validation.constraints.NotNull.class), "must not be null", "name");

        // when
        ErrorsDoc doc = getSupplier().getErrorResponse(exception);

        // then
        assertSingleError(doc, "VALUE_IS_ABSENT", "must not be null", "name");
    }

    @Test
    void notBlank_mapsToValueEmpty() {
        // given
        var exception = createException(mock(jakarta.validation.constraints.NotBlank.class), "must not be blank", "title");

        // when
        ErrorsDoc doc = getSupplier().getErrorResponse(exception);

        // then
        assertSingleError(doc, "VALUE_EMPTY", "must not be blank", "title");
    }

    @Test
    void sizeOnString_mapsToValueTooLong() {
        // given — invalid value is a String
        var exception = createException(mock(jakarta.validation.constraints.Size.class), "size must be between 0 and 100", "bio", "a very long string");

        // when
        ErrorsDoc doc = getSupplier().getErrorResponse(exception);

        // then
        assertSingleError(doc, "VALUE_TOO_LONG", "size must be between 0 and 100", "bio");
    }

    @Test
    void sizeOnCollection_mapsToArrayLengthTooLong() {
        // given — invalid value is an Iterable
        var exception = createException(mock(jakarta.validation.constraints.Size.class), "size must be between 0 and 10", "items", java.util.List.of(1, 2, 3));

        // when
        ErrorsDoc doc = getSupplier().getErrorResponse(exception);

        // then
        assertSingleError(doc, "ARRAY_LENGTH_TOO_LONG", "size must be between 0 and 10", "items");
    }

    @Test
    void pattern_mapsToValueInvalidFormat() {
        // given
        var exception = createException(mock(jakarta.validation.constraints.Pattern.class), "must match pattern", "code");

        // when
        ErrorsDoc doc = getSupplier().getErrorResponse(exception);

        // then
        assertSingleError(doc, "VALUE_INVALID_FORMAT", "must match pattern", "code");
    }

    @Test
    void digits_mapsToValueInvalidFormat() {
        // given
        var exception = createException(mock(jakarta.validation.constraints.Digits.class), "numeric value out of bounds", "amount");

        // when
        ErrorsDoc doc = getSupplier().getErrorResponse(exception);

        // then
        assertSingleError(doc, "VALUE_INVALID_FORMAT", "numeric value out of bounds", "amount");
    }

    @Test
    void positive_mapsToValueTooLow() {
        // given
        var exception = createException(mock(jakarta.validation.constraints.Positive.class), "must be positive", "quantity");

        // when
        ErrorsDoc doc = getSupplier().getErrorResponse(exception);

        // then
        assertSingleError(doc, "VALUE_TOO_LOW", "must be positive", "quantity");
    }

    @Test
    void max_mapsToValueTooHigh() {
        // given
        var exception = createException(mock(jakarta.validation.constraints.Max.class), "must be less than 1000", "price");

        // when
        ErrorsDoc doc = getSupplier().getErrorResponse(exception);

        // then
        assertSingleError(doc, "VALUE_TOO_HIGH", "must be less than 1000", "price");
    }

    @Test
    void unknownAnnotation_mapsToGenericRequestError() {
        // given — use a custom annotation that doesn't match any known type
        var exception = createException(mock(jakarta.validation.constraints.Min.class), "must be at least 1", "age");

        // when
        ErrorsDoc doc = getSupplier().getErrorResponse(exception);

        // then
        assertSingleError(doc, "GENERIC_REQUEST_ERROR", "must be at least 1", "age");
    }

    // --- Multiple violations ---

    @Test
    void multipleViolations_producesMultipleErrorsSortedByCodeThenDetail() {
        // given
        ConstraintViolation<?> violation1 = createViolation(mock(jakarta.validation.constraints.NotNull.class), "must not be null", "name", null);
        ConstraintViolation<?> violation2 = createViolation(mock(jakarta.validation.constraints.NotBlank.class), "must not be blank", "title", null);
        var exception = new ConstraintViolationException(Set.of(violation1, violation2));

        // when
        ErrorsDoc doc = getSupplier().getErrorResponse(exception);

        // then
        assertThat(doc.getErrors()).hasSize(2);
        // Sorted by code then detail
        assertThat(doc.getErrors()).extracting(ErrorObject::getCode)
                .isSorted();
    }

    // --- Property path resolution ---

    @Test
    void complexPath_extractsLeafFieldName() {
        // given — path like "validateCreate.name"
        var exception = createException(mock(jakarta.validation.constraints.NotNull.class), "must not be null", "validateCreate.name");

        // when
        ErrorsDoc doc = getSupplier().getErrorResponse(exception);

        // then
        assertThat(doc.getErrors().getFirst().getSource().getParameter()).isEqualTo("name");
    }

    // --- Helpers ---

    @SuppressWarnings("unchecked")
    private ErrorsDocSupplier<ConstraintViolationException> getSupplier() {
        return (ErrorsDocSupplier<ConstraintViolationException>) handlers.getErrorResponseMappers()
                .get(ConstraintViolationException.class);
    }

    private ConstraintViolationException createException(Annotation annotation, String message, String propertyPath) {
        return createException(annotation, message, propertyPath, null);
    }

    private ConstraintViolationException createException(Annotation annotation, String message, String propertyPath, Object invalidValue) {
        ConstraintViolation<?> violation = createViolation(annotation, message, propertyPath, invalidValue);
        return new ConstraintViolationException(Set.of(violation));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ConstraintViolation<?> createViolation(Annotation annotation, String message, String propertyPath, Object invalidValue) {
        ConstraintViolation violation = mock(ConstraintViolation.class);
        ConstraintDescriptor descriptor = mock(ConstraintDescriptor.class);
        Path path = mock(Path.class);

        when(violation.getMessage()).thenReturn(message);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getInvalidValue()).thenReturn(invalidValue);
        when(violation.getConstraintDescriptor()).thenReturn(descriptor);
        when(descriptor.getAnnotation()).thenReturn(annotation);
        when(path.toString()).thenReturn(propertyPath);

        return violation;
    }

    private void assertSingleError(ErrorsDoc doc, String expectedCode, String expectedDetail, String expectedParameter) {
        assertThat(doc.getErrors()).hasSize(1);
        ErrorObject error = doc.getErrors().getFirst();
        assertThat(error.getStatus()).isEqualTo("400");
        assertThat(error.getCode()).isEqualTo(expectedCode);
        assertThat(error.getDetail()).isEqualTo(expectedDetail);
        assertThat(error.getSource().getParameter()).isEqualTo(expectedParameter);
        assertThat(error.getId()).isNotBlank();
    }

}
