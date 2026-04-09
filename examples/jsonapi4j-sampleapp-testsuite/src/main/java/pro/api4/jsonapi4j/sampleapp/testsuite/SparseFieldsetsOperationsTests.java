package pro.api4.jsonapi4j.sampleapp.testsuite;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;
import pro.api4.jsonapi4j.request.SparseFieldsetsAwareRequest;
import pro.api4.jsonapi4j.sampleapp.testsuite.util.ResourceUtil;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;

public abstract class SparseFieldsetsOperationsTests {

    private final String jsonApiRootPath;
    private final int serverPort;

    public SparseFieldsetsOperationsTests(String jsonApiRootPath,
                                          int serverPort) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.serverPort = serverPort;
    }

    @Test
    public void test_readByIdWithIncludes() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives", "placeOfBirth.currencies")
                .queryParam(SparseFieldsetsAwareRequest.getFieldsParam("users"), "email")
                .queryParam(SparseFieldsetsAwareRequest.getFieldsParam("countries"), "name")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/sf/single-user-sparse-fieldsets-response.json")));
    }

    @Test
    public void test_readMultipleWithIncludes() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives", "placeOfBirth.currencies")
                .queryParam(SparseFieldsetsAwareRequest.getFieldsParam("users"), "email")
                .queryParam(SparseFieldsetsAwareRequest.getFieldsParam("countries"), "name")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/sf/multiple-users-sparse-fieldsets-response.json")));
    }

    @Test
    public void test_readToOneRelationshipWithIncludes() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "placeOfBirth.currencies")
                .queryParam(SparseFieldsetsAwareRequest.getFieldsParam("countries"), "name")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}/relationships/placeOfBirth")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/sf/to-one-relationship-sparse-fieldsets-response.json")));
    }

    @Test
    public void test_readById_emptyFieldsParam_reductsAllAttributes() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(SparseFieldsetsAwareRequest.getFieldsParam("users"), "")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("data.id", equalTo("1"))
                .body("data.type", equalTo("users"))
                .body("data", not(hasKey("attributes")));
    }

    @Test
    public void test_readMultiple_emptyFieldsParam_reductsAllAttributes() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(SparseFieldsetsAwareRequest.getFieldsParam("users"), "")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("data[0]", not(hasKey("attributes")))
                .body("data[1]", not(hasKey("attributes")));
    }

    @Test
    public void test_readById_nonExistentField_reductsAllAttributes() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(SparseFieldsetsAwareRequest.getFieldsParam("users"), "nonExistentField")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("data.id", equalTo("1"))
                .body("data.type", equalTo("users"))
                .body("data", not(hasKey("attributes")));
    }

    @Test
    public void test_readMultiple_nonExistentField_reductsAllAttributes() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(SparseFieldsetsAwareRequest.getFieldsParam("users"), "nonExistentField")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("data[0]", not(hasKey("attributes")))
                .body("data[1]", not(hasKey("attributes")));
    }

    @Test
    public void test_readToManyRelationshipWithIncludes() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(SparseFieldsetsAwareRequest.getFieldsParam("countries"), "name")
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "citizenships.currencies")
                .pathParam("userId", "5")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}/relationships/citizenships")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/sf/to-many-relationship-sparse-fieldsets-response.json")));
    }

}
