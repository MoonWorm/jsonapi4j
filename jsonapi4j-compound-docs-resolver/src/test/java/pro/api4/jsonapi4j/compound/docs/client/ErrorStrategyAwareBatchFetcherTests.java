package pro.api4.jsonapi4j.compound.docs.client;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsRequest;
import pro.api4.jsonapi4j.compound.docs.DomainSettings;
import pro.api4.jsonapi4j.compound.docs.IncompleteReason;
import pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy;
import pro.api4.jsonapi4j.compound.docs.exception.ErrorJsonApiResponseException;
import pro.api4.jsonapi4j.compound.docs.exception.RejectedIncludesException;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ErrorStrategyAwareBatchFetcherTests {

    private static final BatchFetch<DomainSettings.OverHttp> BATCH = new BatchFetch<>(
            DomainSettings.overHttp(URI.create("http://geo.internal/jsonapi")), "countries", Set.of("US"), Set.of()
    );
    private static final CompoundDocsRequest REQUEST = new CompoundDocsRequest(
            "GET", List.of("placeOfBirth"), Map.of(), Map.of(), "/users/1", Map.of()
    );

    @Nested
    class Ignore {

        @Test
        void fetch_succeeds_returnsTheResult() {
            FetchResult result = FetchResult.empty();
            ErrorStrategyAwareBatchFetcher<DomainSettings.OverHttp> sut =
                    new ErrorStrategyAwareBatchFetcher<>((batch, request) -> result, ErrorStrategy.IGNORE);

            assertThat(sut.fetch(BATCH, REQUEST)).isSameAs(result);
        }

        @Test
        void fetch_fails_returnsIgnoredFailure() {
            ErrorStrategyAwareBatchFetcher<DomainSettings.OverHttp> sut = new ErrorStrategyAwareBatchFetcher<>(
                    (batch, request) -> {
                        throw new ErrorJsonApiResponseException("boom");
                    },
                    ErrorStrategy.IGNORE
            );

            FetchResult result = sut.fetch(BATCH, REQUEST);

            assertThat(result.resources()).isEmpty();
            assertThat(result.incompleteReason()).isEqualTo(IncompleteReason.FETCH_FAILED);
            assertThat(result.directives().isNoStore()).isTrue();
        }

        @Test
        void fetch_rejectedIncludes_propagates() {
            ErrorStrategyAwareBatchFetcher<DomainSettings.OverHttp> sut = new ErrorStrategyAwareBatchFetcher<>(
                    (batch, request) -> {
                        throw new RejectedIncludesException("countries", Set.of("economy"));
                    },
                    ErrorStrategy.IGNORE
            );

            assertThatThrownBy(() -> sut.fetch(BATCH, REQUEST)).isInstanceOf(RejectedIncludesException.class);
        }

    }

    @Nested
    class Fail {

        @Test
        void fetch_fails_rethrows() {
            ErrorStrategyAwareBatchFetcher<DomainSettings.OverHttp> sut = new ErrorStrategyAwareBatchFetcher<>(
                    (batch, request) -> {
                        throw new ErrorJsonApiResponseException("boom");
                    },
                    ErrorStrategy.FAIL
            );

            assertThatThrownBy(() -> sut.fetch(BATCH, REQUEST)).isExactlyInstanceOf(ErrorJsonApiResponseException.class);
        }

    }

}
