package pro.api4.jsonapi4j.compound.docs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pro.api4.jsonapi4j.compound.docs.client.BatchFetch;
import pro.api4.jsonapi4j.compound.docs.client.BatchFetchResult;
import pro.api4.jsonapi4j.compound.docs.client.CachingCompoundDocsFetcher;
import pro.api4.jsonapi4j.compound.docs.config.CompoundDocsResolverConfig;
import pro.api4.jsonapi4j.compound.docs.config.Deduplication;
import pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy;
import pro.api4.jsonapi4j.compound.docs.config.UnsupportedIncludeStrategy;
import pro.api4.jsonapi4j.compound.docs.exception.DomainResolutionException;
import pro.api4.jsonapi4j.compound.docs.exception.ErrorJsonApiResponseException;
import pro.api4.jsonapi4j.compound.docs.exception.RejectedIncludesException;
import pro.api4.jsonapi4j.exception.UnsupportedIncludeException;
import pro.api4.jsonapi4j.compound.docs.json.JsonApiResponseWriter;
import pro.api4.jsonapi4j.http.cache.CacheControlDirectives;
import pro.api4.jsonapi4j.processor.IdAndType;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static pro.api4.jsonapi4j.compound.docs.IdAndTypeFixtures.idAndType;

@ExtendWith(MockitoExtension.class)
public class CompoundDocsResolverTests {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final URI BASE_URL = URI.create("http://localhost/jsonapi");
    private static final DomainSettingsResolver ROUTE_ALL = resourceType -> Optional.of(DomainSettings.of(BASE_URL));

    private static final IdAndType USER_1 = idAndType("users", "1");
    private static final IdAndType USER_2 = idAndType("users", "2");
    private static final IdAndType USER_3 = idAndType("users", "3");
    private static final IdAndType USER_4 = idAndType("users", "4");
    private static final IdAndType USER_5 = idAndType("users", "5");
    private static final IdAndType NORWAY = idAndType("countries", "NO");
    private static final IdAndType FINLAND = idAndType("countries", "FI");
    private static final IdAndType USA = idAndType("countries", "US");
    private static final IdAndType NOK = idAndType("currencies", "NOK");
    private static final IdAndType EUR = idAndType("currencies", "EUR");
    private static final IdAndType USD = idAndType("currencies", "USD");

    private static final Map<IdAndType, Map<String, List<IdAndType>>> DOWNSTREAM = Map.ofEntries(
            Map.entry(USER_1, Map.of(
                    "relatives", List.of(USER_2, USER_3),
                    "citizenships", List.of(NORWAY, USA),
                    "placeOfBirth", List.of(USA)
            )),
            Map.entry(USER_2, Map.of(
                    "relatives", List.of(USER_3),
                    "placeOfBirth", List.of(FINLAND)
            )),
            Map.entry(USER_3, Map.of(
                    "relatives", List.of(),
                    "placeOfBirth", List.of(NORWAY)
            )),
            Map.entry(USER_4, Map.of(
                    "relatives", List.of(USER_5),
                    "placeOfBirth", List.of(FINLAND)
            )),
            Map.entry(USER_5, Map.of(
                    "relatives", List.of(USER_4),
                    "placeOfBirth", List.of(USA)
            )),
            Map.entry(NORWAY, Map.of("currencies", List.of(NOK))),
            Map.entry(FINLAND, Map.of("currencies", List.of(EUR))),
            Map.entry(USA, Map.of("currencies", List.of(USD)))
    );

    private record FetchCall(String type, Set<String> ids, Set<String> includes) {
    }

    @Mock
    private CachingCompoundDocsFetcher fetcher;

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private final List<FetchCall> fetchCalls = Collections.synchronizedList(new ArrayList<>());

    @AfterEach
    public void tearDown() {
        executorService.shutdownNow();
    }

    @Nested
    class ResolveCompoundDocs {

        @Test
        public void resolveCompoundDocs_includePathsReachingSameType_includesOnlyResourcesOnRequestedPaths() throws Exception {
            CompoundDocsResolver sut = resolver(3, Deduplication.DATA_AND_INCLUDED);
            stubDownstream();

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    primaryDoc(USER_1, Set.of("citizenships", "placeOfBirth")),
                    request("/users/1", "citizenships", "placeOfBirth.currencies"),
                    ROUTE_ALL
            );

            assertThat(included(result)).containsExactlyInAnyOrder(NORWAY, USA, USD);
            assertThat(fetchCalls).containsExactlyInAnyOrder(
                    new FetchCall("countries", Set.of("NO", "US"), Set.of("currencies")),
                    new FetchCall("currencies", Set.of("USD"), Set.of())
            );
        }

        @Test
        public void resolveCompoundDocs_relationshipDoc_followsIncludesFromRelationshipName() throws Exception {
            CompoundDocsResolver sut = resolver(3, Deduplication.DATA_AND_INCLUDED);
            stubDownstream();

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    "{\"data\":[{\"type\":\"countries\",\"id\":\"NO\"},{\"type\":\"countries\",\"id\":\"US\"}]}",
                    request("/users/1/relationships/citizenships", "citizenships.currencies"),
                    ROUTE_ALL
            );

            assertThat(included(result)).containsExactlyInAnyOrder(NORWAY, USA, NOK, USD);
        }

        @Test
        public void resolveCompoundDocs_resourceReachedAgainNeedingMoreIncludes_refetchesItWithCombinedIncludes() throws Exception {
            CompoundDocsResolver sut = resolver(3, Deduplication.DATA_AND_INCLUDED);
            stubDownstream();

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    primaryDoc(USER_1, Set.of("relatives")),
                    request("/users/1", "relatives", "relatives.relatives.placeOfBirth"),
                    ROUTE_ALL
            );

            assertThat(included(result)).containsExactlyInAnyOrder(USER_2, USER_3, NORWAY);
            assertThat(fetchCalls).containsExactly(
                    new FetchCall("users", Set.of("2", "3"), Set.of("relatives")),
                    new FetchCall("users", Set.of("3"), Set.of("relatives", "placeOfBirth")),
                    new FetchCall("countries", Set.of("NO"), Set.of())
            );
        }

        @Test
        public void resolveCompoundDocs_resourceReachedAgainWithCoveredIncludes_doesNotRefetchIt() throws Exception {
            CompoundDocsResolver sut = resolver(3, Deduplication.DATA_AND_INCLUDED);
            stubDownstream();

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    primaryDoc(USER_1, Set.of("relatives")),
                    request("/users/1", "relatives.relatives"),
                    ROUTE_ALL
            );

            assertThat(included(result)).containsExactlyInAnyOrder(USER_2, USER_3);
            assertThat(fetchCalls).containsExactly(
                    new FetchCall("users", Set.of("2", "3"), Set.of("relatives"))
            );
        }

        @Test
        public void resolveCompoundDocs_linkageForRelationshipNotRequested_ignoresIt() throws Exception {
            CompoundDocsResolver sut = resolver(3, Deduplication.DATA_AND_INCLUDED);
            stubDownstream();

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    primaryDoc(USER_1, Set.of("relatives", "citizenships", "placeOfBirth")),
                    request("/users/1", "placeOfBirth"),
                    ROUTE_ALL
            );

            assertThat(included(result)).containsExactly(USA);
        }

    }

    @Nested
    class PrimaryResources {

        @Test
        public void resolveCompoundDocs_pathCyclesBackToPrimaryResource_neitherIncludesNorFetchesIt() throws Exception {
            CompoundDocsResolver sut = resolver(3, Deduplication.DATA_AND_INCLUDED);
            stubDownstream();

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    primaryDoc(USER_5, Set.of("relatives")),
                    request("/users/5", "relatives.relatives"),
                    ROUTE_ALL
            );

            assertThat(included(result)).containsExactly(USER_4);
            assertThat(fetchCalls).containsExactly(new FetchCall("users", Set.of("4"), Set.of("relatives")));
        }

        @Test
        public void resolveCompoundDocs_pathContinuesThroughPrimaryResource_fetchesItsLinkageButExcludesIt() throws Exception {
            CompoundDocsResolver sut = resolver(3, Deduplication.DATA_AND_INCLUDED);
            stubDownstream();

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    primaryDoc(USER_5, Set.of("relatives")),
                    request("/users/5", "relatives.relatives.placeOfBirth"),
                    ROUTE_ALL
            );

            assertThat(included(result)).containsExactlyInAnyOrder(USER_4, USA);
            assertThat(fetchCalls).containsExactly(
                    new FetchCall("users", Set.of("4"), Set.of("relatives")),
                    new FetchCall("users", Set.of("5"), Set.of("relatives", "placeOfBirth")),
                    new FetchCall("countries", Set.of("US"), Set.of())
            );
        }

        @Test
        public void resolveCompoundDocs_listWithPrimaryResourcesRelatedToEachOther_includesOnlyTheOthers() throws Exception {
            CompoundDocsResolver sut = resolver(3, Deduplication.DATA_AND_INCLUDED);
            stubDownstream();

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    primaryListDoc(Set.of("relatives"), USER_1, USER_2),
                    request("/users", "relatives"),
                    ROUTE_ALL
            );

            assertThat(included(result)).containsExactly(USER_3);
            assertThat(fetchCalls).containsExactly(new FetchCall("users", Set.of("3"), Set.of()));
        }

        @Test
        public void resolveCompoundDocs_includedOnly_repeatsReachedPrimaryResourcesWithoutFetchingThem() throws Exception {
            CompoundDocsResolver sut = resolver(3, Deduplication.INCLUDED_ONLY);
            stubDownstream();

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    primaryListDoc(Set.of("relatives"), USER_1, USER_2),
                    request("/users", "relatives"),
                    ROUTE_ALL
            );

            assertThat(included(result)).containsExactlyInAnyOrder(USER_2, USER_3);
            assertThat(fetchCalls).containsExactly(new FetchCall("users", Set.of("3"), Set.of()));
        }

        @Test
        public void resolveCompoundDocs_none_fetchesAndIncludesReachedPrimaryResources() throws Exception {
            CompoundDocsResolver sut = resolver(3, Deduplication.NONE);
            stubDownstream();

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    primaryDoc(USER_5, Set.of("relatives")),
                    request("/users/5", "relatives.relatives"),
                    ROUTE_ALL
            );

            assertThat(included(result)).containsExactlyInAnyOrder(USER_4, USER_5);
            assertThat(fetchCalls).containsExactly(
                    new FetchCall("users", Set.of("4"), Set.of("relatives")),
                    new FetchCall("users", Set.of("5"), Set.of("relatives"))
            );
        }

    }

    @Nested
    class ErrorStrategies {

        private static final DomainSettingsResolver COUNTRIES_ONLY = resourceType -> "countries".equals(resourceType)
                ? Optional.of(DomainSettings.of(BASE_URL))
                : Optional.empty();

        @Test
        public void resolveCompoundDocs_typeWithoutRouteUnderIgnore_skipsItAndForbidsStoring() throws Exception {
            CompoundDocsResolver sut = resolver(3, Deduplication.DATA_AND_INCLUDED, ErrorStrategy.IGNORE);
            stubDownstream();

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    primaryDoc(USER_1, Set.of("placeOfBirth")),
                    request("/users/1", "placeOfBirth.currencies"),
                    COUNTRIES_ONLY
            );

            assertThat(included(result)).containsExactly(USA);
            assertThat(result.cacheControlDirectives().isNoStore()).isTrue();
        }

        @Test
        public void resolveCompoundDocs_fetchForbiddingStorage_forbidsStoringTheDocument() throws Exception {
            CompoundDocsResolver sut = resolver(3, Deduplication.DATA_AND_INCLUDED, ErrorStrategy.IGNORE);
            when(fetcher.fetch(any(), any())).thenReturn(new BatchFetchResult(List.of(), CacheControlDirectives.NO_STORE, IncompleteReason.FETCH_FAILED));

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    primaryDoc(USER_1, Set.of("placeOfBirth")),
                    request("/users/1", "placeOfBirth"),
                    ROUTE_ALL
            );

            assertThat(result.cacheControlDirectives().isNoStore()).isTrue();
        }

        @Test
        public void resolveCompoundDocs_everythingResolved_allowsStoring() throws Exception {
            CompoundDocsResolver sut = resolver(3, Deduplication.DATA_AND_INCLUDED, ErrorStrategy.IGNORE);
            stubDownstream();

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    primaryDoc(USER_1, Set.of("placeOfBirth")),
                    request("/users/1", "placeOfBirth"),
                    ROUTE_ALL
            );

            assertThat(result.cacheControlDirectives()).isNull();
        }

        @Test
        public void resolveCompoundDocs_typeWithoutRouteUnderIgnore_listsNoRouteGapInMeta() throws Exception {
            CompoundDocsResolver sut = resolver(3, Deduplication.DATA_AND_INCLUDED, ErrorStrategy.IGNORE);
            stubDownstream();

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    primaryDoc(USER_1, Set.of("placeOfBirth")),
                    request("/users/1", "placeOfBirth.currencies"),
                    COUNTRIES_ONLY
            );

            assertThat(gaps(result)).containsExactly(IncludedGap.forType(IncompleteReason.NO_ROUTE, "currencies"));
        }

        @Test
        public void resolveCompoundDocs_nothingResolvedUnderIgnore_listsFetchFailedGapWithoutIncluded() throws Exception {
            CompoundDocsResolver sut = resolver(3, Deduplication.DATA_AND_INCLUDED, ErrorStrategy.IGNORE);
            when(fetcher.fetch(any(), any())).thenReturn(
                    new BatchFetchResult(List.of(), CacheControlDirectives.NO_STORE, IncompleteReason.FETCH_FAILED)
            );

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    primaryDoc(USER_1, Set.of("placeOfBirth")),
                    request("/users/1", "placeOfBirth"),
                    ROUTE_ALL
            );

            assertThat(gaps(result)).containsExactly(IncludedGap.forType(IncompleteReason.FETCH_FAILED, "countries"));
            assertThat(MAPPER.readTree(result.responseBody()).has("included")).isFalse();
        }

        @Test
        public void resolveCompoundDocs_documentWithOwnMeta_keepsItsMembers() throws Exception {
            CompoundDocsResolver sut = resolver(3, Deduplication.DATA_AND_INCLUDED, ErrorStrategy.IGNORE);
            stubDownstream();
            ObjectNode document = (ObjectNode) MAPPER.readTree(primaryDoc(USER_1, Set.of("placeOfBirth")));
            document.putObject("meta").put("total", 1);

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    MAPPER.writeValueAsString(document),
                    request("/users/1", "placeOfBirth.currencies"),
                    COUNTRIES_ONLY
            );

            assertThat(MAPPER.readTree(result.responseBody()).path("meta").path("total").asInt()).isEqualTo(1);
            assertThat(gaps(result)).containsExactly(IncludedGap.forType(IncompleteReason.NO_ROUTE, "currencies"));
        }

        @Test
        public void resolveCompoundDocs_everythingResolved_addsNoMeta() throws Exception {
            CompoundDocsResolver sut = resolver(3, Deduplication.DATA_AND_INCLUDED, ErrorStrategy.IGNORE);
            stubDownstream();

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    primaryDoc(USER_1, Set.of("placeOfBirth")),
                    request("/users/1", "placeOfBirth"),
                    ROUTE_ALL
            );

            assertThat(MAPPER.readTree(result.responseBody()).has("meta")).isFalse();
        }

        @Test
        public void resolveCompoundDocs_fetchFailsUnderFail_rethrowsItUnwrapped() {
            CompoundDocsResolver sut = resolver(3, Deduplication.DATA_AND_INCLUDED, ErrorStrategy.FAIL);
            when(fetcher.fetch(any(), any())).thenThrow(new ErrorJsonApiResponseException("boom"));

            assertThatThrownBy(() -> sut.resolveCompoundDocs(
                    primaryDoc(USER_1, Set.of("placeOfBirth")),
                    request("/users/1", "placeOfBirth"),
                    ROUTE_ALL
            )).isExactlyInstanceOf(ErrorJsonApiResponseException.class);
        }

    }

    @Nested
    class UnsupportedIncludes {

        @Test
        public void resolveCompoundDocs_includeDeeperThanMaxHopsUnderFail_throwsBeforeFetching() {
            CompoundDocsResolver sut = resolver(1, UnsupportedIncludeStrategy.FAIL, 100);

            assertThatThrownBy(() -> sut.resolveCompoundDocs(
                    primaryDoc(USER_1, Set.of("placeOfBirth")),
                    request("/users/1", "placeOfBirth.currencies"),
                    ROUTE_ALL
            )).isInstanceOf(UnsupportedIncludeException.class);
            assertThat(fetchCalls).isEmpty();
        }

        @Test
        public void resolveCompoundDocs_includeDeeperThanMaxHopsUnderIgnore_resolvesSupportedDepthAndListsPath() throws Exception {
            CompoundDocsResolver sut = resolver(1, UnsupportedIncludeStrategy.IGNORE, 100);
            stubDownstream();

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    primaryDoc(USER_1, Set.of("placeOfBirth")),
                    request("/users/1", "placeOfBirth.currencies"),
                    ROUTE_ALL
            );

            assertThat(included(result)).containsExactly(USA);
            assertThat(fetchCalls).containsExactly(new FetchCall("countries", Set.of("US"), Set.of()));
            assertThat(gaps(result)).containsExactly(
                    IncludedGap.forPath(IncompleteReason.UNSUPPORTED_INCLUDE, "placeOfBirth.currencies")
            );
        }

    }

    @Nested
    class RejectedByDownstream {

        @Test
        public void resolveCompoundDocs_unknownRelationshipUnderFail_throwsNamingTheFullPath() {
            CompoundDocsResolver sut = resolver(3, UnsupportedIncludeStrategy.FAIL, 100);
            stubDownstreamRejecting("countries", "economy");

            assertThatThrownBy(() -> sut.resolveCompoundDocs(
                    primaryDoc(USER_1, Set.of("placeOfBirth")),
                    request("/users/1", "placeOfBirth.economy"),
                    ROUTE_ALL
            )).isInstanceOfSatisfying(UnsupportedIncludeException.class, e -> assertThat(e.getUnsupportedIncludes())
                    .containsExactly(new UnsupportedIncludeException.UnsupportedInclude(
                            "placeOfBirth.economy", "Resource type 'countries' has no relationship 'economy'"
                    )));
        }

        @Test
        public void resolveCompoundDocs_unknownRelationshipUnderIgnore_refetchesWithoutItAndListsTheFullPath() throws Exception {
            CompoundDocsResolver sut = resolver(3, UnsupportedIncludeStrategy.IGNORE, 100);
            stubDownstreamRejecting("countries", "economy");

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    primaryDoc(USER_1, Set.of("placeOfBirth")),
                    request("/users/1", "placeOfBirth.economy", "placeOfBirth.currencies"),
                    ROUTE_ALL
            );

            assertThat(included(result)).containsExactlyInAnyOrder(USA, USD);
            assertThat(fetchCalls).containsExactly(
                    new FetchCall("countries", Set.of("US"), Set.of("currencies", "economy")),
                    new FetchCall("countries", Set.of("US"), Set.of("currencies")),
                    new FetchCall("currencies", Set.of("USD"), Set.of())
            );
            assertThat(gaps(result)).containsExactly(
                    IncludedGap.forPath(IncompleteReason.UNSUPPORTED_INCLUDE, "placeOfBirth.economy")
            );
        }

        @Test
        public void resolveCompoundDocs_includesRejectedByPrimaryServer_listsThemWithoutFetching() throws Exception {
            CompoundDocsResolver sut = resolver(3, UnsupportedIncludeStrategy.IGNORE, 100);

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    primaryDoc(USER_1, Set.of()),
                    new CompoundDocsRequest("GET", null, List.of("foo"), Map.of(), Map.of(), "/users/1", Map.of()),
                    ROUTE_ALL
            );

            assertThat(fetchCalls).isEmpty();
            assertThat(gaps(result)).containsExactly(IncludedGap.forPath(IncompleteReason.UNSUPPORTED_INCLUDE, "foo"));
        }

    }

    @Nested
    class MaxIncludedResources {

        @Test
        public void resolveCompoundDocs_reachedWithResourcesLeftToFetch_stopsAndListsTheirPath() throws Exception {
            CompoundDocsResolver sut = resolver(3, UnsupportedIncludeStrategy.FAIL, 2);
            stubDownstream();

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    primaryDoc(USER_1, Set.of("relatives")),
                    request("/users/1", "relatives.relatives.placeOfBirth"),
                    ROUTE_ALL
            );

            assertThat(included(result)).containsExactlyInAnyOrder(USER_2, USER_3);
            assertThat(fetchCalls).containsExactly(new FetchCall("users", Set.of("2", "3"), Set.of("relatives")));
            assertThat(gaps(result)).containsExactly(
                    IncludedGap.forPath(IncompleteReason.MAX_INCLUDED_RESOURCES, "relatives.relatives")
            );
        }

        @Test
        public void resolveCompoundDocs_reachedWithEverythingAlreadyFetched_completesWithoutGap() throws Exception {
            CompoundDocsResolver sut = resolver(3, UnsupportedIncludeStrategy.FAIL, 2);
            stubDownstream();

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    primaryDoc(USER_1, Set.of("relatives")),
                    request("/users/1", "relatives.relatives"),
                    ROUTE_ALL
            );

            assertThat(included(result)).containsExactlyInAnyOrder(USER_2, USER_3);
            assertThat(gaps(result)).isEmpty();
        }

        @Test
        public void resolveCompoundDocs_exceededByLastHop_keepsEverythingWithoutGap() throws Exception {
            CompoundDocsResolver sut = resolver(3, UnsupportedIncludeStrategy.FAIL, 1);
            stubDownstream();

            CompoundDocsResult result = sut.resolveCompoundDocs(
                    primaryDoc(USER_1, Set.of("relatives")),
                    request("/users/1", "relatives"),
                    ROUTE_ALL
            );

            assertThat(included(result)).containsExactlyInAnyOrder(USER_2, USER_3);
            assertThat(gaps(result)).isEmpty();
        }

    }

    @Nested
    class Routing {

        @Test
        public void resolveCompoundDocs_includedTypeWithoutRoute_throwsDomainResolutionException() {
            CompoundDocsResolver sut = resolver(3, Deduplication.DATA_AND_INCLUDED);
            stubDownstream();

            assertThatThrownBy(() -> sut.resolveCompoundDocs(
                    primaryDoc(USER_1, Set.of("placeOfBirth")),
                    request("/users/1", "placeOfBirth.currencies"),
                    resourceType -> "countries".equals(resourceType)
                            ? Optional.of(DomainSettings.of(BASE_URL))
                            : Optional.empty()
            ))
                    .isInstanceOf(DomainResolutionException.class)
                    .hasMessage("Resource type 'currencies' has no mapping");
        }

    }

    private CompoundDocsResolver resolver(int maxHops, Deduplication deduplication) {
        return resolver(maxHops, deduplication, ErrorStrategy.FAIL);
    }

    private CompoundDocsResolver resolver(int maxHops, Deduplication deduplication, ErrorStrategy errorStrategy) {
        return resolver(maxHops, UnsupportedIncludeStrategy.FAIL, 100, deduplication, errorStrategy);
    }

    private CompoundDocsResolver resolver(int maxHops,
                                          UnsupportedIncludeStrategy unsupportedIncludes,
                                          int maxIncludedResources) {
        return resolver(maxHops, unsupportedIncludes, maxIncludedResources, Deduplication.DATA_AND_INCLUDED, ErrorStrategy.FAIL);
    }

    private CompoundDocsResolver resolver(int maxHops,
                                          UnsupportedIncludeStrategy unsupportedIncludes,
                                          int maxIncludedResources,
                                          Deduplication deduplication,
                                          ErrorStrategy errorStrategy) {
        CompoundDocsResolverConfig config = new CompoundDocsResolverConfig(
                true, maxHops, unsupportedIncludes, maxIncludedResources, errorStrategy, List.of(), deduplication,
                1000, 1000, false, 1
        );
        return new CompoundDocsResolver(config, MAPPER, executorService, fetcher);
    }

    @SuppressWarnings("unchecked")
    private void stubDownstreamRejecting(String rejectingType, String unknownRelationship) {
        when(fetcher.fetch(any(), any())).thenAnswer(invocation -> {
            BatchFetch batch = invocation.getArgument(0);
            if (batch.resourceType().equals(rejectingType) && batch.includes().contains(unknownRelationship)) {
                fetchCalls.add(new FetchCall(batch.resourceType(), Set.copyOf(batch.ids()), Set.copyOf(batch.includes())));
                throw new RejectedIncludesException(rejectingType, Set.of(unknownRelationship));
            }
            return respond(batch);
        });
    }

    private void stubDownstream() {
        when(fetcher.fetch(any(), any())).thenAnswer(invocation -> respond(invocation.getArgument(0)));
    }

    private BatchFetchResult respond(BatchFetch batch) throws JsonProcessingException {
        String type = batch.resourceType();
        Set<String> ids = batch.ids();
        Set<String> includes = batch.includes();
        fetchCalls.add(new FetchCall(type, Set.copyOf(ids), Set.copyOf(includes)));
        List<String> resources = new ArrayList<>();
        for (String id : ids) {
            resources.add(MAPPER.writeValueAsString(resourceNode(idAndType(type, id), includes)));
        }
        return new BatchFetchResult(resources, null);
    }

    private static CompoundDocsRequest request(String relativePath, String... includes) {
        return new CompoundDocsRequest("GET", List.of(includes), Map.of(), Map.of(), relativePath, Map.of());
    }

    private static String primaryDoc(IdAndType resource, Set<String> linkedRelationships) throws JsonProcessingException {
        ObjectNode document = MAPPER.createObjectNode();
        document.set("data", resourceNode(resource, linkedRelationships));
        return MAPPER.writeValueAsString(document);
    }

    private static String primaryListDoc(Set<String> linkedRelationships, IdAndType... resources) throws JsonProcessingException {
        ObjectNode document = MAPPER.createObjectNode();
        ArrayNode data = document.putArray("data");
        for (IdAndType resource : resources) {
            data.add(resourceNode(resource, linkedRelationships));
        }
        return MAPPER.writeValueAsString(document);
    }

    private static ObjectNode resourceNode(IdAndType resource, Set<String> linkedRelationships) {
        ObjectNode node = MAPPER.createObjectNode()
                .put("type", resource.getType().getType())
                .put("id", resource.getId());
        ObjectNode relationships = node.putObject("relationships");
        DOWNSTREAM.getOrDefault(resource, Map.of()).forEach((relationshipName, linkage) -> {
            ObjectNode relationship = relationships.putObject(relationshipName);
            if (linkedRelationships.contains(relationshipName)) {
                ArrayNode data = relationship.putArray("data");
                linkage.forEach(target -> data.addObject().put("type", target.getType().getType()).put("id", target.getId()));
            }
        });
        return node;
    }

    private static List<IncludedGap> gaps(CompoundDocsResult result) throws JsonProcessingException {
        List<IncludedGap> gaps = new ArrayList<>();
        JsonNode gapsNode = MAPPER.readTree(result.responseBody())
                .path("meta")
                .path(JsonApiResponseWriter.INCLUDED_INCOMPLETE_META_FIELD);
        for (JsonNode node : gapsNode) {
            gaps.add(new IncludedGap(
                    IncompleteReason.valueOf(node.get("reason").asText()),
                    node.has("type") ? node.get("type").asText() : null,
                    node.has("path") ? node.get("path").asText() : null
            ));
        }
        return gaps;
    }

    private static List<IdAndType> included(CompoundDocsResult result) throws JsonProcessingException {
        List<IdAndType> included = new ArrayList<>();
        for (JsonNode node : MAPPER.readTree(result.responseBody()).path("included")) {
            included.add(idAndType(node.get("type").asText(), node.get("id").asText()));
        }
        return included;
    }

}
