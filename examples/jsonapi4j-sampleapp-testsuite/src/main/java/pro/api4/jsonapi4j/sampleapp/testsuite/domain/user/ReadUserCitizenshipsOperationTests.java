package pro.api4.jsonapi4j.sampleapp.testsuite.domain.user;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;
import pro.api4.jsonapi4j.sampleapp.testsuite.util.ResourceUtil;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;

public abstract class ReadUserCitizenshipsOperationTests {

    private final String jsonApiRootPath;
    private final int appPort;

    public ReadUserCitizenshipsOperationTests(String jsonApiRootPath,
                                              int appPort) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.appPort = appPort;
    }

    @Test
    public void test_readCitizenshipsRelationshipAcPassed() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "citizenships")
                .pathParam("userId", "5")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/citizenships")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/domain/user/multiple-countries-linkage-response.json")));
    }

    @Test
    public void test_readCitizenshipsRelationshipAcNotPassed() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "citizenships")
                .pathParam("userId", "5")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/citizenships")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/domain/user/multiple-countries-linkage-response-not-allowed.json")));
    }

}
