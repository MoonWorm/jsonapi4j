package pro.api4.jsonapi4j.servlet.response.errorhandling.impl;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.http.HttpStatusCodes;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.operation.exception.OperationNotFoundException;
import pro.api4.jsonapi4j.processor.exception.DataRetrievalException;
import pro.api4.jsonapi4j.processor.exception.InvalidCursorException;
import pro.api4.jsonapi4j.processor.exception.InvalidPayloadException;
import pro.api4.jsonapi4j.request.exception.ConflictJsonApiRequestException;
import pro.api4.jsonapi4j.request.exception.ForbiddenJsonApiRequestException;
import pro.api4.jsonapi4j.servlet.response.errorhandling.ErrorHandlerFactoriesRegistry;
import pro.api4.jsonapi4j.servlet.response.errorhandling.JsonApi4jErrorHandlerFactoriesRegistry;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultErrorHandlerFactoryRequestValidationTests {

    @Test
    public void conflictRequestValidationException_isMappedTo409() {
        ErrorHandlerFactoriesRegistry registry = new JsonApi4jErrorHandlerFactoriesRegistry();
        registry.registerAll(new DefaultErrorHandlerFactory());

        ConflictJsonApiRequestException exception = new ConflictJsonApiRequestException(
                DefaultErrorCodes.CONFLICT,
                "requestBody.data.type",
                "Payload type mismatch"
        );

        assertThat(registry.resolveStatusCode(exception)).isEqualTo(HttpStatusCodes.SC_409_CONFLICT.getCode());
        assertThat(registry.resolveErrorsDoc(exception).getErrors().getFirst().getCode())
                .isEqualTo(DefaultErrorCodes.CONFLICT.toCode());
    }

    @Test
    public void forbiddenRequestValidationException_isMappedTo403() {
        ErrorHandlerFactoriesRegistry registry = new JsonApi4jErrorHandlerFactoriesRegistry();
        registry.registerAll(new DefaultErrorHandlerFactory());

        ForbiddenJsonApiRequestException exception = new ForbiddenJsonApiRequestException(
                DefaultErrorCodes.FORBIDDEN,
                "requestBody.data.relationships",
                "Relationship replacement is not supported"
        );

        assertThat(registry.resolveStatusCode(exception)).isEqualTo(HttpStatusCodes.SC_403_FORBIDDEN.getCode());
        assertThat(registry.resolveErrorsDoc(exception).getErrors().getFirst().getCode())
                .isEqualTo(DefaultErrorCodes.FORBIDDEN.toCode());
    }

    @Test
    public void invalidPayloadException_isMappedTo400() {
        ErrorHandlerFactoriesRegistry registry = new JsonApi4jErrorHandlerFactoriesRegistry();
        registry.registerAll(new DefaultErrorHandlerFactory());

        InvalidPayloadException exception = new InvalidPayloadException("Invalid payload");

        assertThat(registry.resolveStatusCode(exception)).isEqualTo(HttpStatusCodes.SC_400_BAD_REQUEST.getCode());
        assertThat(registry.resolveErrorsDoc(exception).getErrors().getFirst().getCode())
                .isEqualTo(DefaultErrorCodes.INVALID_PAYLOAD.toCode());
    }

    @Test
    public void invalidCursorException_isMappedTo400() {
        ErrorHandlerFactoriesRegistry registry = new JsonApi4jErrorHandlerFactoriesRegistry();
        registry.registerAll(new DefaultErrorHandlerFactory());

        InvalidCursorException exception = new InvalidCursorException("bad-cursor");

        assertThat(registry.resolveStatusCode(exception)).isEqualTo(HttpStatusCodes.SC_400_BAD_REQUEST.getCode());
        assertThat(registry.resolveErrorsDoc(exception).getErrors().getFirst().getCode())
                .isEqualTo(DefaultErrorCodes.INVALID_CURSOR.toCode());
    }

    @Test
    public void dataRetrievalWrappingOperationNotFound_isMappedTo404() {
        ErrorHandlerFactoriesRegistry registry = new JsonApi4jErrorHandlerFactoriesRegistry();
        registry.registerAll(new DefaultErrorHandlerFactory());

        DataRetrievalException exception = new DataRetrievalException(
                "wrapped",
                new RuntimeException(new OperationNotFoundException("Operation not found"))
        );

        assertThat(registry.resolveStatusCode(exception)).isEqualTo(HttpStatusCodes.SC_404_RESOURCE_NOT_FOUND.getCode());
        assertThat(registry.resolveErrorsDoc(exception).getErrors().getFirst().getCode())
                .isEqualTo(DefaultErrorCodes.NOT_FOUND.toCode());
    }

    @Test
    public void dataRetrievalWrappingInvalidCursor_isMappedTo400() {
        ErrorHandlerFactoriesRegistry registry = new JsonApi4jErrorHandlerFactoriesRegistry();
        registry.registerAll(new DefaultErrorHandlerFactory());

        DataRetrievalException exception = new DataRetrievalException(
                "wrapped",
                new RuntimeException(new InvalidCursorException("bad-cursor"))
        );

        assertThat(registry.resolveStatusCode(exception)).isEqualTo(HttpStatusCodes.SC_400_BAD_REQUEST.getCode());
        assertThat(registry.resolveErrorsDoc(exception).getErrors().getFirst().getCode())
                .isEqualTo(DefaultErrorCodes.INVALID_CURSOR.toCode());
    }
}
