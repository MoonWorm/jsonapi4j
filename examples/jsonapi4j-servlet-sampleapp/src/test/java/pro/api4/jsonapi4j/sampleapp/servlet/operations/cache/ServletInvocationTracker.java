package pro.api4.jsonapi4j.sampleapp.servlet.operations.cache;

import pro.api4.jsonapi4j.sampleapp.testsuite.CacheCompoundDocsTests.DownstreamInvocations;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-resource-type invocation counter shared between the {@link CacheDownstreamServlet} (which increments it)
 * and {@link ServletCacheCompoundDocsTests} (which reads/resets it). A single {@link #INSTANCE} is used so the
 * servlet — instantiated by the embedded Jetty server — and the test see the same counters without a DI container.
 */
public class ServletInvocationTracker implements DownstreamInvocations {

    public static final ServletInvocationTracker INSTANCE = new ServletInvocationTracker();

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
