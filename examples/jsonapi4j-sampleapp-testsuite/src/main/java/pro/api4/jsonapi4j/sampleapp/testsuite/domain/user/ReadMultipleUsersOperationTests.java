package pro.api4.jsonapi4j.sampleapp.testsuite.domain.user;

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

    public ReadMultipleUsersOperationTests(String jsonApiRootPath,
                                           int appPort) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.appPort = appPort;
    }

    @Test
    public void test_readMultiple() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/domain/user/multiple-users-response.json")));
    }

    @Test
    public void test_readMultipleWithRelationships() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives", "citizenships", "placeOfBirth")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/domain/user/multiple-users-with-relationships-response.json")));
    }

    @Test
    public void test_readMultipleWithRelationshipsWithAccessToSensitiveData() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives", "citizenships", "placeOfBirth")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/domain/user/multiple-users-with-sensitive-data-response.json")));
    }

    @Test
    public void test_filterByIdsWithRelationships() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives", "citizenships", "placeOfBirth")
                .queryParam(FiltersAwareRequest.getFilterParam(ID_FILTER_NAME), "1", "2")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/domain/user/multiple-users-byids-response.json")));
    }

}
