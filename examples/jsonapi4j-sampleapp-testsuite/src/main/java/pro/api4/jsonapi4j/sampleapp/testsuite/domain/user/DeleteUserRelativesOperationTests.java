package pro.api4.jsonapi4j.sampleapp.testsuite.domain.user;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import static io.restassured.RestAssured.given;

public abstract class DeleteUserRelativesOperationTests {

    private final String jsonApiRootPath;
    private final int appPort;

    public DeleteUserRelativesOperationTests(String jsonApiRootPath,
                                             int appPort) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.appPort = appPort;
    }

    @Test
    public void test_deleteRelatives() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "5")
                .body("""
                        {
                          "data": [
                            { "type": "users", "id": "4" }
                          ]
                        }
                        """)
                .delete("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/relatives")
                .then()
                .statusCode(204);
    }

}
