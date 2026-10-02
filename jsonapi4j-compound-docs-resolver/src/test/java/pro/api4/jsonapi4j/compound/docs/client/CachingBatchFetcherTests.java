package pro.api4.jsonapi4j.compound.docs.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsRequest;
import pro.api4.jsonapi4j.compound.docs.DomainSettings;
import pro.api4.jsonapi4j.compound.docs.IncompleteReason;
import pro.api4.jsonapi4j.compound.docs.cache.CacheKey;
import pro.api4.jsonapi4j.compound.docs.cache.InMemoryCompoundDocsResourceCache;
import pro.api4.jsonapi4j.compound.docs.config.Propagation;
import pro.api4.jsonapi4j.compound.docs.json.JsonApiResponseParser;
import pro.api4.jsonapi4j.compound.docs.json.ParsedResource;
import pro.api4.jsonapi4j.http.cache.CacheControlParser;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static pro.api4.jsonapi4j.compound.docs.IdAndTypeFixtures.idAndType;

class CachingBatchFetcherTests {

    private static final JsonApiResponseParser PARSER = new JsonApiResponseParser(new ObjectMapper());
    private static final CompoundDocsRequest REQUEST = new CompoundDocsRequest(
            "GET", List.of("placeOfBirth"), Map.of("countries", List.of("name")), Map.of(), "/users/1",
            Map.of("locale", List.of("de"))
    );

    private final InMemoryCompoundDocsResourceCache cache = new InMemoryCompoundDocsResourceCache(100);
    private final List<Set<String>> fetchedIds = new ArrayList<>();
    private String cacheControl = "max-age=300";
    private IncompleteReason incompleteReason;

    private final CachingBatchFetcher<DomainSettings.OverHttp> sut = new CachingBatchFetcher<>(
            this::fetch, cache, PARSER, List.of(Propagation.FIELDS, Propagation.CUSTOM_QUERY_PARAMS)
    );

    @Nested
    class Fetch {

        @Test
        void fetch_noneCached_fetchesThemAndStoresThem() {
            FetchResult result = sut.fetch(batch("NO", "US"), REQUEST);

            assertThat(ids(result)).containsExactlyInAnyOrder("NO", "US");
            assertThat(fetchedIds).containsExactly(Set.of("NO", "US"));
            assertThat(result.directives().getMaxAge()).isEqualTo(300L);
            assertThat(cache.get(key("NO", REQUEST))).isPresent();
        }

        @Test
        void fetch_allCached_servesThemWithoutFetching() {
            sut.fetch(batch("NO", "US"), REQUEST);
            fetchedIds.clear();

            FetchResult result = sut.fetch(batch("NO", "US"), REQUEST);

            assertThat(ids(result)).containsExactlyInAnyOrder("NO", "US");
            assertThat(fetchedIds).isEmpty();
            assertThat(result.directives().getMaxAge()).isPositive().isLessThanOrEqualTo(300L);
        }

        @Test
        void fetch_someCached_fetchesOnlyTheMissesInOneFetch() {
            sut.fetch(batch("NO"), REQUEST);
            fetchedIds.clear();

            FetchResult result = sut.fetch(batch("NO", "US", "FI"), REQUEST);

            assertThat(ids(result)).containsExactlyInAnyOrder("NO", "US", "FI");
            assertThat(fetchedIds).containsExactly(Set.of("US", "FI"));
        }

        @Test
        void fetch_responseNotCacheable_storesNothing() {
            cacheControl = "no-store";

            sut.fetch(batch("NO"), REQUEST);

            assertThat(cache.get(key("NO", REQUEST))).isEmpty();
        }

        @Test
        void fetch_incomplete_keepsTheReason() {
            incompleteReason = IncompleteReason.FETCH_FAILED;

            assertThat(sut.fetch(batch("NO"), REQUEST).incompleteReason()).isEqualTo(IncompleteReason.FETCH_FAILED);
        }

    }

    @Nested
    class CacheKeys {

        @Test
        void fetch_differentPropagatedQueryParams_doesNotServeTheCachedResource() {
            sut.fetch(batch("NO"), REQUEST);
            fetchedIds.clear();
            CompoundDocsRequest otherLocale = new CompoundDocsRequest(
                    "GET", List.of("placeOfBirth"), Map.of("countries", List.of("name")), Map.of(), "/users/1",
                    Map.of("locale", List.of("en"))
            );

            sut.fetch(batch("NO"), otherLocale);

            assertThat(fetchedIds).containsExactly(Set.of("NO"));
        }

        @Test
        void fetch_nothingPropagated_cacheKeyIgnoresFieldsAndQueryParams() {
            CachingBatchFetcher<DomainSettings.OverHttp> withoutPropagation =
                    new CachingBatchFetcher<>(CachingBatchFetcherTests.this::fetch, cache, PARSER, List.of());

            withoutPropagation.fetch(batch("NO"), REQUEST);

            assertThat(cache.get(CacheKey.of(idAndType("countries", "NO"), Set.of()))).isPresent();
        }

    }

    private FetchResult fetch(BatchFetch<DomainSettings.OverHttp> batch, CompoundDocsRequest request) {
        fetchedIds.add(batch.ids());
        List<ParsedResource> resources = batch.ids().stream()
                .map(id -> PARSER.parseResource("{\"type\":\"countries\",\"id\":\"" + id + "\"}"))
                .toList();
        return new FetchResult(resources, CacheControlParser.parse(cacheControl), incompleteReason);
    }

    private static BatchFetch<DomainSettings.OverHttp> batch(String... ids) {
        return new BatchFetch<>(
                DomainSettings.overHttp(URI.create("http://geo.internal/jsonapi")), "countries", Set.of(ids), Set.of()
        );
    }

    private static CacheKey key(String id, CompoundDocsRequest request) {
        return new CacheKey(idAndType("countries", id), Set.of(), Set.of("name"), request.getCustomQueryParams());
    }

    private static List<String> ids(FetchResult result) {
        return result.resources().stream().map(resource -> resource.idAndType().getId()).toList();
    }

}
