package pro.api4.jsonapi4j.sampleapp.testsuite;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.equalTo;

/**
 * Black-box tests for the JSON:API entry-point document served at {@code GET {rootPath}}. Runs with the default
 * configuration, where the entry point is enabled.
 */
public abstract class EntryPointTests {

    private final String jsonApiRootPath;
    private final int appPort;

    public EntryPointTests(String jsonApiRootPath, int appPort) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.appPort = appPort;
    }

    private String url(String path) {
        return "http://localhost:" + appPort + jsonApiRootPath + path;
    }

    @Test
    public void test_readEntryPoint_linksEveryCollection() {
        given()
                .get(url(""))
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("links.self", equalTo("/"))
                .body("links.countries", equalTo("/countries"))
                .body("links.currencies", equalTo("/currencies"))
                .body("links.users", equalTo("/users"))
                .body("meta", anEmptyMap())
                .body("jsonapi.version", equalTo("1.1"));
    }

    @Test
    public void test_readEntryPoint_trailingSlash() {
        given()
                .get(url("/"))
                .then()
                .statusCode(200)
                .body("links.self", equalTo("/"));
    }

    @Test
    public void test_postEntryPoint_methodNotSupported() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .body("{}")
                .post(url(""))
                .then()
                .statusCode(405)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("errors[0].status", equalTo("405"));
    }

}
