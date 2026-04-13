package pro.api4.jsonapi4j.sampleapp.operations.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("cacheTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SpringCacheCompoundDocsTests {

    private final String jsonApiRootPath;
    private final int serverPort;

    @Autowired
    private InvocationTracker invocationTracker;

    public SpringCacheCompoundDocsTests(@Value("${jsonapi4j.rootPath}") String jsonApiRootPath,
                                        @LocalServerPort int serverPort) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.serverPort = serverPort;
    }

    @BeforeEach
    void resetTracker() {
        invocationTracker.resetAll();
    }

    @Test
    void test_compoundDocsResolution_cacheHitReducesDownstreamCalls() {
        String url = "http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}";

        // First request — should call the test controller (cache miss)
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "citizenships")
                .pathParam("userId", "1")
                .get(url)
                .then()
                .statusCode(200)
                .body("data.id", equalTo("1"))
                // user 1's citizenships: {NO, FI, US}
                .body("included", hasSize(3))
                .body("included.findAll { it.type == 'countries' }.id", containsInAnyOrder("NO", "FI", "US"));

        int firstCallCountCountries = invocationTracker.getCount("countries");
        assertThat(firstCallCountCountries).as("First request should hit the downstream controller").isEqualTo(1);

        // Second request — same includes, should be served from cache
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "citizenships")
                .pathParam("userId", "1")
                .get(url)
                .then()
                .statusCode(200)
                .body("data.id", equalTo("1"))
                // user 1's citizenships: {NO, FI, US}
                .body("included", hasSize(3))
                .body("included.findAll { it.type == 'countries' }.id", containsInAnyOrder("NO", "FI", "US"));

        int secondCallCountCountries = invocationTracker.getCount("countries");
        assertThat(secondCallCountCountries).as("Second request should be served from cache — no downstream calls").isEqualTo(1);
    }

    @Test
    void test_compoundDocsResolution_cacheControlHeaderAggregated() {
        // countries returns max-age=300, currencies returns max-age=60
        // aggregated Cache-Control should use the minimum: max-age=60
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "citizenships.currencies")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .body("data.id", equalTo("1"))
                // user 1's citizenships {NO, FI, US} + their currencies {NOK, EUR, USD} = 6 included
                .body("included", hasSize(6))
                .body("included.findAll { it.type == 'countries' }.id", containsInAnyOrder("NO", "FI", "US"))
                .body("included.findAll { it.type == 'currencies' }.id", containsInAnyOrder("NOK", "EUR", "USD"))
                .header("Cache-Control", "max-age=60");
    }

    @Test
    void test_compoundDocsResolution_nonCacheableDirectivePreventsCache() {
        String url = "http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}";

        // First request with no-store — should NOT be cached
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "citizenships")
                .queryParam("x-cache-control", "no-store")
                .pathParam("userId", "1")
                .get(url)
                .then()
                .statusCode(200)
                .body("data.id", equalTo("1"))
                // user 1's citizenships: {NO, FI, US}
                .body("included", hasSize(3))
                .body("included.findAll { it.type == 'countries' }.id", containsInAnyOrder("NO", "FI", "US"));

        int firstCallCount = invocationTracker.getCount("countries");
        assertThat(firstCallCount).isEqualTo(1);

        // Second request — should still hit downstream because no-store prevented caching
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "citizenships")
                .queryParam("x-cache-control", "no-store")
                .pathParam("userId", "1")
                .get(url)
                .then()
                .statusCode(200)
                .body("data.id", equalTo("1"))
                // user 1's citizenships: {NO, FI, US}
                .body("included", hasSize(3))
                .body("included.findAll { it.type == 'countries' }.id", containsInAnyOrder("NO", "FI", "US"));

        int secondCallCount = invocationTracker.getCount("countries");
        assertThat(secondCallCount).as("no-store should prevent caching — downstream still called").isEqualTo(2);
    }

}
