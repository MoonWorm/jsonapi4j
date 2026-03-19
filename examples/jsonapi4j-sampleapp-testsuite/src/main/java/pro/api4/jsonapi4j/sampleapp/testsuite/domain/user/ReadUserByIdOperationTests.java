package pro.api4.jsonapi4j.sampleapp.testsuite.domain.user;

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

    public ReadUserByIdOperationTests(String jsonApiRootPath,
                                      int appPort) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.appPort = appPort;
    }

    @Test
    public void test_readById() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "1")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/domain/user/single-user-byid-response.json")));
    }

    @Test
    public void test_readByIdWithAccessToSensitiveData() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "1")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/domain/user/single-user-byid-with-sensitive-data-response.json")));
    }

    @Test
    public void test_readByIdWithAllRelationships() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives", "citizenships", "placeOfBirth")
                .pathParam("userId", "1")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/domain/user/single-user-byid-response-with-relationships.json")));
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
