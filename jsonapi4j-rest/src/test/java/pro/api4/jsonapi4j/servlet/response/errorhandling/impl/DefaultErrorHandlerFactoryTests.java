package pro.api4.jsonapi4j.servlet.response.errorhandling.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.exception.ConstraintViolationException;
import pro.api4.jsonapi4j.exception.JsonApi4jException;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.model.document.error.ErrorObject;
import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;
import pro.api4.jsonapi4j.operation.exception.OperationNotFoundException;
import pro.api4.jsonapi4j.processor.exception.DataRetrievalException;
import pro.api4.jsonapi4j.processor.exception.MappingException;
import pro.api4.jsonapi4j.servlet.response.errorhandling.ErrorsDocSupplier;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultErrorHandlerFactoryTests {

    private DefaultErrorHandlerFactory factory;

    @BeforeEach
    void setUp() {
        factory = new DefaultErrorHandlerFactory();
    }

    // --- Registered exception classes ---

    @Test
    void registersHandlersForFiveExceptionClasses() {
        // given/when
        var mappers = factory.getErrorResponseMappers();

        // then
        assertThat(mappers).containsOnlyKeys(
                DataRetrievalException.class,
                MappingException.class,
                OperationNotFoundException.class,
                ConstraintViolationException.class,
                JsonApi4jException.class
        );
    }

    // --- DataRetrievalException ---

    @Test
    void dataRetrievalException_returns502() {
        // given
        var exception = new DataRetrievalException("downstream failed");
        ErrorsDocSupplier<DataRetrievalException> supplier = getSupplier(DataRetrievalException.class);

        // when/then
        assertThat(supplier.getHttpStatus(exception)).isEqualTo(502);
    }

    @Test
    void dataRetrievalException_producesErrorDocWithBadGatewayCode() {
        // given
        var exception = new DataRetrievalException("downstream failed");
        ErrorsDocSupplier<DataRetrievalException> supplier = getSupplier(DataRetrievalException.class);

        // when
        ErrorsDoc doc = supplier.getErrorResponse(exception);

        // then
        assertSingleError(doc, "502", "BAD_GATEWAY", "downstream failed");
    }

    // --- MappingException ---

    @Test
    void mappingException_returns500() {
        // given
        var exception = new MappingException("mapping failed");
        ErrorsDocSupplier<MappingException> supplier = getSupplier(MappingException.class);

        // when/then
        assertThat(supplier.getHttpStatus(exception)).isEqualTo(500);
    }

    @Test
    void mappingException_producesErrorDocWithInternalServerErrorCode() {
        // given
        var exception = new MappingException("mapping failed");
        ErrorsDocSupplier<MappingException> supplier = getSupplier(MappingException.class);

        // when
        ErrorsDoc doc = supplier.getErrorResponse(exception);

        // then
        assertSingleError(doc, "500", "INTERNAL_SERVER_ERROR", "mapping failed");
    }

    // --- OperationNotFoundException ---

    @Test
    void operationNotFoundException_returns404() {
        // given
        var exception = new OperationNotFoundException("readById not found");
        ErrorsDocSupplier<OperationNotFoundException> supplier = getSupplier(OperationNotFoundException.class);

        // when/then
        assertThat(supplier.getHttpStatus(exception)).isEqualTo(404);
    }

    @Test
    void operationNotFoundException_producesErrorDocWithNotFoundCode() {
        // given
        var exception = new OperationNotFoundException("readById not found");
        ErrorsDocSupplier<OperationNotFoundException> supplier = getSupplier(OperationNotFoundException.class);

        // when
        ErrorsDoc doc = supplier.getErrorResponse(exception);

        // then
        assertSingleError(doc, "404", "NOT_FOUND", "readById not found");
    }

    // --- ConstraintViolationException ---

    @Test
    void constraintViolationException_returns400() {
        // given
        var exception = new ConstraintViolationException(
                DefaultErrorCodes.VALUE_IS_ABSENT, "must not be null", "name"
        );
        ErrorsDocSupplier<ConstraintViolationException> supplier = getSupplier(ConstraintViolationException.class);

        // when/then
        assertThat(supplier.getHttpStatus(exception)).isEqualTo(400);
    }

    @Test
    void constraintViolationException_producesErrorDocWithCorrectCodeAndParameter() {
        // given
        var exception = new ConstraintViolationException(
                DefaultErrorCodes.VALUE_INVALID_FORMAT, "invalid email", "email"
        );
        ErrorsDocSupplier<ConstraintViolationException> supplier = getSupplier(ConstraintViolationException.class);

        // when
        ErrorsDoc doc = supplier.getErrorResponse(exception);

        // then
        assertThat(doc.getErrors()).hasSize(1);
        ErrorObject error = doc.getErrors().getFirst();
        assertThat(error.getStatus()).isEqualTo("400");
        assertThat(error.getCode()).isEqualTo("VALUE_INVALID_FORMAT");
        assertThat(error.getDetail()).isEqualTo("invalid email");
        assertThat(error.getSource().getParameter()).isEqualTo("email");
    }

    // --- JsonApi4jException (catch-all) ---

    @Test
    void jsonApi4jException_returnsStatusFromException() {
        // given
        var exception = new JsonApi4jException(403, DefaultErrorCodes.FORBIDDEN, "access denied");
        ErrorsDocSupplier<JsonApi4jException> supplier = getSupplier(JsonApi4jException.class);

        // when/then
        assertThat(supplier.getHttpStatus(exception)).isEqualTo(403);
    }

    @Test
    void jsonApi4jException_producesErrorDocWithExceptionFields() {
        // given
        var exception = new JsonApi4jException(409, DefaultErrorCodes.CONFLICT, "duplicate entry");
        ErrorsDocSupplier<JsonApi4jException> supplier = getSupplier(JsonApi4jException.class);

        // when
        ErrorsDoc doc = supplier.getErrorResponse(exception);

        // then
        assertSingleError(doc, "409", "CONFLICT", "duplicate entry");
    }

    // --- Helpers ---

    @SuppressWarnings("unchecked")
    private <T extends Throwable> ErrorsDocSupplier<T> getSupplier(Class<T> exceptionClass) {
        return (ErrorsDocSupplier<T>) factory.getErrorResponseMappers().get(exceptionClass);
    }

    private void assertSingleError(ErrorsDoc doc, String expectedStatus, String expectedCode, String expectedDetail) {
        assertThat(doc.getErrors()).hasSize(1);
        ErrorObject error = doc.getErrors().getFirst();
        assertThat(error.getStatus()).isEqualTo(expectedStatus);
        assertThat(error.getCode()).isEqualTo(expectedCode);
        assertThat(error.getDetail()).isEqualTo(expectedDetail);
        assertThat(error.getId()).isNotBlank();
    }

}
