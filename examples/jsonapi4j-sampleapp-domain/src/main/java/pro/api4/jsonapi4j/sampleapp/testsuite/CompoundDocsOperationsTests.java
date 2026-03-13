package pro.api4.jsonapi4j.sampleapp.testsuite;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;
import pro.api4.jsonapi4j.sampleapp.util.ResourceUtil;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;

public abstract class CompoundDocsOperationsTests {

    private final String jsonApiRootPath;
    private final int serverPort;

    private final String defaultAccessTierHeaderName;
    private final String defaultScopesHeaderName;
    private final String defaultUserIdHeaderName;

    public CompoundDocsOperationsTests(String jsonApiRootPath,
                                       int serverPort,
                                       String defaultAccessTierHeaderName,
                                       String defaultScopesHeaderName,
                                       String defaultUserIdHeaderName) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.serverPort = serverPort;
        this.defaultAccessTierHeaderName = defaultAccessTierHeaderName;
        this.defaultScopesHeaderName = defaultScopesHeaderName;
        this.defaultUserIdHeaderName = defaultUserIdHeaderName;
    }

    @Test
    public void test_readByIdWithIncludes() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultUserIdHeaderName, "2")
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives", "citizenships", "placeOfBirth.currencies")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/single-user-compound-docs-response.json")));
    }

    @Test
    public void test_readMultipleWithIncludes() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultUserIdHeaderName, "2")
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives", "citizenships", "placeOfBirth.currencies")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/multiple-users-compound-docs-response.json")));
    }

    @Test
    public void test_readToOneRelationshipWithIncludes() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "placeOfBirth.currencies")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}/relationships/placeOfBirth")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/to-one-relationship-compound-docs-response.json")));
    }

    @Test
    public void test_readToManyRelationshipWithIncludes() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultScopesHeaderName, "users.citizenships.read")
                .header(defaultUserIdHeaderName, "5")
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "citizenships.currencies")
                .pathParam("userId", "5")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}/relationships/citizenships")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/to-many-relationship-compound-docs-response.json")));
    }

}
