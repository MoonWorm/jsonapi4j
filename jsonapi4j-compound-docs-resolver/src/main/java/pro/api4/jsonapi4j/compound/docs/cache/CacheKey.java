package pro.api4.jsonapi4j.compound.docs.cache;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Composite cache key for individual JSON:API resources during compound document resolution.
 *
 * <p>The key includes the resource type, resource ID, downstream {@code include} parameter,
 * and sparse fieldset ({@code fields[type]}) to ensure that resources fetched with different
 * query parameters are cached separately (as the response shape differs).
 *
 * <p>Sets are stored in sorted, immutable form for deterministic {@link #equals(Object)}
 * and {@link #hashCode()} behavior.
 */
public class CacheKey {

    private final String resourceType;
    private final String resourceId;
    private final Set<String> includes;
    private final Set<String> fields;

    /**
     * @param resourceType JSON:API resource type, must not be null
     * @param resourceId   resource ID, must not be null
     * @param includes     relationship names for downstream include, may be null (treated as empty)
     * @param fields       sparse fieldset field names, may be null (treated as empty)
     */
    public CacheKey(String resourceType, String resourceId,
                    Set<String> includes, Set<String> fields) {
        this.resourceType = Objects.requireNonNull(resourceType, "resourceType must not be null");
        this.resourceId = Objects.requireNonNull(resourceId, "resourceId must not be null");
        this.includes = normalizeSet(includes);
        this.fields = normalizeSet(fields);
    }

    /**
     * Creates a cache key with no includes and no fields.
     */
    public static CacheKey of(String resourceType, String resourceId) {
        return new CacheKey(resourceType, resourceId, null, null);
    }

    /**
     * Creates a cache key with includes but no fields.
     */
    public static CacheKey of(String resourceType, String resourceId, Set<String> includes) {
        return new CacheKey(resourceType, resourceId, includes, null);
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public Set<String> getIncludes() {
        return includes;
    }

    public Set<String> getFields() {
        return fields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CacheKey cacheKey = (CacheKey) o;
        return Objects.equals(resourceType, cacheKey.resourceType)
                && Objects.equals(resourceId, cacheKey.resourceId)
                && Objects.equals(includes, cacheKey.includes)
                && Objects.equals(fields, cacheKey.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceType, resourceId, includes, fields);
    }

    @Override
    public String toString() {
        return "CacheKey{" +
                "resourceType='" + resourceType + '\'' +
                ", resourceId='" + resourceId + '\'' +
                ", includes=" + includes +
                ", fields=" + fields +
                '}';
    }

    private static Set<String> normalizeSet(Set<String> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new TreeSet<>(input));
    }

}
