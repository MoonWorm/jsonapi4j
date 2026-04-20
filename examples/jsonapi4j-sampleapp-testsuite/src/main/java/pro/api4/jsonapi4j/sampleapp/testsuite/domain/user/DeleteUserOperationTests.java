package pro.api4.jsonapi4j.sampleapp.testsuite.domain.user;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public abstract class DeleteUserOperationTests {

    private final String jsonApiRootPath;
    private final int appPort;

    public DeleteUserOperationTests(String jsonApiRootPath,
                                    int appPort) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.appPort = appPort;
    }

    @Test
    public void test_deleteUser() {
        // delete user
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "5")
                .delete("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(204);

        // verify user no longer exists
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "5")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(404)
                .body("errors[0].code", equalTo("NOT_FOUND"))
                .body("errors[0].status", equalTo("404"))
                .body("errors[0].id", notNullValue());
    }

    @Test
    public void test_deleteUser_nonExistentUser() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "999")
                .delete("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(404);
    }

}
