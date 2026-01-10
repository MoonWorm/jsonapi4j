package pro.api4.jsonapi4j.sampleapp.operations.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;
import pro.api4.jsonapi4j.sampleapp.utils.ResourceUtil;
import pro.api4.jsonapi4j.servlet.filter.ac.DefaultPrincipalResolver;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
public class ReadUserCitizenshipsOperationTests {

    @Value("${jsonapi4j.root-path}")
    private String jsonApiRootPath;

    @LocalServerPort
    private int appPort;

    @Test
    public void test_readCitizenshipsRelationshipAcPassed() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(DefaultPrincipalResolver.DEFAULT_SCOPES_HEADER_NAME, "users.citizenships.read")
                .header(DefaultPrincipalResolver.DEFAULT_USER_ID_HEADER_NAME, "5")
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "citizenships")
                .pathParam("userId", "5")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/citizenships")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/user/jsonapi/multiple-countries-linkage-response.json")));
    }

    @Test
    public void test_readCitizenshipsRelationshipAcNotPassed() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(DefaultPrincipalResolver.DEFAULT_SCOPES_HEADER_NAME, "users.citizenships.read")
                .header(DefaultPrincipalResolver.DEFAULT_USER_ID_HEADER_NAME, "555")
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "citizenships")
                .pathParam("userId", "5")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/citizenships")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/user/jsonapi/multiple-countries-linkage-response-not-allowed.json")));
    }

}
