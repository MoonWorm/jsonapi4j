package pro.api4.jsonapi4j.plugin.cd;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsRequest;
import pro.api4.jsonapi4j.compound.docs.DomainSettings;
import pro.api4.jsonapi4j.compound.docs.client.BatchFetch;
import pro.api4.jsonapi4j.compound.docs.client.FetchResult;
import pro.api4.jsonapi4j.compound.docs.config.Propagation;
import pro.api4.jsonapi4j.compound.docs.exception.ErrorJsonApiResponseException;
import pro.api4.jsonapi4j.compound.docs.exception.RejectedIncludesException;
import pro.api4.jsonapi4j.exception.UnsupportedIncludeException;
import pro.api4.jsonapi4j.http.cache.CacheControlParser;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.principal.AuthenticatedPrincipalContextHolder;
import pro.api4.jsonapi4j.principal.Principal;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.servlet.response.ResponseHeaders;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InProcessBatchFetcherTests {

    private static final Map<String, Object> COUNTRIES_DOC = Map.of("data", List.of(
            Map.of("type", "countries", "id", "NO"),
            Map.of("type", "countries", "id", "US")
    ));

    private final JsonApi4j jsonApi4j = mock(JsonApi4j.class);
    private final InProcessBatchFetcher sut = new InProcessBatchFetcher(
            jsonApi4j,
            new ObjectMapper(),
            List.of(Propagation.FIELDS, Propagation.CUSTOM_QUERY_PARAMS, Propagation.HEADERS)
    );

    @AfterEach
    void tearDown() {
        AuthenticatedPrincipalContextHolder.clear();
    }

    @Nested
    class Fetch {

        @Test
        void fetch_chunk_readsItsResourcesThroughTheFrameworkAsTheRequestPrincipal() {
            AtomicReference<JsonApiRequest> executed = new AtomicReference<>();
            AtomicReference<String> executedAs = new AtomicReference<>();
            when(jsonApi4j.execute(any())).thenAnswer(invocation -> {
                executed.set(invocation.getArgument(0));
                executedAs.set(AuthenticatedPrincipalContextHolder.copy().authenticatedUserId());
                ResponseHeaders.propagateCacheControl(CacheControlParser.parse("max-age=60"));
                return COUNTRIES_DOC;
            });

            FetchResult result = sut.fetch(chunk(Set.of("US", "NO"), Set.of("currencies")), request(principal("alice")));

            assertThat(result.resources()).extracting(r -> r.idAndType().getId()).containsExactly("NO", "US");
            assertThat(result.directives().getMaxAge()).isEqualTo(60L);
            assertThat(executedAs.get()).isEqualTo("alice");
            JsonApiRequest request = executed.get();
            assertThat(request.getOperationType()).isEqualTo(OperationType.READ_MULTIPLE_RESOURCES);
            assertThat(request.getTargetResourceType().getType()).isEqualTo("countries");
            assertThat(request.getFilters()).isEqualTo(Map.of("id", List.of("NO", "US")));
            assertThat(request.getEffectiveIncludes()).containsExactly("currencies");
            assertThat(request.getFieldSets()).isEqualTo(Map.of("countries", List.of("name")));
            assertThat(request.getCustomQueryParams()).isEqualTo(Map.of("locale", List.of("de")));
            assertThat(request.getHeaders()).containsEntry("Accept-Language", "de");
        }

        @Test
        void fetch_noPropagation_carriesNothingOfTheOriginalRequestOver() {
            InProcessBatchFetcher withoutPropagation = new InProcessBatchFetcher(jsonApi4j, new ObjectMapper(), List.of());
            AtomicReference<JsonApiRequest> executed = new AtomicReference<>();
            when(jsonApi4j.execute(any())).thenAnswer(invocation -> {
                executed.set(invocation.getArgument(0));
                return COUNTRIES_DOC;
            });

            withoutPropagation.fetch(chunk(Set.of("US"), Set.of()), request(null));

            assertThat(executed.get().getFieldSets()).isEmpty();
            assertThat(executed.get().getCustomQueryParams()).isEmpty();
            assertThat(executed.get().getHeaders()).isEmpty();
        }

        @Test
        void fetch_executed_restoresThePrincipalOfTheThread() {
            AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(principal("worker"));
            when(jsonApi4j.execute(any())).thenThrow(new IllegalStateException("boom"));

            assertThatThrownBy(() -> sut.fetch(chunk(Set.of("US"), Set.of()), request(principal("alice"))))
                    .isInstanceOf(ErrorJsonApiResponseException.class);
            assertThat(AuthenticatedPrincipalContextHolder.copy().authenticatedUserId()).isEqualTo("worker");
        }

    }

    @Nested
    class Failures {

        @Test
        void fetch_includeOfTheChunkUnsupported_throwsRejectedIncludes() {
            when(jsonApi4j.execute(any())).thenThrow(
                    new UnsupportedIncludeException("economy", "Resource type 'countries' has no relationship 'economy'")
            );

            assertThatThrownBy(() -> sut.fetch(chunk(Set.of("US"), Set.of("economy", "currencies")), request(null)))
                    .isInstanceOfSatisfying(RejectedIncludesException.class, e -> {
                        assertThat(e.getResourceType()).isEqualTo("countries");
                        assertThat(e.getRelationshipNames()).containsExactly("economy");
                    });
        }

        @Test
        void fetch_includeOutsideTheChunkUnsupported_throwsErrorJsonApiResponseException() {
            when(jsonApi4j.execute(any())).thenThrow(new UnsupportedIncludeException("foo", "unknown"));

            assertThatThrownBy(() -> sut.fetch(chunk(Set.of("US"), Set.of("currencies")), request(null)))
                    .isExactlyInstanceOf(ErrorJsonApiResponseException.class);
        }

    }

    private static BatchFetch<DomainSettings.InProcess> chunk(Set<String> ids, Set<String> includes) {
        return new BatchFetch<>(DomainSettings.inProcess(20), "countries", ids, includes);
    }

    private static CompoundDocsRequest request(Principal principal) {
        return new CompoundDocsRequest(
                "GET",
                List.of("placeOfBirth"),
                List.of(),
                Map.of("countries", List.of("name")),
                Map.of("Accept-Language", List.of("de")),
                "/users/1",
                Map.of("locale", List.of("de")),
                principal
        );
    }

    private static Principal principal(String userId) {
        return new Principal() {
            @Override
            public String authenticatedUserId() {
                return userId;
            }

            @Override
            public List<String> authenticatedClientEntitlements() {
                return List.of();
            }

            @Override
            public Set<String> authenticatedClientScopes() {
                return Set.of();
            }

            @Override
            public Map<String, Object> attributes() {
                return Map.of();
            }
        };
    }

}
