package pro.api4.jsonapi4j.sampleapp;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static pro.api4.jsonapi4j.principal.DefaultPrincipalResolver.DEFAULT_SCOPES_HEADER_NAME;
import static pro.api4.jsonapi4j.principal.DefaultPrincipalResolver.DEFAULT_USER_ID_HEADER_NAME;

/**
 * Exercises the packaged application over HTTP, against the artifact the current build produced — the native
 * executable when built with {@code -Dnative}, the runner jar otherwise.
 *
 * <p>The JVM test suites already cover behaviour; this covers what only a native image can break. Attributes
 * objects, the document model and the copies access control builds during redaction all reach Jackson as
 * {@code Object}, never as a declared endpoint return type, so the native-image analysis has no reason to keep
 * their members unless the extension registered them ahead of time. Nothing about that is visible in a JVM run:
 * a missing registration builds cleanly and then fails on the first request that returns the affected type.
 *
 * <p>The assertions are therefore deliberately shallow and deliberately wide — one request per distinct
 * serialization path rather than deep coverage of any one of them. Every resource type the application serves is
 * requested at least once, including the six built-in meta types, whose absence from the registration is exactly
 * how this gap was found.
 *
 * <p>Runs against the application's own {@code application.properties} rather than a test profile: the plugin
 * toggles are {@code BUILD_AND_RUN_TIME_FIXED}, so a {@code @TestProfile} could not change them in a
 * pre-built artifact anyway. That configuration has every plugin enabled, which is what makes a single binary
 * enough here.
 */
@QuarkusIntegrationTest
public class QuarkusNativeSmokeIT {

    private static final String ROOT_PATH = "/jsonapi";

    private static String url(String path) {
        return ROOT_PATH + path;
    }

    @Nested
    class MetaApi {

        @Test
        public void get_configSingleton_serializesAttributes() {
            given()
                    .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                    .get(url("/config/this"))
                    .then()
                    .statusCode(200)
                    .contentType(JsonApiMediaType.MEDIA_TYPE)
                    .body("data.type", equalTo("config"))
                    .body("data.attributes.settings.rootPath", equalTo(ROOT_PATH));
        }

        @Test
        public void get_stateSingleton_serializesAttributes() {
            given()
                    .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                    .get(url("/state/this"))
                    .then()
                    .statusCode(200)
                    .body("data.type", equalTo("state"))
                    .body("data.attributes.frameworkVersion", notNullValue())
                    .body("data.attributes.javaVersion", notNullValue());
        }

        @Test
        public void get_plugins_serializesAttributes() {
            given()
                    .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                    .get(url("/plugins"))
                    .then()
                    .statusCode(200)
                    .body("data[0].type", equalTo("plugins"))
                    .body("data[0].attributes.name", notNullValue())
                    .body("data[0].attributes.className", notNullValue());
        }

        @Test
        public void get_resources_serializesAttributes() {
            given()
                    .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                    .get(url("/resources"))
                    .then()
                    .statusCode(200)
                    .body("data[0].type", equalTo("resources"))
                    .body("data[0].attributes.type", notNullValue())
                    .body("data[0].attributes.className", notNullValue());
        }

        @Test
        public void get_relationships_serializesAttributes() {
            given()
                    .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                    .get(url("/relationships"))
                    .then()
                    .statusCode(200)
                    .body("data[0].type", equalTo("relationships"))
                    .body("data[0].attributes.name", notNullValue());
        }

        @Test
        public void get_operations_serializesAttributes() {
            given()
                    .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                    .get(url("/operations"))
                    .then()
                    .statusCode(200)
                    .body("data[0].type", equalTo("operations"))
                    .body("data[0].attributes.operationType", notNullValue());
        }
    }

    @Nested
    class DomainResources {

        /**
         * Sends a principal: the users resource is access-controlled, and an anonymous caller is served a fully
         * anonymized document, which would not exercise attributes serialization at all.
         */
        @Test
        public void get_usersCollection_serializesAttributes() {
            given()
                    .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                    .header(DEFAULT_USER_ID_HEADER_NAME, "1")
                    .get(url("/users"))
                    .then()
                    .statusCode(200)
                    .body("data[0].type", equalTo("users"))
                    .body("data[0].attributes.fullName", notNullValue());
        }

        @Test
        public void get_countryById_serializesAttributes() {
            given()
                    .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                    .get(url("/countries/TG"))
                    .then()
                    .statusCode(200)
                    .body("data.type", equalTo("countries"))
                    .body("data.attributes.name", equalTo("Togo"))
                    .body("data.attributes.region", equalTo("Africa"));
        }

        @Test
        public void get_currenciesFilteredById_serializesAttributes() {
            given()
                    .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                    .get(url("/currencies?filter[id]=XOF"))
                    .then()
                    .statusCode(200)
                    .body("data", hasSize(1))
                    .body("data[0].type", equalTo("currencies"))
                    .body("data[0].attributes.name", equalTo("West African CFA franc"))
                    .body("data[0].attributes.symbol", equalTo("Fr"));
        }
    }

    /**
     * The path most exposed to native-image breakage. Hiding a field builds a copy of the attributes object
     * without running its constructor, which the image permits only for classes registered ahead of time — so a
     * denied request is the first thing to fail, and the error document reporting that failure serializes to an
     * empty body.
     */
    @Nested
    class AccessControl {

        @Test
        public void get_userByIdAsNonOwner_hidesSensitiveFields() {
            given()
                    .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                    .header(DEFAULT_USER_ID_HEADER_NAME, "2")
                    .get(url("/users/1"))
                    .then()
                    .statusCode(200)
                    .contentType(JsonApiMediaType.MEDIA_TYPE)
                    .body("data.attributes.fullName", equalTo("John Doe"))
                    .body("data.attributes", not(hasKey("creditCardNumber")))
                    .body("data.attributes.addresses[0].city", equalTo("Oslo"))
                    .body("data.attributes.addresses[0]", not(hasKey("zip")))
                    .body("data.attributes.addresses[0]", not(hasKey("doorCode")));
        }

        @Test
        public void get_userByIdAsOwnerWithSensitiveScope_revealsSensitiveFields() {
            given()
                    .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                    .header(DEFAULT_USER_ID_HEADER_NAME, "1")
                    .header(DEFAULT_SCOPES_HEADER_NAME, "users.sensitive.read")
                    .get(url("/users/1"))
                    .then()
                    .statusCode(200)
                    .body("data.attributes.creditCardNumber", equalTo("123456789"))
                    .body("data.attributes.addresses[0].city", equalTo("Oslo"))
                    .body("data.attributes.addresses[0].zip", equalTo("0150"))
                    .body("data.attributes.addresses[0].doorCode", equalTo("door-1"));
        }
    }

    @Nested
    class Plugins {

        @Test
        public void get_countryWithInclude_serializesIncludedResources() {
            given()
                    .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                    .get(url("/countries/TG?include=currencies"))
                    .then()
                    .statusCode(200)
                    .body("data.relationships.currencies.data[0].id", equalTo("XOF"))
                    .body("included", hasSize(1))
                    .body("included[0].type", equalTo("currencies"))
                    .body("included[0].attributes.name", equalTo("West African CFA franc"));
        }

        @Test
        public void get_countryWithSparseFieldset_omitsUnrequestedAttributes() {
            given()
                    .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                    .get(url("/countries/TG?fields[countries]=name"))
                    .then()
                    .statusCode(200)
                    .body("data.attributes.name", equalTo("Togo"))
                    .body("data.attributes", not(hasKey("region")));
        }

        /**
         * The OpenAPI document is bound into Swagger's own model, which is third-party and ships no
         * native-image metadata — a distinct registration from the one covering resource attributes.
         */
        @Test
        public void get_oasDocument_serializesOpenApiModel() {
            given()
                    .get(ROOT_PATH + "/oas")
                    .then()
                    .statusCode(200)
                    .body("openapi", notNullValue())
                    .body("info.title", equalTo("JSON:API Users & Countries"))
                    .body("paths", notNullValue());
        }
    }
}
