package pro.api4.jsonapi4j.sampleapp.testsuite.domain.user;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public abstract class UpdateUserCitizenshipsOperationTests {

    private final String jsonApiRootPath;
    private final int appPort;

    public UpdateUserCitizenshipsOperationTests(String jsonApiRootPath,
                                                int appPort) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.appPort = appPort;
    }

    @Test
    public void test_updateCitizenships() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "3")
                .body("""
                        {
                          "data": [
                            { "type": "countries", "id": "NO" },
                            { "type": "countries", "id": "FI" }
                          ]
                        }
                        """)
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/citizenships")
                .then()
                .statusCode(204);
    }

    @Test
    public void test_updateCitizenships_emptyList() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "4")
                .body("""
                        {
                          "data": []
                        }
                        """)
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/citizenships")
                .then()
                .statusCode(204);
    }

    @Test
    public void test_updateCitizenships_validationError_invalidResourceType() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "1")
                .body("""
                        {
                          "data": [
                            { "type": "wrong-type", "id": "US" }
                          ]
                        }
                        """)
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/citizenships")
                .then()
                .statusCode(400)
                .body("errors[0].code", equalTo("INVALID_ENUM_VALUE"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].detail", equalTo("'wrong-type' value is not allowed, available values: [countries]"))
                .body("errors[0].source.pointer", equalTo("/data/0/type"))
                .body("errors[0].id", notNullValue());
    }

    @Test
    public void test_updateCitizenships_validationError_blankResourceId() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "1")
                .body("""
                        {
                          "data": [
                            { "type": "countries", "id": "" }
                          ]
                        }
                        """)
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/citizenships")
                .then()
                .statusCode(400)
                .body("errors[0].code", equalTo("VALUE_EMPTY"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].detail", equalTo("value can't be blank"))
                .body("errors[0].source.pointer", equalTo("/data/0/id"))
                .body("errors[0].id", notNullValue());
    }

    @Test
    public void test_updateCitizenships_validationError_tooLongResourceId() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "1")
                .body("""
                        {
                          "data": [
                            { "type": "countries", "id": "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" }
                          ]
                        }
                        """)
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/citizenships")
                .then()
                .statusCode(400)
                .body("errors[0].code", equalTo("GENERIC_REQUEST_ERROR"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].detail", equalTo("resource id length can't be more than 64"))
                .body("errors[0].source.pointer", equalTo("/data/0/id"))
                .body("errors[0].id", notNullValue());
    }

}
