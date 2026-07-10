package pro.api4.jsonapi4j.sampleapp.testsuite;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.FiltersAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import static io.restassured.RestAssured.given;
import static pro.api4.jsonapi4j.request.IncludeAwareRequest.INCLUDE_PARAM;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.oneOf;

/**
 * Black-box tests for the built-in introspection ("meta") API. Verifies the singleton {@code state}/{@code config}
 * resources, the four meta collections, in-house relationship linkage (no compound-docs/HTTP needed), and the
 * reserved-id 404 behavior. Also verifies {@code ?include=...} populates the {@code included} array with the full
 * meta resources via the compound-docs plugin (self-HTTP); those same-app resources resolve against the incoming
 * request's own endpoint, so no {@code jsonapi4j.cd.mapping} entries are required. Runs under a dedicated profile with
 * the meta API and compound-docs plugin enabled.
 */
public abstract class MetaApiTests {

    private static final String COMPOUND_DOCS_PLUGIN_NAME = "JsonApiCompoundDocsPlugin";

    private final String jsonApiRootPath;
    private final int appPort;

    public MetaApiTests(String jsonApiRootPath, int appPort) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.appPort = appPort;
    }

    private String url(String path) {
        return "http://localhost:" + appPort + jsonApiRootPath + path;
    }

    @Test
    public void test_readState() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .get(url("/state/this"))
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("data.id", equalTo("this"))
                .body("data.type", equalTo("state"))
                .body("data.attributes.frameworkVersion", notNullValue())
                .body("data.attributes.javaVersion", notNullValue())
                .body("data.attributes.integration", oneOf("SPRING", "QUARKUS", "SERVLET"))
                .body("data.attributes.resourcesCount", equalTo(3))
                .body("data.attributes.relationshipsCount", equalTo(4))
                .body("data.attributes.operationsCount", greaterThan(0))
                .body("data.relationships.operations.links.self",
                        equalTo("/state/this/relationships/operations"))
                .body("data.relationships.config.links.self",
                        equalTo("/state/this/relationships/config"));
    }

    @Test
    public void test_readState_linkageResolvedInHouse() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(INCLUDE_PARAM, "operations,config")
                .get(url("/state/this"))
                .then()
                .statusCode(200)
                .body("data.relationships.operations.data.size()", greaterThan(0))
                .body("data.relationships.operations.data[0].type", equalTo("operations"))
                .body("data.relationships.config.data.type", equalTo("config"))
                .body("data.relationships.config.data.id", equalTo("this"));
    }

    @Test
    public void test_readState_asCollection() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .get(url("/state"))
                .then()
                .statusCode(200)
                .body("data", hasSize(1))
                .body("data[0].id", equalTo("this"))
                .body("data[0].type", equalTo("state"));
    }

    @Test
    public void test_readState_reservedIdOnly() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .get(url("/state/bogus"))
                .then()
                .statusCode(404)
                .body("errors[0].status", equalTo("404"));
    }

    @Test
    public void test_readPlugins() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .get(url("/plugins"))
                .then()
                .statusCode(200)
                .body("data.size()", greaterThan(0))
                .body("data[0].type", equalTo("plugins"))
                .body("data.attributes.name", hasItem(COMPOUND_DOCS_PLUGIN_NAME))
                .body("data.find { it.attributes.name == '" + COMPOUND_DOCS_PLUGIN_NAME + "' }.attributes.enabled",
                        equalTo(true));
    }

    @Test
    public void test_readConfig() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .get(url("/config/this"))
                .then()
                .statusCode(200)
                .body("data.type", equalTo("config"))
                .body("data.attributes.settings.rootPath", equalTo(jsonApiRootPath))
                .body("data.attributes.settings.meta.enabled", notNullValue())
                .body("data.attributes.settings.cd.enabled", notNullValue());
    }

    @Test
    public void test_readResourceDescriptor() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .get(url("/resources/users"))
                .then()
                .statusCode(200)
                .body("data.type", equalTo("resources"))
                .body("data.attributes.type", equalTo("users"))
                .body("data.attributes.className", notNullValue());
    }

    @Test
    public void test_resources_excludeReservedMetaTypes() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .get(url("/resources"))
                .then()
                .statusCode(200)
                .body("data.id", hasItems("users", "countries", "currencies"))
                .body("data.id", not(hasItem("state")))
                .body("data.id", not(hasItem("config")))
                .body("data.id", not(hasItem("operations")));
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .get(url("/resources/state"))
                .then()
                .statusCode(404);
    }

    @Test
    public void test_operationsCarryEndpointRoute() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(FiltersAwareRequest.getFilterParam("id"), "users.READ_RESOURCE_BY_ID")
                .get(url("/operations"))
                .then()
                .statusCode(200)
                .body("data.size()", greaterThan(0))
                .body("data[0].attributes.httpMethod", equalTo("GET"))
                .body("data[0].attributes.pathTemplate", equalTo(jsonApiRootPath + "/users/{id}"));
    }

    @Test
    public void test_includePlugins() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(INCLUDE_PARAM, "plugins")
                .get(url("/state/this"))
                .then()
                .statusCode(200)
                .body("data.relationships.plugins.data.size()", greaterThan(0))
                .body("included.findAll { it.type == 'plugins' }.attributes.name", hasItem(COMPOUND_DOCS_PLUGIN_NAME))
                .body("included.find { it.attributes.name == '" + COMPOUND_DOCS_PLUGIN_NAME + "' }.id",
                        equalTo(COMPOUND_DOCS_PLUGIN_NAME))
                .body("included.find { it.attributes.name == '" + COMPOUND_DOCS_PLUGIN_NAME + "' }.attributes.enabled",
                        equalTo(true));
    }

    @Test
    public void test_includePluginsAndConfig() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(INCLUDE_PARAM, "plugins,config")
                .get(url("/state/this"))
                .then()
                .statusCode(200)
                .body("included.findAll { it.type == 'plugins' }.attributes.name", hasItem(COMPOUND_DOCS_PLUGIN_NAME))
                .body("included.find { it.type == 'config' }.id", equalTo("this"))
                .body("included.find { it.type == 'config' }.attributes.settings.rootPath", equalTo(jsonApiRootPath));
    }

}
