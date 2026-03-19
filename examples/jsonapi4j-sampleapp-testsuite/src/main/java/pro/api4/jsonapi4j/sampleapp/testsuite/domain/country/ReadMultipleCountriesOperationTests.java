package pro.api4.jsonapi4j.sampleapp.testsuite.domain.country;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.FiltersAwareRequest;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;
import pro.api4.jsonapi4j.sampleapp.testsuite.util.ResourceUtil;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static pro.api4.jsonapi4j.operation.ReadMultipleResourcesOperation.ID_FILTER_NAME;
import static pro.api4.jsonapi4j.sampleapp.operations.country.ReadMultipleCountriesOperation.REGION_FILTER_NAME;

public abstract class ReadMultipleCountriesOperationTests {

    private final String jsonApiRootPath;
    private final int appPort;

    public ReadMultipleCountriesOperationTests(String jsonApiRootPath,
                                               int appPort) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.appPort = appPort;
    }

    @Test
    public void test_readMultiple() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .get("http://localhost:" + appPort + jsonApiRootPath + "/countries")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/domain/country/multiple-countries-all-response.json")));
    }

    @Test
    public void test_readMultipleWithRelationships() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "currencies")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/countries")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/domain/country/multiple-countries-all-with-relationships-response.json")));
    }

    @Test
    public void test_filterByIdsWithRelationships() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "currencies")
                .queryParam(FiltersAwareRequest.getFilterParam(ID_FILTER_NAME), "TG", "YT")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/countries")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/domain/country/multiple-countries-byids-response.json")));
    }

    @Test
    public void test_filterByRegion() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "currencies")
                .queryParam(FiltersAwareRequest.getFilterParam(REGION_FILTER_NAME), "europe")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/countries")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/domain/country/multiple-countries-byregion-response.json")));
    }

    @Test
    public void test_readAll_validationError() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "currencies")
                .queryParam(FiltersAwareRequest.getFilterParam(REGION_FILTER_NAME), "foo")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/countries")
                .then()
                .statusCode(400)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("errors[0].code", equalTo("GENERIC_REQUEST_ERROR"))
                .body("errors[0].detail", equalTo("Unknown region"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].source.parameter", equalTo("region"))
                .body("errors[0].id", notNullValue());
    }

}
