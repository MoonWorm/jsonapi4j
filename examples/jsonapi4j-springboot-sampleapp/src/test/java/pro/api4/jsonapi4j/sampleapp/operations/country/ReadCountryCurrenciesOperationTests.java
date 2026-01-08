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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ReadCountryCurrenciesOperationTests {

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
    public void test_readCurrenciesRelationship() {
        wiremockServer.stubFor(get(urlEqualTo("/alpha?codes=TG&fields=cca2&fields=name&fields=region&fields=currencies"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ResourceUtil.readResourceFile("operations/country/restcountries/single-country-response.json"))));

        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .get("http://localhost:" + appPort + "/jsonapi/countries/TG/relationships/currencies?include=currencies")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/country/jsonapi/multiple-currencies-response.json")));
    }

}
