package pro.api4.jsonapi4j.sampleapp.testsuite;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;

/**
 * Compound documents with {@code jsonapi4j.cd.unsupportedIncludes: IGNORE} and {@code maxHops: 2}: an unsupported
 * include path is resolved as far as supported and listed in {@code meta.includedIncomplete}, instead of failing the
 * request.
 */
public abstract class UnsupportedIncludesIgnoredTests {

    private final String jsonApiRootPath;
    private final int serverPort;

    public UnsupportedIncludesIgnoredTests(String jsonApiRootPath,
                                           int serverPort) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.serverPort = serverPort;
    }

    @Test
    public void test_readById_unknownRelationshipFirstInPath_servesTheRestAndListsIt() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives,pets")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("data.id", equalTo("1"))
                .body("included.id", containsInAnyOrder("2", "3"))
                .body("meta.includedIncomplete", hasSize(1))
                .body("meta.includedIncomplete[0].reason", equalTo("UNSUPPORTED_INCLUDE"))
                .body("meta.includedIncomplete[0].path", equalTo("pets"));
    }

    @Test
    public void test_readById_onlyUnknownRelationships_servesPrimaryDataAndListsThem() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "pets")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .body("data.id", equalTo("1"))
                .body("$", not(hasKey("included")))
                .body("meta.includedIncomplete.path", containsInAnyOrder("pets"));
    }

    @Test
    public void test_readById_unknownRelationshipInTheMiddleOfPath_resolvesUpToItAndListsTheFullPath() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "placeOfBirth.economy,placeOfBirth.currencies")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .body("included.findAll { it.type == 'countries' }.id", containsInAnyOrder("US"))
                .body("included.findAll { it.type == 'currencies' }.id", containsInAnyOrder("USD"))
                .body("meta.includedIncomplete", hasSize(1))
                .body("meta.includedIncomplete[0].reason", equalTo("UNSUPPORTED_INCLUDE"))
                .body("meta.includedIncomplete[0].path", equalTo("placeOfBirth.economy"));
    }

    @Test
    public void test_readById_includeDeeperThanMaxHops_resolvesSupportedDepthAndListsIt() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives.relatives.relatives")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .body("included.findAll { it.type == 'users' }.id", containsInAnyOrder("2", "3", "4"))
                .body("meta.includedIncomplete", hasSize(1))
                .body("meta.includedIncomplete[0].reason", equalTo("UNSUPPORTED_INCLUDE"))
                .body("meta.includedIncomplete[0].path", equalTo("relatives.relatives.relatives"));
    }

}
