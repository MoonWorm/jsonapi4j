package pro.api4.jsonapi4j.sampleapp.testsuite.domain.country;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;

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
                .pathParam("countryId", "TG")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/countries/{countryId}/relationships/currencies")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                // top-level links
                .body("links.self", equalTo("/countries/TG/relationships/currencies"))
                .body("links", hasKey("related:currencies"))
                // data
                .body("data", hasSize(1))
                .body("data[0].id", equalTo("XOF"))
                .body("data[0].type", equalTo("currencies"));
    }

}
