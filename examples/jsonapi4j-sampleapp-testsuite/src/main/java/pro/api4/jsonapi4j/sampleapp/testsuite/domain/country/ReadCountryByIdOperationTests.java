package pro.api4.jsonapi4j.sampleapp.testsuite.domain.country;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

public abstract class ReadCountryByIdOperationTests {

    private final String jsonApiRootPath;
    private final int appPort;

    public ReadCountryByIdOperationTests(String jsonApiRootPath,
                                         int appPort) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.appPort = appPort;
    }

    @Test
    public void test_readById() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("countryId", "TG")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/countries/{countryId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("links.self", equalTo("/countries/TG"))
                .body("data.id", equalTo("TG"))
                .body("data.type", equalTo("countries"))
                .body("data.attributes.name", equalTo("Togo"))
                .body("data.attributes.region", equalTo("Africa"))
                .body("data.relationships.currencies.links.self", equalTo("/countries/TG/relationships/currencies"))
                .body("data.links.self", equalTo("/countries/TG"));
    }

    @Test
    public void test_readByIdWithRelationships() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("countryId", "TG")
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "currencies")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/countries/{countryId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("data.id", equalTo("TG"))
                .body("data.attributes.name", equalTo("Togo"))
                .body("data.attributes.region", equalTo("Africa"))
                // currencies relationship with data
                .body("data.relationships.currencies.data", hasSize(1))
                .body("data.relationships.currencies.data[0].id", equalTo("XOF"))
                .body("data.relationships.currencies.data[0].type", equalTo("currencies"))
                .body("data.relationships.currencies.links", hasKey("related:currencies"));
    }

    @Test
    public void test_readById_validationError() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("countryId", "TG")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/foobars/{countryId}")
                .then()
                .statusCode(404)
                .body("errors[0].code", equalTo("NOT_FOUND"))
                .body("errors[0].status", equalTo("404"))
                .body("errors[0].detail", equalTo("JSON:API operation can not be resolved for the path: /foobars/TG, and method: GET. Unknown resource type: foobars"))
                .body("errors[0].id", notNullValue());
    }

}
