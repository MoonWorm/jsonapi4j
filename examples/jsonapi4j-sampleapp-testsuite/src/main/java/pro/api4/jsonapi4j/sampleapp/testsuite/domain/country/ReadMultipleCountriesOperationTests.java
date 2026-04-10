package pro.api4.jsonapi4j.sampleapp.testsuite.domain.country;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.FiltersAwareRequest;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
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
                // top-level links with pagination
                .body("links.self", equalTo("/countries"))
                .body("links.next", notNullValue())
                // paginated — default page size is 2
                .body("data", hasSize(2))
                .body("data[0].id", equalTo("TG"))
                .body("data[0].type", equalTo("countries"))
                .body("data[0].attributes.name", equalTo("Togo"))
                .body("data[0].attributes.region", equalTo("Africa"))
                .body("data[0].relationships.currencies.links.self", equalTo("/countries/TG/relationships/currencies"))
                .body("data[0].links.self", equalTo("/countries/TG"))
                .body("data[1].id", equalTo("YT"))
                .body("data[1].attributes.name", equalTo("Mayotte"))
                .body("data[1].attributes.region", equalTo("Africa"))
                .body("data[1].links.self", equalTo("/countries/YT"));
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
                .body("data", hasSize(2))
                // TG — currencies
                .body("data[0].relationships.currencies.data", hasSize(1))
                .body("data[0].relationships.currencies.data[0].id", equalTo("XOF"))
                .body("data[0].relationships.currencies.links", hasKey("related:currencies"))
                // YT — currencies
                .body("data[1].relationships.currencies.data", hasSize(1))
                .body("data[1].relationships.currencies.data[0].id", equalTo("EUR"))
                .body("data[1].relationships.currencies.links", hasKey("related:currencies"));
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
                .body("data", hasSize(2))
                .body("data.find { it.id == 'TG' }.attributes.name", equalTo("Togo"))
                .body("data.find { it.id == 'TG' }.relationships.currencies.data[0].id", equalTo("XOF"))
                .body("data.find { it.id == 'YT' }.attributes.name", equalTo("Mayotte"))
                .body("data.find { it.id == 'YT' }.relationships.currencies.data[0].id", equalTo("EUR"));
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
                .body("data", hasSize(2))
                .body("data.find { it.id == 'NO' }.attributes.name", equalTo("Norway"))
                .body("data.find { it.id == 'NO' }.attributes.region", equalTo("Europe"))
                .body("data.find { it.id == 'NO' }.relationships.currencies.data[0].id", equalTo("NOK"))
                .body("data.find { it.id == 'FI' }.attributes.name", equalTo("Finland"))
                .body("data.find { it.id == 'FI' }.attributes.region", equalTo("Europe"))
                .body("data.find { it.id == 'FI' }.relationships.currencies.data[0].id", equalTo("EUR"));
    }

    @Test
    public void test_readAll_validationError() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
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
