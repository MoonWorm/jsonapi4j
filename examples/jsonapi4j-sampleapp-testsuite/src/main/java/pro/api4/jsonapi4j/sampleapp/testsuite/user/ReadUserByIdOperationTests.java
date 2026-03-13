package pro.api4.jsonapi4j.sampleapp.testsuite.user;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;
import pro.api4.jsonapi4j.sampleapp.testsuite.util.ResourceUtil;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public abstract class ReadUserByIdOperationTests {

    private final String jsonApiRootPath;
    private final int appPort;

    private final String defaultAccessTierHeaderName;
    private final String defaultScopesHeaderName;
    private final String defaultUserIdHeaderName;

    public ReadUserByIdOperationTests(String jsonApiRootPath,
                                      int appPort,
                                      String defaultAccessTierHeaderName,
                                      String defaultScopesHeaderName,
                                      String defaultUserIdHeaderName) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.appPort = appPort;
        this.defaultAccessTierHeaderName = defaultAccessTierHeaderName;
        this.defaultScopesHeaderName = defaultScopesHeaderName;
        this.defaultUserIdHeaderName = defaultUserIdHeaderName;
    }

    @Test
    public void test_readById() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultUserIdHeaderName, "2")
                .pathParam("userId", "1")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/user/jsonapi/single-user-byid-response.json")));
    }

    @Test
    public void test_readByIdWithAccessToSensitiveData() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultScopesHeaderName, "users.sensitive.read")
                .header(defaultUserIdHeaderName, "1")
                .pathParam("userId", "1")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/user/jsonapi/single-user-byid-with-sensitive-data-response.json")));
    }

    @Test
    public void test_readByIdWithAllRelationships() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultUserIdHeaderName, "2")
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives", "citizenships", "placeOfBirth")
                .pathParam("userId", "1")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/user/jsonapi/single-user-byid-response-with-relationships.json")));
    }

    @Test
    public void test_readById_validationError() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "100")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(404)
                .body("errors[0].code", equalTo("NOT_FOUND"))
                .body("errors[0].status", equalTo("404"))
                .body("errors[0].detail", equalTo("'ResourceType(type=users)' resource of a given id (100) is not found"))
                .body("errors[0].id", notNullValue());
    }

}
