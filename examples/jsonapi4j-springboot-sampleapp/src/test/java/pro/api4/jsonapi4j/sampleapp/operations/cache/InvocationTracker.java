package pro.api4.jsonapi4j.sampleapp.operations.cache;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class InvocationTracker {

    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    public int increment(String resourceType) {
        return counters.computeIfAbsent(resourceType, k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    public int getCount(String resourceType) {
        AtomicInteger counter = counters.get(resourceType);
        return counter != null ? counter.get() : 0;
    }

    public void resetAll() {
        counters.clear();
    }

}
