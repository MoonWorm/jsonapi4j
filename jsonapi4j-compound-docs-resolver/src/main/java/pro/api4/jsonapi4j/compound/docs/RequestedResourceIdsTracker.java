package pro.api4.jsonapi4j.compound.docs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RequestedResourceIdsTracker {

    private final Map<String, Set<String>> requestedResourceIds;
    private final boolean deduplicateResources;

    public RequestedResourceIdsTracker(boolean deduplicateResources) {
        this.requestedResourceIds = new HashMap<>();
        this.deduplicateResources = deduplicateResources;
    }

    public Set<String> calculateNonRequested(String resourceType, Set<String> requestedIds) {
        if (!deduplicateResources) {
            return requestedIds;
        }
        if (this.requestedResourceIds.containsKey(resourceType)) {
            Set<String> alreadyRequestedIds = this.requestedResourceIds.get(resourceType);
            Set<String> result = requestedIds.stream()
                    .filter(id -> !alreadyRequestedIds.contains(id))
                    .collect(Collectors.toSet());
            this.requestedResourceIds.computeIfAbsent(resourceType, rt -> new HashSet<>()).addAll(result);
            return result;
        } else {
            this.requestedResourceIds.put(resourceType, new HashSet<>(requestedIds));
            return requestedIds;
        }
    }

}
