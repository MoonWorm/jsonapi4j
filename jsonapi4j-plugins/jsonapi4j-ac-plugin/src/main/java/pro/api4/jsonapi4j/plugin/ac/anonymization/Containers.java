package pro.api4.jsonapi4j.plugin.ac.anonymization;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Reading and rebuilding the containers an attributes graph can hold.
 *
 * <p>Map keys are not visited — a JSON member name is a string, so a requirement on a key type would have
 * nothing to hide. A denied map value takes its whole entry with it.
 */
final class Containers {

    private Containers() {

    }

    static boolean isContainerType(Class<?> type) {
        return Collection.class.isAssignableFrom(type)
                || Map.class.isAssignableFrom(type)
                || Optional.class.isAssignableFrom(type)
                || type.isArray()
                || type == Object.class;
    }

    static boolean isContainer(Object value) {
        return value instanceof Collection<?>
                || value instanceof Map<?, ?>
                || value instanceof Optional<?>
                || (value != null && value.getClass().isArray());
    }

    /**
     * Rebuilds a container from redacted elements, in the order they were read.
     *
     * <p>A standard container is produced first — an {@code ArrayList} for a {@code List}, and so on. That
     * only works when the field can hold it, so it is checked against the field's declared type: a field
     * declared {@code List} accepts an {@code ArrayList}, a field declared as a specific collection class
     * does not. In that case the container's own class is tried, which works when it can be constructed
     * empty and filled.
     *
     * @param original     the container that was read
     * @param elements     the elements to keep; denied ones have already been dropped
     * @param keys         for a map, the key of each kept element in the same order; ignored otherwise
     * @param declaredType the type the rebuilt container has to fit into, or {@code null} when unconstrained
     * @return the rebuilt container, or {@code null} when nothing assignable can be produced
     */
    static Object rebuild(Object original, List<Object> elements, List<Object> keys, Class<?> declaredType) {
        Object standard = rebuildAsStandardType(original, elements, keys);
        if (fits(standard, declaredType)) {
            return standard;
        }
        Object ownType = rebuildAsOwnType(original, elements);
        return fits(ownType, declaredType) ? ownType : null;
    }

    private static boolean fits(Object rebuilt, Class<?> declaredType) {
        return rebuilt != null && (declaredType == null || declaredType.isAssignableFrom(rebuilt.getClass()));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object rebuildAsStandardType(Object original, List<Object> elements, List<Object> keys) {
        if (original.getClass().isArray()) {
            Object rebuilt = Array.newInstance(original.getClass().getComponentType(), elements.size());
            for (int i = 0; i < elements.size(); i++) {
                Array.set(rebuilt, i, elements.get(i));
            }
            return rebuilt;
        }
        if (original instanceof Optional<?>) {
            return elements.isEmpty() ? Optional.empty() : Optional.ofNullable(elements.get(0));
        }
        if (original instanceof SortedMap<?, ?> sorted) {
            Map rebuilt = new TreeMap(sorted.comparator());
            putAll(rebuilt, keys, elements);
            return rebuilt;
        }
        if (original instanceof Map<?, ?>) {
            Map rebuilt = new LinkedHashMap();
            putAll(rebuilt, keys, elements);
            return rebuilt;
        }
        if (original instanceof SortedSet<?> sorted) {
            Collection rebuilt = new TreeSet(sorted.comparator());
            rebuilt.addAll(elements);
            return rebuilt;
        }
        if (original instanceof Set<?>) {
            return new LinkedHashSet<>(elements);
        }
        if (original instanceof List<?>) {
            return new ArrayList<>(elements);
        }
        return rebuildAsOwnType(original, elements);
    }

    /**
     * Last resort for a container the standard types cannot stand in for — a collection subclass with its
     * own behaviour. Works when it can be constructed empty and filled.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object rebuildAsOwnType(Object original, List<Object> elements) {
        if (!(original instanceof Collection<?>)) {
            return null;
        }
        try {
            Collection rebuilt = (Collection) original.getClass().getDeclaredConstructor().newInstance();
            rebuilt.addAll(elements);
            return rebuilt;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void putAll(Map rebuilt, List<Object> keys, List<Object> elements) {
        for (int i = 0; i < elements.size(); i++) {
            rebuilt.put(keys.get(i), elements.get(i));
        }
    }

}
