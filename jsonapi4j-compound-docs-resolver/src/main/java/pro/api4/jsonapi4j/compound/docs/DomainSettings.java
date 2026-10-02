package pro.api4.jsonapi4j.compound.docs;

import java.net.URI;
import java.util.Objects;

/**
 * Where the compound documents resolver fetches included resources of a type from, and how: {@link OverHttp} from a
 * downstream JSON:API service, or {@link InProcess} from the app itself. Created through
 * {@link #overHttp(URI, int, boolean)} and {@link #inProcess(int)}.
 *
 * <p>Either way it carries the maximum number of resource IDs requested in a single {@code filter[id]=...} batch:
 * services typically cap the number of values a filter accepts - the app itself validates it too - so when the resolver
 * needs more IDs than that, it splits the fetch into parallel chunks.
 */
public sealed interface DomainSettings permits DomainSettings.OverHttp, DomainSettings.InProcess {

    /**
     * Default {@code filter[id]=...} batch size used when no per-domain override is provided.
     * Chosen as a safe lower bound — many JSON:API implementations cap filter values at 20–100.
     */
    int DEFAULT_MAX_BATCH_SIZE = 20;

    /**
     * @return the maximum number of IDs per fetch
     */
    int maxBatchSize();

    /**
     * A downstream service at {@code url}, not trusted with the client's credentials, fetched in batches of
     * {@link #DEFAULT_MAX_BATCH_SIZE}.
     */
    static OverHttp overHttp(URI url) {
        return overHttp(url, DEFAULT_MAX_BATCH_SIZE, false);
    }

    /**
     * A downstream service at {@code url}, not trusted with the client's credentials.
     */
    static OverHttp overHttp(URI url, int maxBatchSize) {
        return overHttp(url, maxBatchSize, false);
    }

    /**
     * @param url                  the base URL of the downstream service
     * @param maxBatchSize         the maximum number of IDs per request
     * @param propagateCredentials whether the client's credential headers are sent to the service
     */
    static OverHttp overHttp(URI url, int maxBatchSize, boolean propagateCredentials) {
        return new OverHttp(url, maxBatchSize, propagateCredentials);
    }

    /**
     * Resources of a type the app serves itself, fetched in-process - by the in-process
     * {@link pro.api4.jsonapi4j.compound.docs.client.BatchFetcher} the resolver is created with. They are fetched as
     * the client's principal, so there are no credentials to propagate.
     */
    static InProcess inProcess(int maxBatchSize) {
        return new InProcess(maxBatchSize);
    }

    /**
     * Resources fetched over HTTP from a downstream JSON:API service.
     *
     * @param url                  the base URL of the downstream service
     * @param maxBatchSize         the maximum number of IDs per request (must be positive)
     * @param propagateCredentials whether the client's credential headers are sent to the service
     */
    record OverHttp(URI url, int maxBatchSize, boolean propagateCredentials) implements DomainSettings {

        public OverHttp {
            Objects.requireNonNull(url, "url must not be null");
            requirePositive(maxBatchSize);
        }

    }

    /**
     * Resources of a type the app serves itself, fetched in-process.
     *
     * @param maxBatchSize the maximum number of IDs per read (must be positive)
     */
    record InProcess(int maxBatchSize) implements DomainSettings {

        public InProcess {
            requirePositive(maxBatchSize);
        }

    }

    private static void requirePositive(int maxBatchSize) {
        if (maxBatchSize <= 0) {
            throw new IllegalArgumentException("maxBatchSize must be positive, got " + maxBatchSize);
        }
    }

}
