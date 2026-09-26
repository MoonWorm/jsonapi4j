package pro.api4.jsonapi4j.compound.docs.cache;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.processor.IdAndType;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Composite cache key for individual JSON:API resources during compound document resolution.
 *
 * <p>The key includes the resource's {@code type} and {@code id}, downstream {@code include} parameter,
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

    private final IdAndType idAndType;
    private final Set<String> includes;
    private final Set<String> fields;

    /**
     * @param idAndType the resource's {@code type} and {@code id}, both must not be null
     * @param includes  relationship names for downstream include, may be null (treated as empty)
     * @param fields    sparse fieldset field names, may be null (treated as empty)
     */
    public CacheKey(IdAndType idAndType,
                    Set<String> includes,
                    Set<String> fields) {
        Validate.notNull(idAndType, "idAndType must not be null");
        Validate.notNull(idAndType.getType(), "resource type must not be null");
        Validate.notNull(idAndType.getType().getType(), "resource type must not be null");
        Validate.notNull(idAndType.getId(), "resource id must not be null");
        this.idAndType = idAndType;
        this.includes = normalizeSet(includes);
        this.fields = normalizeSet(fields);
    }

    /**
     * Creates a cache key with no includes and no fields.
     */
    public static CacheKey of(IdAndType idAndType) {
        return new CacheKey(idAndType, null, null);
    }

    /**
     * Creates a cache key with includes but no fields.
     */
    public static CacheKey of(IdAndType idAndType, Set<String> includes) {
        return new CacheKey(idAndType, includes, null);
    }

    /**
     * @return JSON:API resource type
     */
    public String getResourceType() {
        return idAndType.getType().getType();
    }

    /**
     * @return resource ID
     */
    public String getResourceId() {
        return idAndType.getId();
    }

    private static Set<String> normalizeSet(Set<String> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new TreeSet<>(input));
    }

}
