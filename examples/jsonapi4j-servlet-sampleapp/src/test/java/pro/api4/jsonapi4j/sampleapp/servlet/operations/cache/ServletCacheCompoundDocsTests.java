package pro.api4.jsonapi4j.sampleapp.servlet.operations.cache;

import org.junit.jupiter.api.extension.ExtendWith;
import pro.api4.jsonapi4j.sampleapp.servlet.EmbeddedJettyCacheExtension;
import pro.api4.jsonapi4j.sampleapp.servlet.EmbeddedJettyExtension;
import pro.api4.jsonapi4j.sampleapp.testsuite.CacheCompoundDocsTests;

/**
 * Servlet binding of the shared {@link CacheCompoundDocsTests}. The CD-included types
 * ({@code countries}/{@code currencies}) are mapped to the in-app {@link CacheDownstreamServlet} downstream
 * target so downstream invocations can be counted. {@link EmbeddedJettyCacheExtension} restarts the embedded
 * Jetty server before each test, giving every method a fresh CD resource cache.
 */
@ExtendWith(EmbeddedJettyCacheExtension.class)
public class ServletCacheCompoundDocsTests extends CacheCompoundDocsTests {

    public ServletCacheCompoundDocsTests() {
        super(EmbeddedJettyExtension.ROOT_PATH, EmbeddedJettyExtension.PORT, ServletInvocationTracker.INSTANCE);
    }

}
