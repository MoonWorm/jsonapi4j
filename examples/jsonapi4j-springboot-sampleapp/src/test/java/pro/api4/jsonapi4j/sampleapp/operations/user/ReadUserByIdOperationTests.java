package pro.api4.jsonapi4j.sampleapp.operations.user;

import pro.api4.jsonapi4j.sampleapp.operations.RestAssuredUtf8TestBase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;
import pro.api4.jsonapi4j.request.SparseFieldsetsAwareRequest;
import pro.api4.jsonapi4j.sampleapp.utils.ResourceUtil;
import pro.api4.jsonapi4j.principal.DefaultPrincipalResolver;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
public class ReadUserByIdOperationTests extends RestAssuredUtf8TestBase {

    @Value("${jsonapi4j.root-path}")
    private String jsonApiRootPath;

    @LocalServerPort
    private int appPort;

    @Test
    public void test_readById() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(DefaultPrincipalResolver.DEFAULT_USER_ID_HEADER_NAME, "2")
                .pathParam("userId", "1")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/user/jsonapi/single-user-byid-response.json")));
    }

    @Test
    public void test_readByIdWithAccessToSensitiveData() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(DefaultPrincipalResolver.DEFAULT_SCOPES_HEADER_NAME, "users.sensitive.read")
                .header(DefaultPrincipalResolver.DEFAULT_USER_ID_HEADER_NAME, "1")
                .pathParam("userId", "1")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/user/jsonapi/single-user-byid-with-sensitive-data-response.json")));
    }

    @Test
    public void test_readByIdWithAllRelationships() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(DefaultPrincipalResolver.DEFAULT_USER_ID_HEADER_NAME, "2")
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives", "citizenships", "placeOfBirth")
                .pathParam("userId", "1")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body(jsonEquals(ResourceUtil.readResourceFile("operations/user/jsonapi/single-user-byid-response-with-relationships.json")));
    }

    @Test
    public void test_readById_validationError() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "100")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(404)
                .body("errors[0].code", equalTo("NOT_FOUND"))
                .body("errors[0].status", equalTo("404"))
                .body("errors[0].detail", equalTo("'ResourceType(type=users)' resource of a given id (100) is not found"))
                .body("errors[0].id", notNullValue());
    }

    @Test
    public void test_readById_sparseFieldsets_filtersFieldsAndIgnoresUnknown() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(DefaultPrincipalResolver.DEFAULT_USER_ID_HEADER_NAME, "2")
                .queryParam(SparseFieldsetsAwareRequest.toFieldsetParamName("users"), "fullName,unknownField")
                .pathParam("userId", "1")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("links.self", equalTo("/users/1?fields%5Busers%5D=fullName%2CunknownField"))
                .body("data.attributes.fullName", equalTo("John Doe"))
                .body("data.attributes.email", nullValue())
                .body("data.relationships", nullValue());
    }

}
