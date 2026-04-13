package pro.api4.jsonapi4j.compound.docs.cache;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.Validate;

import java.util.Collections;
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
@Getter
@EqualsAndHashCode
@ToString
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
    public CacheKey(String resourceType,
                    String resourceId,
                    Set<String> includes,
                    Set<String> fields) {
        this.resourceType = Validate.notNull(resourceType, "resourceType must not be null");
        this.resourceId = Validate.notNull(resourceId, "resourceId must not be null");
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

    private static Set<String> normalizeSet(Set<String> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new TreeSet<>(input));
    }

}
