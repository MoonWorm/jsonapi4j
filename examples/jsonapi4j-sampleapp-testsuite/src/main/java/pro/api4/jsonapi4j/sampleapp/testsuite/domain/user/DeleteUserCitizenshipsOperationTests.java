package pro.api4.jsonapi4j.sampleapp.testsuite.domain.user;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public abstract class DeleteUserCitizenshipsOperationTests {

    private final String jsonApiRootPath;
    private final int appPort;

    public DeleteUserCitizenshipsOperationTests(String jsonApiRootPath,
                                                int appPort) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.appPort = appPort;
    }

    @Test
    public void test_deleteCitizenships() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "3")
                .body("""
                        {
                          "data": [
                            { "type": "countries", "id": "FI" }
                          ]
                        }
                        """)
                .delete("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/citizenships")
                .then()
                .statusCode(204);
    }

    @Test
    public void test_deleteCitizenships_validationError_invalidResourceType() {
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
                .delete("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/citizenships")
                .then()
                .statusCode(400)
                .body("errors[0].code", equalTo("GENERIC_REQUEST_ERROR"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].detail", equalTo("'wrong-type' value is not allowed, available values: [countries]"))
                .body("errors[0].source.parameter", equalTo("body -> data -> type"))
                .body("errors[0].id", notNullValue());
    }

    @Test
    public void test_deleteCitizenships_validationError_blankResourceId() {
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
                .delete("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/citizenships")
                .then()
                .statusCode(400)
                .body("errors[0].code", equalTo("GENERIC_REQUEST_ERROR"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].detail", equalTo("value can't be blank"))
                .body("errors[0].source.parameter", equalTo("body -> data[0] -> id"))
                .body("errors[0].id", notNullValue());
    }

    @Test
    public void test_deleteCitizenships_validationError_tooLongResourceId() {
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
                .delete("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/citizenships")
                .then()
                .statusCode(400)
                .body("errors[0].code", equalTo("GENERIC_REQUEST_ERROR"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].detail", equalTo("resource id length can't be more than 64"))
                .body("errors[0].source.parameter", equalTo("body -> data[0] -> id"))
                .body("errors[0].id", notNullValue());
    }

}
