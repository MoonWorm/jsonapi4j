package pro.api4.jsonapi4j.sampleapp.testsuite.domain.user;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public abstract class UpdateUserPlaceOfBirthOperationTests {

    private final String jsonApiRootPath;
    private final int appPort;

    public UpdateUserPlaceOfBirthOperationTests(String jsonApiRootPath,
                                                int appPort) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.appPort = appPort;
    }

    @Test
    public void test_updatePlaceOfBirth() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "2")
                .body("""                                                                                                                                                               
                        {
                          "data": { "type": "countries", "id": "NO" }
                        }
                        """)
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/placeOfBirth")
                .then()
                .statusCode(204);
    }

    @Test
    public void test_updatePlaceOfBirth_nullData_deletesPlaceOfBirth() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "1")
                .body("""
                        {
                          "data": null
                        }
                        """)
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/placeOfBirth")
                .then()
                .statusCode(204);
    }

    @Test
    public void test_updatePlaceOfBirth_validationError_invalidResourceType() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "1")
                .body("""
                        {
                          "data": { "type": "wrong-type", "id": "US" }
                        }
                        """)
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/placeOfBirth")
                .then()
                .statusCode(400)
                .body("errors[0].code", equalTo("GENERIC_REQUEST_ERROR"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].detail", equalTo("resource type 'wrong-type' not supported, available resource types: [countries]"))
                .body("errors[0].id", notNullValue());
    }

    @Test
    public void test_updatePlaceOfBirth_validationError_blankResourceId() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "1")
                .body("""
                        {
                          "data": { "type": "countries", "id": "" }
                        }
                        """)
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/placeOfBirth")
                .then()
                .statusCode(400)
                .body("errors[0].code", equalTo("GENERIC_REQUEST_ERROR"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].detail", equalTo("resource id can't be blank"))
                .body("errors[0].id", notNullValue());
    }

}
