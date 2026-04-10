package pro.api4.jsonapi4j.sampleapp.testsuite;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;
import pro.api4.jsonapi4j.request.SparseFieldsetsAwareRequest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

public abstract class SparseFieldsetsOperationsTests {

    private final String jsonApiRootPath;
    private final int serverPort;

    public SparseFieldsetsOperationsTests(String jsonApiRootPath,
                                          int serverPort) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.serverPort = serverPort;
    }

    @Test
    public void test_readByIdWithIncludes() {
        // fields[users]=email, fields[countries]=name — only those attributes should appear
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives", "placeOfBirth.currencies")
                .queryParam(SparseFieldsetsAwareRequest.getFieldsParam("users"), "email")
                .queryParam(SparseFieldsetsAwareRequest.getFieldsParam("countries"), "name")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                // primary resource — only email
                .body("data.attributes.email", equalTo("john@doe.com"))
                .body("data.attributes", not(hasKey("fullName")))
                .body("data.attributes", not(hasKey("creditCardNumber")))
                // relationships still resolved
                .body("data.relationships.placeOfBirth.data.id", equalTo("US"))
                .body("data.relationships.relatives.data", hasSize(2))
                // included users — only email (fields propagated via CD)
                .body("included.findAll { it.type == 'users' }.attributes", everyItem(hasKey("email")))
                .body("included.findAll { it.type == 'users' }.attributes", everyItem(not(hasKey("fullName"))))
                // included country — only name
                .body("included.find { it.id == 'US' }.attributes.name", equalTo("United States"))
                .body("included.find { it.id == 'US' }.attributes", not(hasKey("region")));
    }

    @Test
    public void test_readMultipleWithIncludes() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives", "placeOfBirth.currencies")
                .queryParam(SparseFieldsetsAwareRequest.getFieldsParam("users"), "email")
                .queryParam(SparseFieldsetsAwareRequest.getFieldsParam("countries"), "name")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("data", hasSize(2))
                // all primary users — only email
                .body("data[0].attributes.email", equalTo("john@doe.com"))
                .body("data[0].attributes", not(hasKey("fullName")))
                .body("data[1].attributes.email", equalTo("jane@doe.com"))
                .body("data[1].attributes", not(hasKey("fullName")))
                // included users — only email
                .body("included.findAll { it.type == 'users' }.attributes", everyItem(hasKey("email")))
                .body("included.findAll { it.type == 'users' }.attributes", everyItem(not(hasKey("fullName"))));
    }

    @Test
    public void test_readToOneRelationshipWithIncludes() {
        // fields[countries]=name — included country should only have name
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "placeOfBirth.currencies")
                .queryParam(SparseFieldsetsAwareRequest.getFieldsParam("countries"), "name")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}/relationships/placeOfBirth")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("data.id", equalTo("US"))
                .body("data.type", equalTo("countries"))
                // included country — only name, no region
                .body("included.find { it.id == 'US' }.attributes.name", equalTo("United States"))
                .body("included.find { it.id == 'US' }.attributes", not(hasKey("region")))
                // included currency — not affected by fields[countries], should have all attributes
                .body("included.find { it.id == 'USD' }.attributes.name", equalTo("United States dollar"))
                .body("included.find { it.id == 'USD' }.attributes.symbol", equalTo("$"));
    }

    @Test
    public void test_readById_emptyFieldsParam_reductsAllAttributes() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(SparseFieldsetsAwareRequest.getFieldsParam("users"), "")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("data.id", equalTo("1"))
                .body("data.type", equalTo("users"))
                .body("data", not(hasKey("attributes")));
    }

    @Test
    public void test_readMultiple_emptyFieldsParam_reductsAllAttributes() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(SparseFieldsetsAwareRequest.getFieldsParam("users"), "")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("data[0]", not(hasKey("attributes")))
                .body("data[1]", not(hasKey("attributes")));
    }

    @Test
    public void test_readById_nonExistentField_reductsAllAttributes() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(SparseFieldsetsAwareRequest.getFieldsParam("users"), "nonExistentField")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("data.id", equalTo("1"))
                .body("data.type", equalTo("users"))
                .body("data", not(hasKey("attributes")));
    }

    @Test
    public void test_readMultiple_nonExistentField_reductsAllAttributes() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(SparseFieldsetsAwareRequest.getFieldsParam("users"), "nonExistentField")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("data[0]", not(hasKey("attributes")))
                .body("data[1]", not(hasKey("attributes")));
    }

    @Test
    public void test_readToManyRelationshipWithIncludes() {
        // fields[countries]=name — included country should only have name
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(SparseFieldsetsAwareRequest.getFieldsParam("countries"), "name")
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "citizenships.currencies")
                .pathParam("userId", "5")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}/relationships/citizenships")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("data", hasSize(1))
                .body("data[0].id", equalTo("US"))
                // included country — only name, no region
                .body("included.find { it.id == 'US' }.attributes.name", equalTo("United States"))
                .body("included.find { it.id == 'US' }.attributes", not(hasKey("region")))
                // included currency — not affected by fields[countries]
                .body("included.find { it.id == 'USD' }.attributes.name", equalTo("United States dollar"))
                .body("included.find { it.id == 'USD' }.attributes.symbol", equalTo("$"));
    }

}
