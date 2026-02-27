package pro.api4.jsonapi4j.servlet.response.errorhandling.impl;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.http.HttpStatusCodes;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
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
}
