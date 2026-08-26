package pro.api4.jsonapi4j.sampleapp.testsuite.domain.user;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import static io.restassured.RestAssured.given;
import static pro.api4.jsonapi4j.sampleapp.testsuite.SampleUsers.createUser;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

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
        String userId = createUser(jsonApiRootPath, appPort);

        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", userId)
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
    public void test_updatePlaceOfBirth_nullData_clearsTheRelationshipAndKeepsTheUser() {
        String userId = createUser(jsonApiRootPath, appPort, """
                { "placeOfBirth": { "data": { "type": "countries", "id": "US" } } }""");

        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", userId)
                .body("""
                        {
                          "data": null
                        }
                        """)
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/placeOfBirth")
                .then()
                .statusCode(204);

        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", userId)
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .body("data.id", equalTo(userId));

        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", userId)
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/placeOfBirth")
                .then()
                .statusCode(200)
                .body("data", nullValue());
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
                .body("errors[0].code", equalTo("INVALID_ENUM_VALUE"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].detail", equalTo("'wrong-type' value is not allowed, available values: [countries]"))
                .body("errors[0].source.pointer", equalTo("/data/type"))
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
                .body("errors[0].code", equalTo("VALUE_EMPTY"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].detail", equalTo("value can't be blank"))
                .body("errors[0].source.pointer", equalTo("/data/id"))
                .body("errors[0].id", notNullValue());
    }

    // same here

}
