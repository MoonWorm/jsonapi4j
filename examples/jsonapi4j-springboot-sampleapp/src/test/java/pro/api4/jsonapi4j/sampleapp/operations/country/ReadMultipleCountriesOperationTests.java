package pro.api4.jsonapi4j.sampleapp.operations.country;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import pro.api4.jsonapi4j.request.FiltersAwareRequest;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;
import pro.api4.jsonapi4j.sampleapp.utils.ResourceUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static pro.api4.jsonapi4j.operation.ReadMultipleResourcesOperation.ID_FILTER_NAME;
import static pro.api4.jsonapi4j.sampleapp.operations.country.ReadMultipleCountriesOperation.REGION_FILTER_NAME;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ReadMultipleCountriesOperationTests {

    private WireMockServer wiremockServer;

    @Value("${wiremock.port}")
    private int wiremockPort;

    @Value("${jsonapi4j.root-path}")
    private String jsonApiRootPath;

    @LocalServerPort
    private int appPort;

    @BeforeEach
    void setup() {
        wiremockServer = new WireMockServer(WireMockConfiguration.options().port(wiremockPort));
        wiremockServer.start();
        WireMock.configureFor("localhost", wiremockPort);
    }

    @AfterEach
    void teardown() {
        if (wiremockServer != null && wiremockServer.isRunning()) {
            wiremockServer.stop();
        }
    }

    @Test
    public void test_readMultipleWithRelationships() {
        wiremockServer.stubFor(get(urlEqualTo("/all?fields=cca2&fields=name&fields=region&fields=currencies"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ResourceUtil.readResourceFile("operations/country/restcountries/multiple-countries-response.json"))));

        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "currencies")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/countries")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/country/jsonapi/multiple-countries-all-response.json")));
    }

    @Test
    public void test_filterByIdsWithRelationships() {
        wiremockServer.stubFor(get(urlEqualTo("/alpha?codes=TG&codes=YT&fields=cca2&fields=name&fields=region&fields=currencies"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ResourceUtil.readResourceFile("operations/country/restcountries/multiple-countries-response.json"))));

        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "currencies")
                .queryParam(FiltersAwareRequest.getFilterParam(ID_FILTER_NAME), "TG", "YT")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/countries")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/country/jsonapi/multiple-countries-byids-response.json")));
    }

    @Test
    public void test_filterByRegion() {
        wiremockServer.stubFor(get(urlEqualTo("/region/europe?fields=cca2&fields=name&fields=region&fields=currencies"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ResourceUtil.readResourceFile("operations/country/restcountries/multiple-countries-response.json"))));

        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "currencies")
                .queryParam(FiltersAwareRequest.getFilterParam(REGION_FILTER_NAME), "europe")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/countries")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/country/jsonapi/multiple-countries-byregion-response.json")));
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
