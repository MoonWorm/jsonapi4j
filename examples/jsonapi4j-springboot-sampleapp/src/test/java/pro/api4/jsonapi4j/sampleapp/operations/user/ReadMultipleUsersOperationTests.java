package pro.api4.jsonapi4j.sampleapp.operations.user;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import pro.api4.jsonapi4j.request.FiltersAwareRequest;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;
import pro.api4.jsonapi4j.sampleapp.utils.ResourceUtil;
import pro.api4.jsonapi4j.servlet.filter.ac.DefaultPrincipalResolver;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static pro.api4.jsonapi4j.operation.ReadMultipleResourcesOperation.ID_FILTER_NAME;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ReadMultipleUsersOperationTests {

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
        wiremockServer.stubFor(get(urlMatching(".*/alpha.*?"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ResourceUtil.readResourceFile("operations/country/restcountries/multiple-countries-response-2.json"))));

        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(DefaultPrincipalResolver.DEFAULT_USER_ID_HEADER_NAME, "2")
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives", "citizenships", "placeOfBirth")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/user/jsonapi/multiple-users-response.json")));
    }

    @Test
    public void test_readMultipleWithRelationshipsWithAccessToSensitiveData() {
        wiremockServer.stubFor(get(urlMatching(".*/alpha.*?"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ResourceUtil.readResourceFile("operations/country/restcountries/multiple-countries-response-2.json"))));

        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(DefaultPrincipalResolver.DEFAULT_SCOPES_HEADER_NAME, "users.sensitive.read")
                .header(DefaultPrincipalResolver.DEFAULT_USER_ID_HEADER_NAME, "1")
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives", "citizenships", "placeOfBirth")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/user/jsonapi/multiple-users-with-sensitive-data-response.json")));
    }

    @Test
    public void test_filterByIdsWithRelationships() {
        wiremockServer.stubFor(get(urlMatching(".*/alpha.*?"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ResourceUtil.readResourceFile("operations/country/restcountries/multiple-countries-response-2.json"))));


        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(DefaultPrincipalResolver.DEFAULT_USER_ID_HEADER_NAME, "3")
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives", "citizenships", "placeOfBirth")
                .queryParam(FiltersAwareRequest.getFilterParam(ID_FILTER_NAME), "1", "2")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/user/jsonapi/multiple-users-byids-response.json")));
    }

}
