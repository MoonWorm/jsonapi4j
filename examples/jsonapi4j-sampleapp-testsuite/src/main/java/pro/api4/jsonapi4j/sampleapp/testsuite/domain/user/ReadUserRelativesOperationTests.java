package pro.api4.jsonapi4j.sampleapp.testsuite.domain.user;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;
import pro.api4.jsonapi4j.sampleapp.testsuite.util.ResourceUtil;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;

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
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives")
                .pathParam("userId", "1")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/relatives")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/domain/user/multiple-users-linkage-response.json")));
    }

}
