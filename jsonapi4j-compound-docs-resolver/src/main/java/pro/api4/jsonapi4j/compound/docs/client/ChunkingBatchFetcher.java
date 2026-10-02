package pro.api4.jsonapi4j.compound.docs.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsRequest;
import pro.api4.jsonapi4j.compound.docs.DomainSettings;
import pro.api4.jsonapi4j.compound.docs.json.ParsedResource;
import pro.api4.jsonapi4j.http.cache.CacheControlAggregator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * Splits a fetch into chunks of at most {@link DomainSettings#maxBatchSize()} ids - the most a single request may ask
 * for - and fetches them in parallel on the executor. The results are merged: all resources, the most restrictive
 * {@code Cache-Control} directives, and the reason any of them is incomplete.
 */
@Slf4j
public class ChunkingBatchFetcher<R extends DomainSettings> implements BatchFetcher<R> {

    private final BatchFetcher<R> delegate;
    private final Executor executor;

    public ChunkingBatchFetcher(BatchFetcher<R> delegate, Executor executor) {
        this.delegate = Validate.notNull(delegate, "delegate must not be null");
        this.executor = Validate.notNull(executor, "executor must not be null");
    }

    /**
     * A single chunk is fetched on the calling thread. A failure of any chunk propagates as thrown, unwrapped from the
     * {@link CompletionException}.
     */
    @Override
    public FetchResult fetch(BatchFetch<R> batch, CompoundDocsRequest originalRequest) {
        int maxBatchSize = batch.domainSettings().maxBatchSize();
        List<Set<String>> chunks = chunk(batch.ids() == null ? Set.of() : batch.ids(), maxBatchSize);
        if (chunks.isEmpty()) {
            return FetchResult.empty();
        }
        if (chunks.size() == 1) {
            return delegate.fetch(batch.withIds(chunks.get(0)), originalRequest);
        }

        log.debug("Chunked fetch for type '{}': {} ids → {} chunks (maxBatchSize={})",
                batch.resourceType(), batch.ids().size(), chunks.size(), maxBatchSize);

        List<CompletableFuture<FetchResult>> futures = chunks.stream()
                .map(chunk -> CompletableFuture.supplyAsync(
                        () -> delegate.fetch(batch.withIds(chunk), originalRequest),
                        executor
                ))
                .toList();
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw e;
        }
        return merge(futures.stream().map(CompletableFuture::join).toList());
    }

    private static FetchResult merge(List<FetchResult> chunkResults) {
        List<ParsedResource> resources = new ArrayList<>();
        CacheControlAggregator aggregator = new CacheControlAggregator();
        for (FetchResult chunkResult : chunkResults) {
            resources.addAll(chunkResult.resources());
            aggregator.add(chunkResult.directives());
        }
        return new FetchResult(
                resources,
                aggregator.getResult(),
                chunkResults.stream()
                        .map(FetchResult::incompleteReason)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null)
        );
    }

    /**
     * Splits a set of IDs into ordered chunks of at most {@code chunkSize}.
     * The last chunk may be smaller. Returns an empty list for an empty input.
     */
    static List<Set<String>> chunk(Set<String> ids, int chunkSize) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        if (ids.size() <= chunkSize) {
            return Collections.singletonList(ids);
        }
        List<String> ordered = new ArrayList<>(ids);
        List<Set<String>> chunks = new ArrayList<>((ordered.size() + chunkSize - 1) / chunkSize);
        for (int i = 0; i < ordered.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, ordered.size());
            chunks.add(new LinkedHashSet<>(ordered.subList(i, end)));
        }
        return chunks;
    }

}
