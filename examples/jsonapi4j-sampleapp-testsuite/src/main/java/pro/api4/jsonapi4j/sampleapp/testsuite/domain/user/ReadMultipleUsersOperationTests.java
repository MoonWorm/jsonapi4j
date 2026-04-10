package pro.api4.jsonapi4j.sampleapp.testsuite.domain.user;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.FiltersAwareRequest;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static pro.api4.jsonapi4j.operation.ReadMultipleResourcesOperation.ID_FILTER_NAME;

public abstract class ReadMultipleUsersOperationTests {

    private final String jsonApiRootPath;
    private final int appPort;

    public ReadMultipleUsersOperationTests(String jsonApiRootPath,
                                           int appPort) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.appPort = appPort;
    }

    @Test
    public void test_readMultiple() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                // top-level links with pagination
                .body("links.self", equalTo("/users"))
                .body("links.next", notNullValue())
                // paginated — default page size is 2
                .body("data", hasSize(2))
                // first user
                .body("data[0].id", equalTo("1"))
                .body("data[0].type", equalTo("users"))
                .body("data[0].attributes.fullName", equalTo("John Doe"))
                .body("data[0].attributes.email", equalTo("john@doe.com"))
                .body("data[0].attributes.creditCardNumber", equalTo("123456789"))
                .body("data[0].relationships.citizenships.links.self", equalTo("/users/1/relationships/citizenships"))
                .body("data[0].links.self", equalTo("/users/1"))
                // second user
                .body("data[1].id", equalTo("2"))
                .body("data[1].type", equalTo("users"))
                .body("data[1].attributes.fullName", equalTo("Jane Doe"))
                .body("data[1].attributes.email", equalTo("jane@doe.com"))
                .body("data[1].links.self", equalTo("/users/2"));
    }

    @Test
    public void test_readMultipleWithRelationships() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives", "citizenships", "placeOfBirth")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("data", hasSize(2))
                // user 1 — citizenships
                .body("data[0].relationships.citizenships.data", hasSize(3))
                .body("data[0].relationships.citizenships.links", hasKey("related:countries"))
                // user 1 — placeOfBirth
                .body("data[0].relationships.placeOfBirth.data.id", equalTo("US"))
                .body("data[0].relationships.placeOfBirth.data.type", equalTo("countries"))
                // user 1 — relatives
                .body("data[0].relationships.relatives.data", hasSize(2))
                .body("data[0].relationships.relatives.data.find { it.id == '2' }.meta.relationshipType", equalTo("HUSBAND"))
                .body("data[0].relationships.relatives.data.find { it.id == '3' }.meta.relationshipType", equalTo("BROTHER"))
                // user 2 — citizenships
                .body("data[1].relationships.citizenships.data", hasSize(1))
                .body("data[1].relationships.citizenships.data[0].id", equalTo("US"))
                // user 2 — placeOfBirth
                .body("data[1].relationships.placeOfBirth.data.id", equalTo("FI"))
                // user 2 — relatives
                .body("data[1].relationships.relatives.data", hasSize(2))
                .body("data[1].relationships.relatives.data.find { it.id == '1' }.meta.relationshipType", equalTo("WIFE"))
                .body("data[1].relationships.relatives.data.find { it.id == '4' }.meta.relationshipType", equalTo("SON"));
    }

    @Test
    public void test_filterByIdsWithRelationships() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives", "citizenships", "placeOfBirth")
                .queryParam(FiltersAwareRequest.getFilterParam(ID_FILTER_NAME), "1", "2")
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                // filter returns exactly 2 users, no pagination
                .body("data", hasSize(2))
                .body("data[0].id", equalTo("1"))
                .body("data[0].attributes.fullName", equalTo("John Doe"))
                .body("data[0].relationships.citizenships.data", hasSize(3))
                .body("data[0].relationships.placeOfBirth.data.id", equalTo("US"))
                .body("data[0].relationships.relatives.data", hasSize(2))
                .body("data[1].id", equalTo("2"))
                .body("data[1].attributes.fullName", equalTo("Jane Doe"))
                .body("data[1].relationships.citizenships.data", hasSize(1))
                .body("data[1].relationships.placeOfBirth.data.id", equalTo("FI"))
                .body("data[1].relationships.relatives.data", hasSize(2));
    }

}
