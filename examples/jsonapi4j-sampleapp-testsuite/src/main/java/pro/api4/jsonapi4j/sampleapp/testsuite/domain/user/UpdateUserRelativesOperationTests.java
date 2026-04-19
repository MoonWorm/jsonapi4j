package pro.api4.jsonapi4j.sampleapp.testsuite.domain.user;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public abstract class UpdateUserRelativesOperationTests {

    private final String jsonApiRootPath;
    private final int appPort;

    public UpdateUserRelativesOperationTests(String jsonApiRootPath,
                                             int appPort) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.appPort = appPort;
    }

    @Test
    public void test_updateRelatives() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "3")
                .body("""
                        {
                          "data": [
                            {
                              "type": "users",
                              "id": "1",
                              "meta": { "relationshipType": "BROTHER" }
                            },
                            {
                              "type": "users",
                              "id": "2",
                              "meta": { "relationshipType": "WIFE" }
                            }
                          ]
                        }
                        """)
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/relatives")
                .then()
                .statusCode(204);
    }

    @Test
    public void test_updateRelatives_emptyList() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "4")
                .body("""
                        {
                          "data": []
                        }
                        """)
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/relatives")
                .then()
                .statusCode(204);
    }

    @Test
    public void test_updateRelatives_validationError_invalidResourceType() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "1")
                .body("""
                        {
                          "data": [
                            { "type": "wrong-type", "id": "2", "meta": { "relationshipType": "BROTHER" } }
                          ]
                        }
                        """)
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/relatives")
                .then()
                .statusCode(400)
                .body("errors[0].code", equalTo("GENERIC_REQUEST_ERROR"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].detail", equalTo("resource type 'wrong-type' not supported, available resource types: [users]"))
                .body("errors[0].id", notNullValue());
    }

    @Test
    public void test_updateRelatives_validationError_blankResourceId() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "1")
                .body("""
                        {
                          "data": [
                            { "type": "users", "id": "", "meta": { "relationshipType": "BROTHER" } }
                          ]
                        }
                        """)
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/relatives")
                .then()
                .statusCode(400)
                .body("errors[0].code", equalTo("GENERIC_REQUEST_ERROR"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].detail", equalTo("resource id can't be blank"))
                .body("errors[0].id", notNullValue());
    }

    @Test
    public void test_updateRelatives_validationError_nonExistentUser() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "1")
                .body("""
                        {
                          "data": [
                            { "type": "users", "id": "999", "meta": { "relationshipType": "BROTHER" } }
                          ]
                        }
                        """)
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/relatives")
                .then()
                .statusCode(404)
                .body("errors[0].code", equalTo("NOT_FOUND"))
                .body("errors[0].status", equalTo("404"))
                .body("errors[0].id", notNullValue());
    }

    @Test
    public void test_updateRelatives_validationError_invalidRelationshipMetaType() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "1")
                .body("""
                        {
                          "data": [
                            { "type": "users", "id": "2", "meta": { "relationshipType": "INVALID_TYPE" } }
                          ]
                        }
                        """)
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/relatives")
                .then()
                .statusCode(400)
                .body("errors[0].code", equalTo("GENERIC_REQUEST_ERROR"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].detail", equalTo("Meta 'RelationshipType' object only accepts string values: HUSBAND, WIFE, SON, DAUGHTER, MOTHER, FATHER, BROTHER"))
                .body("errors[0].id", notNullValue());
    }

}
