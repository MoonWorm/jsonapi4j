package pro.api4.jsonapi4j.compound.docs;

import java.net.URI;
import java.util.Objects;

/**
 * Per-domain settings used by the compound documents resolver when fetching included resources
 * from a downstream JSON:API service.
 *
 * <p>Carries the base {@link URI} for a resource type plus the maximum number of resource IDs
 * that can be requested in a single {@code filter[id]=...} batch. The latter exists because
 * downstream services typically enforce a hard cap on the number of values accepted by a single
 * filter parameter — when the resolver needs more IDs than that, it splits the request into
 * parallel chunks.
 *
 * @param url          base URL of the downstream domain (must not be null)
 * @param maxBatchSize maximum number of IDs per downstream HTTP request (must be positive)
 */
public record DomainSettings(URI url, int maxBatchSize) {

    /**
     * Default {@code filter[id]=...} batch size used when no per-domain override is provided.
     * Chosen as a safe lower bound — many JSON:API implementations cap filter values at 20–100.
     */
    public static final int DEFAULT_MAX_BATCH_SIZE = 20;

    public DomainSettings {
        Objects.requireNonNull(url, "url must not be null");
        if (maxBatchSize <= 0) {
            throw new IllegalArgumentException(
                    "maxBatchSize must be positive, got " + maxBatchSize
            );
        }
    }

    /**
     * Convenience factory for a {@link DomainSettings} with the {@link #DEFAULT_MAX_BATCH_SIZE}.
     */
    public static DomainSettings of(URI url) {
        return new DomainSettings(url, DEFAULT_MAX_BATCH_SIZE);
    }
}
