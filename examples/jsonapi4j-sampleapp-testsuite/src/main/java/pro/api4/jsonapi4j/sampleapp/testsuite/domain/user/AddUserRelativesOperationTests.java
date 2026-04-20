package pro.api4.jsonapi4j.sampleapp.testsuite.domain.user;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import static io.restassured.RestAssured.given;

public abstract class AddUserRelativesOperationTests {

    private final String jsonApiRootPath;
    private final int appPort;

    public AddUserRelativesOperationTests(String jsonApiRootPath,
                                          int appPort) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.appPort = appPort;
    }

    @Test
    public void test_addRelatives() {
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
                            }
                          ]
                        }
                        """)
                .post("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/relatives")
                .then()
                .statusCode(204);
    }

}
