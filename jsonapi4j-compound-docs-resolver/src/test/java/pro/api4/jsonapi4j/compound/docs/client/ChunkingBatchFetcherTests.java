package pro.api4.jsonapi4j.compound.docs.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsRequest;
import pro.api4.jsonapi4j.compound.docs.DomainSettings;
import pro.api4.jsonapi4j.compound.docs.IncompleteReason;
import pro.api4.jsonapi4j.compound.docs.exception.DownstreamTimeoutException;
import pro.api4.jsonapi4j.compound.docs.json.JsonApiResponseParser;
import pro.api4.jsonapi4j.compound.docs.json.ParsedResource;
import pro.api4.jsonapi4j.http.cache.CacheControlParser;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChunkingBatchFetcherTests {

    private static final JsonApiResponseParser PARSER = new JsonApiResponseParser(new ObjectMapper());
    private static final CompoundDocsRequest REQUEST = new CompoundDocsRequest(
            "GET", List.of("placeOfBirth"), Map.of(), Map.of(), "/users/1", Map.of()
    );

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final List<Set<String>> fetchedChunks = Collections.synchronizedList(new ArrayList<>());

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Nested
    class Fetch {

        @Test
        void fetch_noIds_returnsEmptyWithoutFetching() {
            ChunkingBatchFetcher<DomainSettings.OverHttp> sut = new ChunkingBatchFetcher<>(echoing("max-age=60"), executor);

            FetchResult result = sut.fetch(batch(Set.of(), 2), REQUEST);

            assertThat(result.resources()).isEmpty();
            assertThat(fetchedChunks).isEmpty();
        }

        @Test
        void fetch_idsWithinBatchSize_fetchesOnce() {
            ChunkingBatchFetcher<DomainSettings.OverHttp> sut = new ChunkingBatchFetcher<>(echoing("max-age=60"), executor);

            sut.fetch(batch(Set.of("A", "B"), 2), REQUEST);

            assertThat(fetchedChunks).hasSize(1);
        }

        @Test
        void fetch_idsAboveBatchSize_fetchesChunksAndMergesThem() {
            ChunkingBatchFetcher<DomainSettings.OverHttp> sut = new ChunkingBatchFetcher<>(echoing("max-age=60"), executor);

            FetchResult result = sut.fetch(batch(Set.of("A", "B", "C", "D", "E"), 2), REQUEST);

            assertThat(fetchedChunks).hasSize(3).allSatisfy(chunk -> assertThat(chunk).hasSizeLessThanOrEqualTo(2));
            assertThat(result.resources()).extracting(r -> r.idAndType().getId())
                    .containsExactlyInAnyOrder("A", "B", "C", "D", "E");
            assertThat(result.directives().getMaxAge()).isEqualTo(60L);
        }

        @Test
        void fetch_oneChunkIgnoredFailure_isMostRestrictiveAndIncomplete() {
            ChunkingBatchFetcher<DomainSettings.OverHttp> sut = new ChunkingBatchFetcher<>(
                    (chunk, request) -> chunk.ids().contains("A")
                            ? FetchResult.ignoredFailure()
                            : echoing("max-age=60").fetch(chunk, request),
                    executor
            );

            FetchResult result = sut.fetch(batch(new LinkedHashSet<>(List.of("A", "B", "C")), 1), REQUEST);

            assertThat(result.resources()).extracting(r -> r.idAndType().getId()).containsExactlyInAnyOrder("B", "C");
            assertThat(result.directives().isNoStore()).isTrue();
            assertThat(result.incompleteReason()).isEqualTo(IncompleteReason.FETCH_FAILED);
        }

        @Test
        void fetch_chunkThrows_rethrowsItUnwrapped() {
            ChunkingBatchFetcher<DomainSettings.OverHttp> sut = new ChunkingBatchFetcher<>(
                    (chunk, request) -> {
                        throw new DownstreamTimeoutException("slow", null);
                    },
                    executor
            );

            assertThatThrownBy(() -> sut.fetch(batch(Set.of("A", "B", "C"), 1), REQUEST))
                    .isExactlyInstanceOf(DownstreamTimeoutException.class);
        }

    }

    @Nested
    class Chunk {

        @Test
        void chunk_idsAboveChunkSize_splitsThemInOrder() {
            List<Set<String>> chunks = ChunkingBatchFetcher.chunk(new LinkedHashSet<>(List.of("A", "B", "C", "D", "E")), 2);

            assertThat(chunks).containsExactly(Set.of("A", "B"), Set.of("C", "D"), Set.of("E"));
        }

        @Test
        void chunk_noIds_returnsNoChunks() {
            assertThat(ChunkingBatchFetcher.chunk(Set.of(), 2)).isEmpty();
        }

    }

    private BatchFetcher<DomainSettings.OverHttp> echoing(String cacheControl) {
        return (chunk, request) -> {
            fetchedChunks.add(chunk.ids());
            List<ParsedResource> resources = chunk.ids().stream()
                    .map(id -> PARSER.parseResource("{\"type\":\"countries\",\"id\":\"" + id + "\"}"))
                    .toList();
            return new FetchResult(resources, CacheControlParser.parse(cacheControl));
        };
    }

    private static BatchFetch<DomainSettings.OverHttp> batch(Set<String> ids, int maxBatchSize) {
        return new BatchFetch<>(
                DomainSettings.overHttp(URI.create("http://geo.internal/jsonapi"), maxBatchSize), "countries", ids, Set.of()
        );
    }

}
