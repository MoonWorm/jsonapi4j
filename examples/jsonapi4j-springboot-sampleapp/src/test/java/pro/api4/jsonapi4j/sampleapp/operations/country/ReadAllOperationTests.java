package pro.api4.jsonapi4j.sampleapp.operations.country;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ReadAllOperationTests {

    private WireMockServer wiremockServer;

    @Value("${wiremock.port}")
    private int wiremockPort;

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
    public void test_readAll() {
        wiremockServer.stubFor(get(urlEqualTo("/all?fields=cca2&fields=name&fields=region&fields=currencies"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ResourceUtil.readResourceFile("operations/country/restcountries/multiple-countries-response.json"))));

        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .get("http://localhost:" + appPort + "/jsonapi/countries?include=currencies")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/country/jsonapi/multiple-countries-all-response.json")));
    }

    @Test
    public void test_filterByIds() {
        wiremockServer.stubFor(get(urlEqualTo("/alpha?codes=TG&codes=YT&fields=cca2&fields=name&fields=region&fields=currencies"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ResourceUtil.readResourceFile("operations/country/restcountries/multiple-countries-response.json"))));

        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .get("http://localhost:" + appPort + "/jsonapi/countries?filter[id]=TG,YT&include=currencies")
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
                .get("http://localhost:" + appPort + "/jsonapi/countries?filter[region]=europe&include=currencies")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/country/jsonapi/multiple-countries-byregion-response.json")));
    }

    @Test
    public void test_readAll_validationError() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .get("http://localhost:" + appPort + "/jsonapi/countries?filter[region]=foo&include=currencies")
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
