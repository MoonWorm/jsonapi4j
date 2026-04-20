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
                .body("errors[0].code", equalTo("GENERIC_REQUEST_ERROR"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].detail", equalTo("resource type 'wrong-type' not supported, available resource types: [countries]"))
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
                .body("errors[0].code", equalTo("GENERIC_REQUEST_ERROR"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].detail", equalTo("resource id can't be blank"))
                .body("errors[0].id", notNullValue());
    }

}
