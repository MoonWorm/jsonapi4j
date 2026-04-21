package pro.api4.jsonapi4j.servlet.response.errorhandling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ErrorHandlerFactoriesRegistryTests {

    private JsonApi4jErrorHandlerFactoriesRegistry registry;

    // --- Exception hierarchy for testing ---

    private static class ParentException extends RuntimeException {
    }

    private static class ChildException extends ParentException {
    }

    private static class GrandchildException extends ChildException {
    }

    private static class UnrelatedException extends RuntimeException {
    }

    // --- Test fixtures ---

    private static final ErrorsDoc PARENT_ERRORS_DOC = mock(ErrorsDoc.class);
    private static final int PARENT_STATUS = 400;

    private static final ErrorsDoc CHILD_ERRORS_DOC = mock(ErrorsDoc.class);
    private static final int CHILD_STATUS = 422;

    private final ErrorsDocSupplier<ParentException> parentSupplier = new ErrorsDocSupplier<>() {
        @Override
        public ErrorsDoc getErrorResponse(ParentException ex) {
            return PARENT_ERRORS_DOC;
        }

        @Override
        public int getHttpStatus(ParentException ex) {
            return PARENT_STATUS;
        }
    };

    private final ErrorsDocSupplier<ChildException> childSupplier = new ErrorsDocSupplier<>() {
        @Override
        public ErrorsDoc getErrorResponse(ChildException ex) {
            return CHILD_ERRORS_DOC;
        }

        @Override
        public int getHttpStatus(ChildException ex) {
            return CHILD_STATUS;
        }
    };

    @BeforeEach
    void setUp() {
        registry = new JsonApi4jErrorHandlerFactoriesRegistry();
    }

    // --- getErrorResponseMapper(Class) ---

    @Test
    void getErrorResponseMapper_exactClassMatch_returnsRegisteredHandler() {
        // given
        registry.register(ParentException.class, parentSupplier);

        // when
        ErrorsDocSupplier<ParentException> result = registry.getErrorResponseMapper(ParentException.class);

        // then
        assertThat(result).isSameAs(parentSupplier);
    }

    @Test
    void getErrorResponseMapper_childException_fallsBackToParentHandler() {
        // given
        registry.register(ParentException.class, parentSupplier);

        // when
        ErrorsDocSupplier<ChildException> result = registry.getErrorResponseMapper(ChildException.class);

        // then
        assertThat(result.getErrorResponse(new ChildException())).isSameAs(PARENT_ERRORS_DOC);
        assertThat(result.getHttpStatus(new ChildException())).isEqualTo(PARENT_STATUS);
    }

    @Test
    void getErrorResponseMapper_grandchildException_fallsBackToParentHandler() {
        // given
        registry.register(ParentException.class, parentSupplier);

        // when
        ErrorsDocSupplier<GrandchildException> result = registry.getErrorResponseMapper(GrandchildException.class);

        // then
        assertThat(result.getErrorResponse(new GrandchildException())).isSameAs(PARENT_ERRORS_DOC);
        assertThat(result.getHttpStatus(new GrandchildException())).isEqualTo(PARENT_STATUS);
    }

    @Test
    void getErrorResponseMapper_childExactMatch_prefersExactOverParent() {
        // given
        registry.register(ParentException.class, parentSupplier);
        registry.register(ChildException.class, childSupplier);

        // when
        ErrorsDocSupplier<ChildException> result = registry.getErrorResponseMapper(ChildException.class);

        // then
        assertThat(result).isSameAs(childSupplier);
    }

    @Test
    void getErrorResponseMapper_noMatch_returnsInternalServerErrorMapper() {
        // given — registry has a handler for ParentException only
        registry.register(ParentException.class, parentSupplier);

        // when — query with an unrelated exception
        ErrorsDocSupplier<UnrelatedException> result = registry.getErrorResponseMapper(UnrelatedException.class);

        // then
        assertThat(result.getHttpStatus(new UnrelatedException())).isEqualTo(500);
    }

    @Test
    void getErrorResponseMapper_emptyRegistry_returnsInternalServerErrorMapper() {
        // given — empty registry

        // when
        ErrorsDocSupplier<RuntimeException> result = registry.getErrorResponseMapper(RuntimeException.class);

        // then
        assertThat(result.getHttpStatus(new RuntimeException())).isEqualTo(500);
    }

    // --- getErrorResponseMapper(Throwable) ---

    @Test
    void getErrorResponseMapper_byInstance_resolvesViaClassHierarchy() {
        // given
        registry.register(ParentException.class, parentSupplier);

        // when
        ErrorsDocSupplier<ChildException> result = registry.getErrorResponseMapper(new ChildException());

        // then
        assertThat(result.getErrorResponse(new ChildException())).isSameAs(PARENT_ERRORS_DOC);
    }

    // --- resolveErrorsDoc / resolveStatusCode convenience methods ---

    @Test
    void resolveErrorsDoc_returnsDocFromMatchedHandler() {
        // given
        registry.register(ParentException.class, parentSupplier);

        // when
        ErrorsDoc result = registry.resolveErrorsDoc(new ParentException());

        // then
        assertThat(result).isSameAs(PARENT_ERRORS_DOC);
    }

    @Test
    void resolveStatusCode_returnsStatusFromMatchedHandler() {
        // given
        registry.register(ChildException.class, childSupplier);

        // when
        int result = registry.resolveStatusCode(new ChildException());

        // then
        assertThat(result).isEqualTo(CHILD_STATUS);
    }

    @Test
    void resolveStatusCode_childException_usesParentHandler() {
        // given
        registry.register(ParentException.class, parentSupplier);

        // when
        int result = registry.resolveStatusCode(new ChildException());

        // then
        assertThat(result).isEqualTo(PARENT_STATUS);
    }

    @Test
    void resolveStatusCode_noMatch_returns500() {
        // given — empty registry

        // when
        int result = registry.resolveStatusCode(new UnrelatedException());

        // then
        assertThat(result).isEqualTo(500);
    }

    // --- Most-specific ancestor selection ---

    @Test
    void getErrorResponseMapper_multipleAncestorsRegistered_selectsMostSpecific() {
        // given — handlers for both ParentException and ChildException
        registry.register(ParentException.class, parentSupplier);
        registry.register(ChildException.class, childSupplier);

        // when — GrandchildException has no exact match; both Parent and Child are assignable
        ErrorsDocSupplier<GrandchildException> result = registry.getErrorResponseMapper(GrandchildException.class);

        // then — should pick ChildException (most specific ancestor), not ParentException
        assertThat(result.getHttpStatus(new GrandchildException())).isEqualTo(CHILD_STATUS);
        assertThat(result.getErrorResponse(new GrandchildException())).isSameAs(CHILD_ERRORS_DOC);
    }

}
