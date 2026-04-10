package pro.api4.jsonapi4j.sampleapp.testsuite;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.FiltersAwareRequest;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static pro.api4.jsonapi4j.operation.ReadMultipleResourcesOperation.ID_FILTER_NAME;

public abstract class AccessControlOperationsTests {

    private final String jsonApiRootPath;
    private final int serverPort;

    private final String defaultAccessTierHeaderName;
    private final String defaultScopesHeaderName;
    private final String defaultUserIdHeaderName;

    public AccessControlOperationsTests(String jsonApiRootPath,
                                      int serverPort,
                                      String defaultAccessTierHeaderName,
                                      String defaultScopesHeaderName,
                                      String defaultUserIdHeaderName) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.serverPort = serverPort;
        this.defaultAccessTierHeaderName = defaultAccessTierHeaderName;
        this.defaultScopesHeaderName = defaultScopesHeaderName;
        this.defaultUserIdHeaderName = defaultUserIdHeaderName;
    }

    @Test
    public void test_readById() {
        // user 2 reads user 1 — not the owner, so creditCardNumber should be hidden
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultUserIdHeaderName, "2")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("data.id", equalTo("1"))
                .body("data.type", equalTo("users"))
                .body("data.attributes.fullName", equalTo("John Doe"))
                .body("data.attributes.email", equalTo("john@doe.com"))
                .body("data.attributes", not(hasKey("creditCardNumber")))
                .body("data.links.self", equalTo("/users/1"));
    }

    @Test
    public void test_readByIdWithAccessToSensitiveData() {
        // user 1 reads own data with sensitive scope — creditCardNumber should be visible
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultScopesHeaderName, "users.sensitive.read")
                .header(defaultUserIdHeaderName, "1")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("data.id", equalTo("1"))
                .body("data.attributes.fullName", equalTo("John Doe"))
                .body("data.attributes.email", equalTo("john@doe.com"))
                .body("data.attributes.creditCardNumber", equalTo("123456789"));
    }

    @Test
    public void test_readByIdWithAllRelationships() {
        // user 2 reads user 1 with relationships
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultUserIdHeaderName, "2")
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives", "citizenships", "placeOfBirth")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("data.id", equalTo("1"))
                .body("data.attributes", not(hasKey("creditCardNumber")))
                // citizenships — AC denies (requires scope + ownership), so no data, only links
                .body("data.relationships.citizenships", not(hasKey("data")))
                .body("data.relationships.citizenships.links.self", notNullValue())
                // placeOfBirth — no AC restriction, data resolved
                .body("data.relationships.placeOfBirth.data.id", equalTo("US"))
                // relatives — no AC restriction, data resolved
                .body("data.relationships.relatives.data", hasSize(2));
    }

    @Test
    public void test_readCitizenshipsRelationshipAcPassed() {
        // user 5 (owner) with citizenships scope — should see citizenships data
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultScopesHeaderName, "users.citizenships.read")
                .header(defaultUserIdHeaderName, "5")
                .pathParam("userId", "5")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}/relationships/citizenships")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("links", hasKey("related:countries"))
                .body("data", hasSize(1))
                .body("data[0].id", equalTo("US"))
                .body("data[0].type", equalTo("countries"));
    }

    @Test
    public void test_readCitizenshipsRelationshipAcNotPassed() {
        // user 555 (non-owner) with citizenships scope — ownership fails, should get empty response
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultScopesHeaderName, "users.citizenships.read")
                .header(defaultUserIdHeaderName, "555")
                .pathParam("userId", "5")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}/relationships/citizenships")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("links.self", notNullValue())
                .body("$", not(hasKey("data")));
    }

    @Test
    public void test_readMultiple() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultUserIdHeaderName, "2")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("data", hasSize(2))
                .body("data[0].id", equalTo("1"))
                .body("data[0].attributes", not(hasKey("creditCardNumber")))
                .body("data[1].id", equalTo("2"))
                .body("data[1].attributes", not(hasKey("creditCardNumber")));
    }

    @Test
    public void test_readMultipleWithRelationships() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultUserIdHeaderName, "2")
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives", "citizenships", "placeOfBirth")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("data", hasSize(2))
                .body("data[0].attributes", not(hasKey("creditCardNumber")))
                // citizenships AC-denied for non-owner
                .body("data[0].relationships.citizenships", not(hasKey("data")))
                .body("data[0].relationships.placeOfBirth.data.id", equalTo("US"))
                .body("data[0].relationships.relatives.data", hasSize(2))
                .body("data[1].attributes", not(hasKey("creditCardNumber")))
                .body("data[1].relationships.citizenships", not(hasKey("data")))
                .body("data[1].relationships.placeOfBirth.data.id", equalTo("FI"))
                .body("data[1].relationships.relatives.data", hasSize(2));
    }

    @Test
    public void test_readMultipleWithRelationshipsWithAccessToSensitiveData() {
        // user 1 with sensitive scope — own creditCardNumber visible, others hidden
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultScopesHeaderName, "users.sensitive.read")
                .header(defaultUserIdHeaderName, "1")
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives", "citizenships", "placeOfBirth")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("data", hasSize(2))
                // user 1 (owner) — creditCardNumber visible
                .body("data[0].id", equalTo("1"))
                .body("data[0].attributes.creditCardNumber", equalTo("123456789"))
                // user 2 (not owner) — creditCardNumber hidden
                .body("data[1].id", equalTo("2"))
                .body("data[1].attributes", not(hasKey("creditCardNumber")));
    }

    @Test
    public void test_headersPropagatedThroughCompoundDocs_sensitiveDataVisibleInIncluded() {
        // User 1 requests user 2's data with include=relatives.
        // User 2's relatives are user 1 and user 4.
        // AC headers (scopes=users.sensitive.read, userId=1) propagate through CD internal HTTP calls.
        // So in the included section, user 1 (the owner) should have creditCardNumber visible
        // because the scope+ownership headers were propagated to the internal fetch.
        // User 2 (primary) and user 4 (included) should NOT have creditCardNumber
        // because user 1 is not their owner.
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultScopesHeaderName, "users.sensitive.read")
                .header(defaultUserIdHeaderName, "1")
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives")
                .pathParam("userId", "2")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                // primary resource (user 2) — user 1 is NOT the owner, so no creditCardNumber
                .body("data.attributes", not(hasKey("creditCardNumber")))
                // included user 1 — user 1 IS the owner, headers propagated via CD, so creditCardNumber visible
                .body("included.find { it.id == '1' }.attributes.creditCardNumber", equalTo("123456789"))
                // included user 4 — user 1 is NOT the owner, so no creditCardNumber
                .body("included.find { it.id == '4' }.attributes", not(hasKey("creditCardNumber")));
    }

    @Test
    public void test_filterByIdsWithRelationships() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultUserIdHeaderName, "3")
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives", "citizenships", "placeOfBirth")
                .queryParam(FiltersAwareRequest.getFilterParam(ID_FILTER_NAME), "1", "2")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("data", hasSize(2))
                .body("data[0].id", equalTo("1"))
                .body("data[0].attributes", not(hasKey("creditCardNumber")))
                .body("data[0].relationships.citizenships", not(hasKey("data")))
                .body("data[0].relationships.placeOfBirth.data.id", equalTo("US"))
                .body("data[1].id", equalTo("2"))
                .body("data[1].attributes", not(hasKey("creditCardNumber")))
                .body("data[1].relationships.citizenships", not(hasKey("data")))
                .body("data[1].relationships.placeOfBirth.data.id", equalTo("FI"));
    }

    @Test
    public void test_updateUser_acDenied_nonOwner() {
        // user 2 tries to update user 1 — ownership check should deny
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultUserIdHeaderName, "2")
                .pathParam("userId", "1")
                .body("""
                        {
                          "data": {
                            "id": "1",
                            "type": "users",
                            "attributes": {
                              "email": "hacked@evil.com"
                            }
                          }
                        }
                        """)
                .patch("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(403);

        // verify user 1's email was NOT changed
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultUserIdHeaderName, "1")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .body("data.attributes.email", equalTo("john@doe.com"));
    }

    @Test
    public void test_updateCitizenships_acDenied_nonOwner() {
        // user 2 tries to update user 1's citizenships — ownership check should deny
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultUserIdHeaderName, "2")
                .pathParam("userId", "1")
                .body("""
                        {
                          "data": [
                            { "type": "countries", "id": "TG" }
                          ]
                        }
                        """)
                .patch("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}/relationships/citizenships")
                .then()
                .statusCode(403);

        // verify user 1's citizenships were NOT changed (should still include US)
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultScopesHeaderName, "users.citizenships.read")
                .header(defaultUserIdHeaderName, "1")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}/relationships/citizenships")
                .then()
                .statusCode(200)
                .body("data[0].type", equalTo("countries"));
    }

    @Test
    public void test_updatePlaceOfBirth_acDenied_nonOwner() {
        // user 2 tries to update user 1's placeOfBirth — ownership check should deny
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultUserIdHeaderName, "2")
                .pathParam("userId", "1")
                .body("""
                        {
                          "data": { "type": "countries", "id": "TG" }
                        }
                        """)
                .patch("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}/relationships/placeOfBirth")
                .then()
                .statusCode(403);

        // verify user 1's placeOfBirth was NOT changed (should still be US)
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultUserIdHeaderName, "1")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}/relationships/placeOfBirth")
                .then()
                .statusCode(200)
                .body("data.id", equalTo("US"));
    }

    @Test
    public void test_updateRelatives_acDenied_nonOwner() {
        // user 2 tries to update user 1's relatives — ownership check should deny
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultUserIdHeaderName, "2")
                .pathParam("userId", "1")
                .body("""
                        {
                          "data": [
                            { "type": "users", "id": "5", "meta": { "relationshipType": "BROTHER" } }
                          ]
                        }
                        """)
                .patch("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}/relationships/relatives")
                .then()
                .statusCode(403);

        // verify user 1's relatives were NOT changed (should still have 2 relatives)
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultUserIdHeaderName, "1")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}/relationships/relatives")
                .then()
                .statusCode(200)
                .body("data.size()", equalTo(2));
    }

    @Test
    public void test_deleteUser_acDenied_nonAdmin() {
        // non-admin user tries to delete — admin tier check should deny
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultUserIdHeaderName, "1")
                .pathParam("userId", "3")
                .delete("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(403);

        // verify user 3 still exists
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header(defaultUserIdHeaderName, "3")
                .pathParam("userId", "3")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .body("data.id", equalTo("3"));
    }

}
