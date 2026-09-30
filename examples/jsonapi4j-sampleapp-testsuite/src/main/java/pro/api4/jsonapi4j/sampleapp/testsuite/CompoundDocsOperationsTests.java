package pro.api4.jsonapi4j.sampleapp.testsuite;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;

public abstract class CompoundDocsOperationsTests {

    private final String jsonApiRootPath;
    private final int serverPort;

    public CompoundDocsOperationsTests(String jsonApiRootPath,
                                       int serverPort) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.serverPort = serverPort;
    }

    @Test
    public void test_readByIdWithIncludes() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives", "citizenships", "placeOfBirth.currencies")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("data.id", equalTo("1"))
                .body("data.attributes.fullName", equalTo("John Doe"))
                // relationships resolved
                .body("data.relationships.citizenships.data", hasSize(3))
                .body("data.relationships.placeOfBirth.data.id", equalTo("US"))
                .body("data.relationships.relatives.data", hasSize(2))
                // included resources — countries, currencies, and relative users resolved via CD, deduplicated:
                // - users (relatives): {2, 3}
                // - countries (citizenships ∪ placeOfBirth): {NO, FI, US}
                // - currencies (placeOfBirth only): US -> USD => {USD}
                .body("included", hasSize(6))
                .body("included.findAll { it.type == 'users' }.size()", equalTo(2))
                .body("included.findAll { it.type == 'users' }.id", containsInAnyOrder("2", "3"))
                .body("included.findAll { it.type == 'countries' }.size()", equalTo(3))
                .body("included.findAll { it.type == 'countries' }.id", containsInAnyOrder("NO", "FI", "US"))
                .body("included.findAll { it.type == 'currencies' }.size()", equalTo(1))
                .body("included.findAll { it.type == 'currencies' }.id", containsInAnyOrder("USD"))
                .body("included.find { it.id == 'US' && it.type == 'countries' }.attributes.name", equalTo("United States"))
                .body("included.find { it.id == 'USD' && it.type == 'currencies' }.attributes.name", equalTo("United States dollar"))
                .body("included.find { it.id == '2' && it.type == 'users' }.attributes.fullName", equalTo("Jane Doe"))
                .body("included.find { it.id == '3' && it.type == 'users' }.attributes.fullName", equalTo("Jack Doe"));
    }

    @Test
    public void test_readMultipleWithIncludes() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives", "citizenships", "placeOfBirth.currencies")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("data", hasSize(2))
                .body("data[0].relationships.citizenships.data", hasSize(3))
                .body("data[1].relationships.citizenships.data", hasSize(1))
                // included resources from both users' relationships, deduplicated:
                // - users (relatives): user 1 -> {2, 3}, user 2 -> {1, 4} => {3, 4}; users 1 and 2 are primary data
                // - countries (citizenships ∪ placeOfBirth): user 1 -> {NO, FI, US} + US; user 2 -> {US} + FI => {NO, FI, US}
                // - currencies (placeOfBirth only): user 1 -> US -> USD; user 2 -> FI -> EUR => {USD, EUR}
                .body("included", hasSize(7))
                .body("included.findAll { it.type == 'users' }.size()", equalTo(2))
                .body("included.findAll { it.type == 'users' }.id", containsInAnyOrder("3", "4"))
                .body("included.findAll { it.type == 'countries' }.size()", equalTo(3))
                .body("included.findAll { it.type == 'countries' }.id", containsInAnyOrder("NO", "FI", "US"))
                .body("included.findAll { it.type == 'currencies' }.size()", equalTo(2))
                .body("included.findAll { it.type == 'currencies' }.id", containsInAnyOrder("USD", "EUR"));
    }

    @Test
    public void test_readToOneRelationshipWithIncludes() {
        // include=placeOfBirth.currencies resolves country US -> currency USD
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "placeOfBirth.currencies")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}/relationships/placeOfBirth")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("links.related", equalTo("/countries/US"))
                .body("data.id", equalTo("US"))
                .body("data.type", equalTo("countries"))
                // included — country US with its currencies resolved
                .body("included", hasSize(2))
                .body("included.find { it.id == 'US' }.attributes.name", equalTo("United States"))
                .body("included.find { it.id == 'US' }.relationships.currencies.data[0].id", equalTo("USD"))
                .body("included.find { it.id == 'USD' }.attributes.name", equalTo("United States dollar"))
                .body("included.find { it.id == 'USD' }.attributes.symbol", equalTo("$"));
    }

    @Test
    public void test_readToManyRelationshipWithIncludes() {
        // user 5's citizenships with include=citizenships.currencies
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "citizenships.currencies")
                .pathParam("userId", "5")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}/relationships/citizenships")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("links", hasKey("related:countries"))
                .body("data", hasSize(1))
                .body("data[0].id", equalTo("US"))
                // included — country US + currency USD
                .body("included", hasSize(2))
                .body("included.find { it.id == 'US' }.attributes.name", equalTo("United States"))
                .body("included.find { it.id == 'USD' }.attributes.name", equalTo("United States dollar"));
    }

    @Test
    public void test_readById_maxHopsExceeded_respondsBadRequestNamingThePath() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives.relatives.relatives.relatives")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(400)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("errors[0].code", equalTo("UNSUPPORTED_INCLUDE"))
                .body("errors[0].source.parameter", equalTo("include"))
                .body("errors[0].detail", containsString("relatives.relatives.relatives.relatives"));
    }

    @Test
    public void test_readById_maxHopsExceeded_namesThePathInMeta() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives.relatives.relatives.relatives")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(400)
                .body("errors[0].meta.path", equalTo("relatives.relatives.relatives.relatives"));
    }

    @Test
    public void test_readById_unknownRelationshipFirstInPath_respondsBadRequestNamingThePath() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives,pets.owner")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(400)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("errors", hasSize(1))
                .body("errors[0].code", equalTo("UNSUPPORTED_INCLUDE"))
                .body("errors[0].source.parameter", equalTo("include"))
                .body("errors[0].detail", equalTo("Resource type 'users' has no relationship 'pets'"))
                .body("errors[0].meta.path", equalTo("pets.owner"));
    }

    @Test
    public void test_readById_unknownRelationshipInTheMiddleOfPath_respondsBadRequestNamingTheFullPath() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "placeOfBirth.economy.currencies")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(400)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("errors", hasSize(1))
                .body("errors[0].code", equalTo("UNSUPPORTED_INCLUDE"))
                .body("errors[0].detail", equalTo("Resource type 'countries' has no relationship 'economy'"))
                .body("errors[0].meta.path", equalTo("placeOfBirth.economy"));
    }

    @Test
    public void test_readMultiple_unknownRelationship_respondsBadRequest() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "pets")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(400)
                .body("errors[0].code", equalTo("UNSUPPORTED_INCLUDE"))
                .body("errors[0].meta.path", equalTo("pets"));
    }

    @Test
    public void test_readById_unknownRelationshipWithCompoundDocsDisabled_respondsBadRequest() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .header("X-Disable-Compound-Docs", "true")
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "pets")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(400)
                .body("errors[0].code", equalTo("UNSUPPORTED_INCLUDE"))
                .body("errors[0].meta.path", equalTo("pets"));
    }

    @Test
    public void test_readById_customQueryParamsPropagated() {
        // custom query params should be propagated to internal CD HTTP calls
        // and appear in self-links of included resources
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives")
                .queryParam("customParam", "customValue")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                // user 1's relatives: {2, 3}
                .body("included", hasSize(2))
                .body("included.id", containsInAnyOrder("2", "3"))
                .body("included.links.self", not(empty()))
                .body("included.links.self", everyItem(containsString("customParam")));
    }

    @Test
    public void test_readById_customQueryParamWithReservedCharacters_propagatedSafely() {
        // a space, '&' and '=' in a custom param value must be encoded on the internal CD calls,
        // not break the URL or inject extra parameters
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives")
                .queryParam("customParam", "a b&filter[id]=5")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                // user 1's relatives: {2, 3} - an injected filter[id]=5 would have changed that
                .body("included", hasSize(2))
                .body("included.id", containsInAnyOrder("2", "3"));
    }

    @Test
    public void test_readByIdWithIncludesCheckDeduplication() {
        // 3 hops of relatives — all reachable users should be deduplicated
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .queryParam(IncludeAwareRequest.INCLUDE_PARAM, "relatives.relatives.relatives")
                .pathParam("userId", "1")
                .get("http://localhost:" + serverPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .body("data.id", equalTo("1"))
                .body("data.relationships.relatives.data", hasSize(2))
                // included users deduplicated across data and included — users 2,3,4 (user 1 is primary data)
                .body("included.findAll { it.type == 'users' }.size()", equalTo(3))
                .body("included.findAll { it.type == 'users' }.id", containsInAnyOrder("2", "3", "4"))
                // user 3 has empty relatives
                .body("included.find { it.id == '3' }.relationships.relatives.data", hasSize(0))
                // user 4 has 2 relatives
                .body("included.find { it.id == '4' }.relationships.relatives.data", hasSize(2));
    }

}
