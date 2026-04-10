package pro.api4.jsonapi4j.sampleapp.testsuite.domain.user;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public abstract class ReadUserPlaceOfBirthOperationTests {

    private final String jsonApiRootPath;
    private final int appPort;

    public ReadUserPlaceOfBirthOperationTests(String jsonApiRootPath, int appPort) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.appPort = appPort;
    }

    @Test
    public void test_readPlaceOfBirthRelationship() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "1")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/placeOfBirth")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                // top-level links
                .body("links.self", equalTo("/users/1/relationships/placeOfBirth"))
                .body("links.related", equalTo("/countries/US"))
                // to-one data
                .body("data.id", equalTo("US"))
                .body("data.type", equalTo("countries"));
    }

}
