package pro.api4.jsonapi4j.plugin.cd;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.compound.docs.exception.DomainResolutionException;
import pro.api4.jsonapi4j.compound.docs.exception.DownstreamTimeoutException;
import pro.api4.jsonapi4j.compound.docs.exception.ErrorJsonApiResponseException;
import pro.api4.jsonapi4j.errorhandling.ErrorHandlerFactoriesRegistry;
import pro.api4.jsonapi4j.errorhandling.JsonApi4jErrorHandlerFactoriesRegistry;
import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;

import static org.assertj.core.api.Assertions.assertThat;

public class CompoundDocsErrorHandlerFactoryTests {

    private final CompoundDocsErrorHandlerFactory sut = new CompoundDocsErrorHandlerFactory();
    private final ErrorHandlerFactoriesRegistry registry = new JsonApi4jErrorHandlerFactoriesRegistry();

    @Nested
    class Mappings {

        @Test
        public void getErrorResponseMappers_downstreamTimeout_maps504GatewayTimeout() {
            registry.registerAll(sut);
            DownstreamTimeoutException e = new DownstreamTimeoutException("slow", null);

            assertThat(registry.resolveStatusCode(e)).isEqualTo(504);
            assertThat(registry.resolveErrorsDoc(e).getErrors().get(0).getCode()).isEqualTo("GATEWAY_TIMEOUT");
        }

        @Test
        public void getErrorResponseMappers_downstreamFailure_maps502BadGateway() {
            registry.registerAll(sut);
            ErrorJsonApiResponseException e = new ErrorJsonApiResponseException("boom");

            assertThat(registry.resolveStatusCode(e)).isEqualTo(502);
            assertThat(registry.resolveErrorsDoc(e).getErrors().get(0).getCode()).isEqualTo("BAD_GATEWAY");
        }

        @Test
        public void getErrorResponseMappers_noRoute_maps500WithoutDetail() {
            registry.registerAll(sut);
            DomainResolutionException e = new DomainResolutionException("Resource type 'orders' has no mapping");

            assertThat(registry.resolveStatusCode(e)).isEqualTo(500);
            assertThat(registry.resolveErrorsDoc(e).getErrors().get(0).getDetail()).doesNotContain("orders");
        }

        @Test
        public void getErrorResponseMappers_sameExceptionTwice_producesDistinctErrorIds() {
            registry.registerAll(sut);
            ErrorJsonApiResponseException e = new ErrorJsonApiResponseException("boom");

            String firstId = registry.resolveErrorsDoc(e).getErrors().get(0).getId();
            String secondId = registry.resolveErrorsDoc(e).getErrors().get(0).getId();

            assertThat(firstId).isNotEqualTo(secondId);
        }

    }

}
