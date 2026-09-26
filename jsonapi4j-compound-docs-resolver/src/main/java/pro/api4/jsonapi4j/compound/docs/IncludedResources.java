package pro.api4.jsonapi4j.compound.docs;

import pro.api4.jsonapi4j.processor.IdAndType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resources collected for the {@code included} member while compound docs are resolved.
 *
 * <p>When deduplicating, a resource fetched again replaces its earlier copy - a later fetch is requested with a
 * superset of the earlier includes, so it carries at least the same linkage. Otherwise every fetched copy is kept.
 */
final class IncludedResources {

    private final boolean deduplicate;
    private final Map<IdAndType, List<String>> resourcesByIdAndType = new LinkedHashMap<>();
    private int size;

    IncludedResources(boolean deduplicate) {
        this.deduplicate = deduplicate;
    }

    void add(IdAndType idAndType, String resourceJson) {
        List<String> resources = resourcesByIdAndType.computeIfAbsent(idAndType, k -> new ArrayList<>());
        if (deduplicate) {
            size -= resources.size();
            resources.clear();
        }
        resources.add(resourceJson);
        size++;
    }

    int size() {
        return size;
    }

    List<String> toList() {
        return resourcesByIdAndType.values().stream().flatMap(List::stream).toList();
    }

}
