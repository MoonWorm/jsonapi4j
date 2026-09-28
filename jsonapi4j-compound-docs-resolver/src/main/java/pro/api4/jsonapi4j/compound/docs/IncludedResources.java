package pro.api4.jsonapi4j.compound.docs;

import pro.api4.jsonapi4j.compound.docs.config.Deduplication;
import pro.api4.jsonapi4j.processor.IdAndType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resources collected for the {@code included} member while compound docs are resolved, following the configured
 * {@link Deduplication}.
 *
 * <p>When deduplicating, a resource fetched again replaces its earlier copy - a later fetch is requested with a
 * superset of the earlier includes, so it carries at least the same linkage.
 */
final class IncludedResources {

    private final Deduplication deduplication;
    private final Map<IdAndType, String> primaryResourceJsons;
    // List<String> is used for values to support 'deduplication' modes that accepts duplicates
    private final Map<IdAndType, List<String>> includedJsons = new LinkedHashMap<>();
    private int size;

    /**
     * @param primaryResourceJsons the JSON of the primary data's resource objects, by identity - never repeated in
     *                             {@code included} under {@link Deduplication#DATA_AND_INCLUDED}, repeated from here
     *                             without a fetch under {@link Deduplication#INCLUDED_ONLY}
     */
    IncludedResources(Deduplication deduplication, Map<IdAndType, String> primaryResourceJsons) {
        this.deduplication = deduplication;
        this.primaryResourceJsons = primaryResourceJsons;
    }

    /**
     * Adds a fetched resource.
     */
    void add(IdAndType idAndType, String resourceJson) {
        if (deduplication == Deduplication.DATA_AND_INCLUDED && primaryResourceJsons.containsKey(idAndType)) {
            return;
        }
        List<String> jsons = includedJsons.computeIfAbsent(idAndType, k -> new ArrayList<>());
        if (deduplication != Deduplication.NONE) {
            size -= jsons.size();
            jsons.clear();
        }
        jsons.add(resourceJson);
        size++;
    }

    /**
     * Notes that an include path reached {@code idAndType}. A primary resource is then repeated in {@code included}
     * under {@link Deduplication#INCLUDED_ONLY}, taken from the primary data.
     */
    void reached(IdAndType idAndType) {
        if (deduplication == Deduplication.INCLUDED_ONLY
                && primaryResourceJsons.containsKey(idAndType)
                && !includedJsons.containsKey(idAndType)) {
            add(idAndType, primaryResourceJsons.get(idAndType));
        }
    }

    int size() {
        return size;
    }

    List<String> toList() {
        return includedJsons.values().stream().flatMap(List::stream).toList();
    }

}
