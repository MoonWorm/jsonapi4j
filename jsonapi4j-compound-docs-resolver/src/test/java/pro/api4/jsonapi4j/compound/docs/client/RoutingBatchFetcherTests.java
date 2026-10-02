package pro.api4.jsonapi4j.compound.docs.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsRequest;
import pro.api4.jsonapi4j.compound.docs.DomainSettings;
import pro.api4.jsonapi4j.compound.docs.cache.InMemoryCompoundDocsResourceCache;
import pro.api4.jsonapi4j.compound.docs.config.CompoundDocsResolverConfig;
import pro.api4.jsonapi4j.compound.docs.config.Deduplication;
import pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy;
import pro.api4.jsonapi4j.compound.docs.config.UnsupportedIncludeStrategy;
import pro.api4.jsonapi4j.compound.docs.exception.DomainResolutionException;
import pro.api4.jsonapi4j.compound.docs.json.JsonApiResponseParser;
import pro.api4.jsonapi4j.http.cache.CacheControlParser;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoutingBatchFetcherTests {

    private static final JsonApiResponseParser PARSER = new JsonApiResponseParser(new ObjectMapper());
    private static final CompoundDocsRequest REQUEST = new CompoundDocsRequest(
            "GET", List.of("placeOfBirth"), Map.of(), Map.of(), "/users/1", Map.of()
    );
    private static final BatchFetch<DomainSettings> OVER_HTTP = new BatchFetch<>(
            DomainSettings.overHttp(URI.create("http://geo.internal/jsonapi")), "countries", Set.of("US"), Set.of()
    );
    private static final BatchFetch<DomainSettings> IN_PROCESS = new BatchFetch<>(
            DomainSettings.inProcess(20), "countries", Set.of("US"), Set.of()
    );

    private final AtomicInteger overHttpFetches = new AtomicInteger();
    private final AtomicInteger inProcessFetches = new AtomicInteger();
    private final BatchFetcher<DomainSettings.OverHttp> overHttp = (batch, request) -> {
        overHttpFetches.incrementAndGet();
        return country(batch.ids());
    };
    private final BatchFetcher<DomainSettings.InProcess> inProcess = (batch, request) -> {
        inProcessFetches.incrementAndGet();
        return country(batch.ids());
    };
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Nested
    class Fetch {

        @Test
        void fetch_overHttpRoute_usesTheOverHttpFetcher() {
            RoutingBatchFetcher sut = new RoutingBatchFetcher(overHttp, inProcess);

            sut.fetch(OVER_HTTP, REQUEST);

            assertThat(overHttpFetches).hasValue(1);
            assertThat(inProcessFetches).hasValue(0);
        }

        @Test
        void fetch_inProcessRoute_usesTheInProcessFetcher() {
            RoutingBatchFetcher sut = new RoutingBatchFetcher(overHttp, inProcess);

            sut.fetch(IN_PROCESS, REQUEST);

            assertThat(inProcessFetches).hasValue(1);
            assertThat(overHttpFetches).hasValue(0);
        }

        @Test
        void fetch_inProcessRouteOfFetcherWithoutInProcessFetcher_throwsDomainResolutionException() {
            RoutingBatchFetcher sut = new RoutingBatchFetcher(overHttp);

            assertThat(sut.isInProcessFetcherConfigured()).isFalse();
            assertThatThrownBy(() -> sut.fetch(IN_PROCESS, REQUEST)).isInstanceOf(DomainResolutionException.class);
        }

    }

    @Nested
    class Compose {

        @Test
        void compose_withCache_cachesOnlyResourcesFetchedOverHttp() {
            RoutingBatchFetcher sut = RoutingBatchFetcher.compose(
                    overHttp, inProcess, new InMemoryCompoundDocsResourceCache(100), PARSER, executor, config()
            );

            sut.fetch(OVER_HTTP, REQUEST);
            sut.fetch(OVER_HTTP, REQUEST);
            sut.fetch(IN_PROCESS, REQUEST);
            sut.fetch(IN_PROCESS, REQUEST);

            assertThat(overHttpFetches).hasValue(1);
            assertThat(inProcessFetches).hasValue(2);
            assertThat(sut.isInProcessFetcherConfigured()).isTrue();
        }

    }

    private static FetchResult country(Set<String> ids) {
        return new FetchResult(
                ids.stream().map(id -> PARSER.parseResource("{\"type\":\"countries\",\"id\":\"" + id + "\"}")).toList(),
                CacheControlParser.parse("max-age=300")
        );
    }

    private static CompoundDocsResolverConfig config() {
        return new CompoundDocsResolverConfig(
                true, 3, UnsupportedIncludeStrategy.FAIL, 100, ErrorStrategy.IGNORE, List.of(), Set.of(),
                Deduplication.DATA_AND_INCLUDED, 1000, 1000, true, 100
        );
    }

}
