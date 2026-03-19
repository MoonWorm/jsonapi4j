package pro.api4.jsonapi4j.sampleapp.testsuite.domain.currencies;

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

public abstract class ReadMultipleCurrenciesOperationTests {

    private final String jsonApiRootPath;
    private final int appPort;

    public ReadMultipleCurrenciesOperationTests(String jsonApiRootPath,
                                                int appPort) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.appPort = appPort;
    }

    @Test
    public void test_filterByIdsWithRelationships() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "currencies")
                .queryParam(FiltersAwareRequest.getFilterParam(ID_FILTER_NAME), "NOK")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/currencies")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/domain/currency/multiple-currencies-byids-response.json")));
    }

    @Test
    public void test_readAll_validationError() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "currencies")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/currencies")
                .then()
                .statusCode(400)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("errors[0].code", equalTo("GENERIC_REQUEST_ERROR"))
                .body("errors[0].detail", equalTo("value can't be null"))
                .body("errors[0].source.parameter", equalTo("filter[id]"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].id", notNullValue());
    }

}
