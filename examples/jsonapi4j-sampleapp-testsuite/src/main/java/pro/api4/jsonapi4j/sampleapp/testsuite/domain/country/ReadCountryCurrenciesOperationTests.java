package pro.api4.jsonapi4j.sampleapp.testsuite.domain.country;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;
import pro.api4.jsonapi4j.sampleapp.testsuite.util.ResourceUtil;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;

public abstract class ReadCountryCurrenciesOperationTests {

    private final String jsonApiRootPath;
    private final int appPort;

    public ReadCountryCurrenciesOperationTests(String jsonApiRootPath, int appPort) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.appPort = appPort;
    }

    @Test
    public void test_readCurrenciesRelationship() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "currencies")
                .pathParam("countryId", "TG")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/countries/{countryId}/relationships/currencies")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/domain/country/multiple-currencies-linkage-response.json")));
    }

}
