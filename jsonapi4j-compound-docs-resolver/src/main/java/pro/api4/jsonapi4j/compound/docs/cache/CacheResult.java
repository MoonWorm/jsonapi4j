package pro.api4.jsonapi4j.compound.docs.cache;

import java.util.Objects;

/**
 * Represents a cache hit result containing the cached resource JSON and the
 * remaining time-to-live at the time of retrieval.
 *
 * <p>The {@link #getRemainingTtlSeconds()} value is used by cache-control aggregation
 * (Epic 5) to compute the most restrictive {@code Cache-Control} header for the
 * final compound document response.
 */
public class CacheResult {

    private final String resourceJson;
    private final long remainingTtlSeconds;

    /**
     * @param resourceJson        cached resource JSON, must not be null
     * @param remainingTtlSeconds remaining TTL in seconds, must be >= 0
     */
    public CacheResult(String resourceJson, long remainingTtlSeconds) {
        this.resourceJson = Objects.requireNonNull(resourceJson, "resourceJson must not be null");
        if (remainingTtlSeconds < 0) {
            throw new IllegalArgumentException("remainingTtlSeconds must be >= 0, got: " + remainingTtlSeconds);
        }
        this.remainingTtlSeconds = remainingTtlSeconds;
    }

    public String getResourceJson() {
        return resourceJson;
    }

    public long getRemainingTtlSeconds() {
        return remainingTtlSeconds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CacheResult that = (CacheResult) o;
        return remainingTtlSeconds == that.remainingTtlSeconds
                && Objects.equals(resourceJson, that.resourceJson);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceJson, remainingTtlSeconds);
    }

    @Override
    public String toString() {
        return "CacheResult{" +
                "remainingTtlSeconds=" + remainingTtlSeconds +
                ", resourceJson='" + (resourceJson.length() > 50
                    ? resourceJson.substring(0, 50) + "...(truncated)"
                    : resourceJson) + '\'' +
                '}';
    }

}
