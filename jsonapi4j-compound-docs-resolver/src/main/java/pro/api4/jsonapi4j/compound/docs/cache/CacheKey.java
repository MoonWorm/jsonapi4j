package pro.api4.jsonapi4j.compound.docs.cache;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.processor.IdAndType;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Composite cache key for individual JSON:API resources during compound document resolution.
 *
 * <p>The key includes the resource's {@code type} and {@code id}, downstream {@code include} parameter,
 * sparse fieldset ({@code fields[type]}) and propagated custom query parameters to ensure that resources fetched with
 * different query parameters are cached separately (as the response may differ).
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
    private final Map<String, List<String>> queryParams;

    /**
     * @param idAndType the resource's {@code type} and {@code id}, both must not be null
     * @param includes  relationship names for downstream include, may be null (treated as empty)
     * @param fields    sparse fieldset field names, may be null (treated as empty)
     */
    public CacheKey(IdAndType idAndType,
                    Set<String> includes,
                    Set<String> fields) {
        this(idAndType, includes, fields, null);
    }

    /**
     * @param idAndType   the resource's {@code type} and {@code id}, both must not be null
     * @param includes    relationship names for downstream include, may be null (treated as empty)
     * @param fields      sparse fieldset field names, may be null (treated as empty)
     * @param queryParams custom query parameters propagated downstream, may be null (treated as empty); compared
     *                    regardless of parameter order, but in value order, since a parameter's values keep theirs
     */
    public CacheKey(IdAndType idAndType,
                    Set<String> includes,
                    Set<String> fields,
                    Map<String, List<String>> queryParams) {
        Validate.notNull(idAndType, "idAndType must not be null");
        Validate.notNull(idAndType.getType(), "resource type must not be null");
        Validate.notNull(idAndType.getType().getType(), "resource type must not be null");
        Validate.notNull(idAndType.getId(), "resource id must not be null");
        this.idAndType = idAndType;
        this.includes = normalizeSet(includes);
        this.fields = normalizeSet(fields);
        this.queryParams = normalizeMap(queryParams);
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

    private static Map<String, List<String>> normalizeMap(Map<String, List<String>> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> sorted = new TreeMap<>();
        input.forEach((key, values) -> sorted.put(key, values == null ? List.of() : List.copyOf(values)));
        return Collections.unmodifiableMap(sorted);
    }

    private static Set<String> normalizeSet(Set<String> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new TreeSet<>(input));
    }

}
