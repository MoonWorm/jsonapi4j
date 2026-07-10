package pro.api4.jsonapi4j.sampleapp.operations.cache;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import pro.api4.jsonapi4j.compound.docs.cache.CompoundDocsResourceCache;
import pro.api4.jsonapi4j.compound.docs.cache.InMemoryCompoundDocsResourceCache;
import pro.api4.jsonapi4j.sampleapp.CacheTestProfile;
import pro.api4.jsonapi4j.sampleapp.testsuite.CacheCompoundDocsTests;

/**
 * Quarkus binding of the shared {@link CacheCompoundDocsTests}. The CD-included types
 * ({@code countries}/{@code currencies}) are mapped to the in-app {@link QuarkusCacheTestResource}
 * downstream target so downstream invocations can be counted.
 *
 * <p>Unlike Spring's {@code @DirtiesContext}, {@code @QuarkusTest} reuses a single application across
 * all methods, so the CD resource cache (a {@code @Singleton}) is cleared before each test to give
 * every method a fresh cache.
 */
@QuarkusTest
@TestProfile(CacheTestProfile.class)
public class QuarkusCacheCompoundDocsTests extends CacheCompoundDocsTests {

    private final CompoundDocsResourceCache cache;

    public QuarkusCacheCompoundDocsTests(@ConfigProperty(name = "jsonapi4j.rootPath") String jsonApiRootPath,
                                         @ConfigProperty(name = "quarkus.http.port") int appPort,
                                         QuarkusInvocationTracker invocationTracker,
                                         CompoundDocsResourceCache cache) {
        super(jsonApiRootPath, appPort, invocationTracker);
        this.cache = cache;
    }

    @BeforeEach
    void clearCache() {
        // The cache bean is the in-memory implementation (@Singleton, not CDI-proxied), so the cast is safe.
        ((InMemoryCompoundDocsResourceCache) cache).clear();
    }

}
