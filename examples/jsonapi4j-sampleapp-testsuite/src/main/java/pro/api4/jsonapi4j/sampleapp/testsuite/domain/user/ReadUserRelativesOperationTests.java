package pro.api4.jsonapi4j.sampleapp.testsuite.domain.user;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;

public abstract class ReadUserRelativesOperationTests {

    private final String jsonApiRootPath;
    private final int appPort;

    public ReadUserRelativesOperationTests(String jsonApiRootPath, int appPort) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.appPort = appPort;
    }

    @Test
    public void test_readRelativesRelationship() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "1")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/relatives")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                // top-level links
                .body("links.self", equalTo("/users/1/relationships/relatives"))
                .body("links", hasKey("related:users"))
                // user 1 has 2 relatives, paginated by 2
                .body("data", hasSize(2))
                .body("data.find { it.id == '2' }.type", equalTo("users"))
                .body("data.find { it.id == '2' }.meta.relationshipType", equalTo("HUSBAND"))
                .body("data.find { it.id == '3' }.type", equalTo("users"))
                .body("data.find { it.id == '3' }.meta.relationshipType", equalTo("BROTHER"));
    }

}
