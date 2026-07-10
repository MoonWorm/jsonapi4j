package pro.api4.jsonapi4j.sampleapp.testsuite;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * Black-box tests for compound-docs resource caching: a cache hit must avoid re-fetching downstream resources, a
 * non-cacheable directive ({@code no-store}) must prevent caching, and {@code Cache-Control} directives from multiple
 * downstream responses must aggregate to the most conservative value.
 *
 * <p>Everything except the downstream invocation counts is exercised over HTTP. The counts come from a host-provided
 * downstream target (a test controller mapped as the {@code countries}/{@code currencies} domain) via
 * {@link DownstreamInvocations}. Subclasses must run under a profile that enables the compound-docs plugin with the
 * cache on, maps those types to the downstream target, and gives each test method a fresh cache.
 */
public abstract class CacheCompoundDocsTests {

    /** Access to the downstream test target's per-resource-type invocation counters (host-provided). */
    public interface DownstreamInvocations {

        /** @return how many times the downstream target was called for {@code resourceType} since the last reset */
        int count(String resourceType);

        /** Clears all counters. */
        void reset();
    }

    private final String jsonApiRootPath;
    private final int appPort;
    private final DownstreamInvocations downstream;

    protected CacheCompoundDocsTests(String jsonApiRootPath, int appPort, DownstreamInvocations downstream) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.appPort = appPort;
        this.downstream = downstream;
    }

    private String url(String path) {
        return "http://localhost:" + appPort + jsonApiRootPath + path;
    }

    @BeforeEach
    void resetDownstream() {
        downstream.reset();
    }

    @Test
    public void test_compoundDocsResolution_cacheHitReducesDownstreamCalls() {
        // First request — cache miss, hits the downstream target
        readUser1Citizenships();
        assertThat(downstream.count("countries"))
                .as("first request should hit the downstream target")
                .isEqualTo(1);

        // Second request — same includes, served from cache (no further downstream call)
        readUser1Citizenships();
        assertThat(downstream.count("countries"))
                .as("second request should be served from cache — no downstream call")
                .isEqualTo(1);
    }

    @Test
    public void test_compoundDocsResolution_nonCacheableDirectivePreventsCache() {
        // First request with no-store — must not be cached
        readUser1CitizenshipsNoStore();
        assertThat(downstream.count("countries")).isEqualTo(1);

        // Second request — still hits downstream because no-store prevented caching
        readUser1CitizenshipsNoStore();
        assertThat(downstream.count("countries"))
                .as("no-store should prevent caching — downstream still called")
                .isEqualTo(2);
    }

    @Test
    public void test_compoundDocsResolution_cacheControlHeaderAggregated() {
        // countries returns max-age=300, currencies returns max-age=60; the aggregated
        // Cache-Control must use the most conservative (minimum) value: max-age=60
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "citizenships.currencies")
                .pathParam("userId", "1")
                .get(url("/users/{userId}"))
                .then()
                .statusCode(200)
                .body("data.id", equalTo("1"))
                // user 1's citizenships {NO, FI, US} + their currencies {NOK, EUR, USD} = 6 included
                .body("included", hasSize(6))
                .body("included.findAll { it.type == 'countries' }.id", containsInAnyOrder("NO", "FI", "US"))
                .body("included.findAll { it.type == 'currencies' }.id", containsInAnyOrder("NOK", "EUR", "USD"))
                .header("Cache-Control", "max-age=60");
    }

    private void readUser1Citizenships() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "citizenships")
                .pathParam("userId", "1")
                .get(url("/users/{userId}"))
                .then()
                .statusCode(200)
                .body("data.id", equalTo("1"))
                // user 1's citizenships: {NO, FI, US}
                .body("included", hasSize(3))
                .body("included.findAll { it.type == 'countries' }.id", containsInAnyOrder("NO", "FI", "US"));
    }

    private void readUser1CitizenshipsNoStore() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "citizenships")
                .queryParam("x-cache-control", "no-store")
                .pathParam("userId", "1")
                .get(url("/users/{userId}"))
                .then()
                .statusCode(200)
                .body("data.id", equalTo("1"))
                .body("included", hasSize(3))
                .body("included.findAll { it.type == 'countries' }.id", containsInAnyOrder("NO", "FI", "US"));
    }

}
