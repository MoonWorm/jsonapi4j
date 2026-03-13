package pro.api4.jsonapi4j.sampleapp.testsuite.user;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;
import pro.api4.jsonapi4j.sampleapp.util.ResourceUtil;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;

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
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "placeOfBirth")
                .pathParam("userId", "1")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/placeOfBirth")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/user/jsonapi/single-country-linkage-response.json")));
    }

}
