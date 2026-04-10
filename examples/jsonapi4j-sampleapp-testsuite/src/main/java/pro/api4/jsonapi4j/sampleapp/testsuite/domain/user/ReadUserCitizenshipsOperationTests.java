package pro.api4.jsonapi4j.sampleapp.testsuite.domain.user;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;

public abstract class ReadUserCitizenshipsOperationTests {

    private final String jsonApiRootPath;
    private final int appPort;

    public ReadUserCitizenshipsOperationTests(String jsonApiRootPath,
                                              int appPort) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.appPort = appPort;
    }

    @Test
    public void test_readCitizenshipsRelationship() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "5")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/citizenships")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                // top-level links
                .body("links.self", equalTo("/users/5/relationships/citizenships"))
                .body("links", hasKey("related:countries"))
                // user 5 has citizenships NO and US, paginated by 2 — first page
                .body("data", hasSize(1))
                .body("data[0].id", equalTo("US"))
                .body("data[0].type", equalTo("countries"));
    }

}
