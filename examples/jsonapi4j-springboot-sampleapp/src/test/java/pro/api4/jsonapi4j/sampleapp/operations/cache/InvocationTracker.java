package pro.api4.jsonapi4j.sampleapp.operations.cache;

import org.springframework.stereotype.Component;
import pro.api4.jsonapi4j.sampleapp.testsuite.CacheCompoundDocsTests.DownstreamInvocations;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class InvocationTracker implements DownstreamInvocations {

    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    public int increment(String resourceType) {
        return counters.computeIfAbsent(resourceType, k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    @Override
    public int count(String resourceType) {
        AtomicInteger counter = counters.get(resourceType);
        return counter != null ? counter.get() : 0;
    }

    @Override
    public void reset() {
        counters.clear();
    }

}
