package pro.api4.jsonapi4j.compound.docs.cache;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.Validate;

/**
 * Represents a cache hit result containing the cached resource JSON and the
 * remaining time-to-live at the time of retrieval.
 *
 * <p>The {@link #getRemainingTtlSeconds()} value is used by cache-control aggregation
 * (Epic 5) to compute the most restrictive {@code Cache-Control} header for the
 * final compound document response.
 */
@Getter
@EqualsAndHashCode
@ToString
public class CacheResult {

    private final String resourceJson;
    private final long remainingTtlSeconds;

    /**
     * @param resourceJson        cached resource JSON, must not be null
     * @param remainingTtlSeconds remaining TTL in seconds, must be >= 0
     */
    public CacheResult(String resourceJson, long remainingTtlSeconds) {
        this.resourceJson = Validate.notNull(resourceJson, "resourceJson must not be null");
        if (remainingTtlSeconds < 0) {
            throw new IllegalArgumentException("remainingTtlSeconds must be >= 0, got: " + remainingTtlSeconds);
        }
        this.remainingTtlSeconds = remainingTtlSeconds;
    }

}
