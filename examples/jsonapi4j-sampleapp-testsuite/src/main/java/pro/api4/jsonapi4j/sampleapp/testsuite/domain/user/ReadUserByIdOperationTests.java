package pro.api4.jsonapi4j.sampleapp.testsuite.domain.user;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

public abstract class ReadUserByIdOperationTests {

    private final String jsonApiRootPath;
    private final int appPort;

    public ReadUserByIdOperationTests(String jsonApiRootPath,
                                      int appPort) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.appPort = appPort;
    }

    @Test
    public void test_readById() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "1")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                // top-level links
                .body("links.self", equalTo("/users/1"))
                // data
                .body("data.id", equalTo("1"))
                .body("data.type", equalTo("users"))
                // attributes
                .body("data.attributes.fullName", equalTo("John Doe"))
                .body("data.attributes.email", equalTo("john@doe.com"))
                .body("data.attributes.creditCardNumber", equalTo("123456789"))
                // relationship links (no data — includes not requested)
                .body("data.relationships.citizenships.links.self", equalTo("/users/1/relationships/citizenships"))
                .body("data.relationships.placeOfBirth.links.self", equalTo("/users/1/relationships/placeOfBirth"))
                .body("data.relationships.relatives.links.self", equalTo("/users/1/relationships/relatives"))
                // resource-level self link
                .body("data.links.self", equalTo("/users/1"));
    }

    @Test
    public void test_readByIdWithAllRelationships() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives", "citizenships", "placeOfBirth")
                .pathParam("userId", "1")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                // attributes
                .body("data.attributes.fullName", equalTo("John Doe"))
                .body("data.attributes.email", equalTo("john@doe.com"))
                // citizenships — to-many with data
                .body("data.relationships.citizenships.data", hasSize(3))
                .body("data.relationships.citizenships.data.find { it.id == 'NO' }.type", equalTo("countries"))
                .body("data.relationships.citizenships.data.find { it.id == 'FI' }.type", equalTo("countries"))
                .body("data.relationships.citizenships.data.find { it.id == 'US' }.type", equalTo("countries"))
                .body("data.relationships.citizenships.links", hasKey("related:countries"))
                // placeOfBirth — to-one with data
                .body("data.relationships.placeOfBirth.data.id", equalTo("US"))
                .body("data.relationships.placeOfBirth.data.type", equalTo("countries"))
                .body("data.relationships.placeOfBirth.links.related", equalTo("/countries/US"))
                // relatives — to-many with data and meta
                .body("data.relationships.relatives.data", hasSize(2))
                .body("data.relationships.relatives.data.find { it.id == '2' }.meta.relationshipType", equalTo("HUSBAND"))
                .body("data.relationships.relatives.data.find { it.id == '3' }.meta.relationshipType", equalTo("BROTHER"))
                .body("data.relationships.relatives.links", hasKey("related:users"));
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
                .body("errors[0].detail", equalTo("'users' resource of a given id (100) is not found"))
                .body("errors[0].id", notNullValue());
    }

}
