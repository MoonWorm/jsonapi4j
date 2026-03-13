package pro.api4.jsonapi4j.sampleapp.testsuite.user;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.FiltersAwareRequest;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;
import pro.api4.jsonapi4j.sampleapp.testsuite.util.ResourceUtil;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static pro.api4.jsonapi4j.operation.ReadMultipleResourcesOperation.ID_FILTER_NAME;

public abstract class ReadMultipleUsersOperationTests {

    private final String jsonApiRootPath;
    private final int appPort;

    private final String defaultAccessTierHeaderName;
    private final String defaultScopesHeaderName;
    private final String defaultUserIdHeaderName;

    public ReadMultipleUsersOperationTests(String jsonApiRootPath,
                                           int appPort,
                                           String defaultAccessTierHeaderName,
                                           String defaultScopesHeaderName,
                                           String defaultUserIdHeaderName) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.appPort = appPort;
        this.defaultAccessTierHeaderName = defaultAccessTierHeaderName;
        this.defaultScopesHeaderName = defaultScopesHeaderName;
        this.defaultUserIdHeaderName = defaultUserIdHeaderName;
    }

    @Test
    public void test_readMultiple() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultUserIdHeaderName, "2")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/user/jsonapi/multiple-users-response.json")));
    }

    @Test
    public void test_readMultipleWithRelationships() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultUserIdHeaderName, "2")
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives", "citizenships", "placeOfBirth")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/user/jsonapi/multiple-users-with-relationships-response.json")));
    }

    @Test
    public void test_readMultipleWithRelationshipsWithAccessToSensitiveData() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultScopesHeaderName, "users.sensitive.read")
                .header(defaultUserIdHeaderName, "1")
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives", "citizenships", "placeOfBirth")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/user/jsonapi/multiple-users-with-sensitive-data-response.json")));
    }

    @Test
    public void test_filterByIdsWithRelationships() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultUserIdHeaderName, "3")
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives", "citizenships", "placeOfBirth")
                .queryParam(FiltersAwareRequest.getFilterParam(ID_FILTER_NAME), "1", "2")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/user/jsonapi/multiple-users-byids-response.json")));
    }

}
