package pro.api4.jsonapi4j.sampleapp.testsuite.domain.user;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public abstract class CreateUserOperationTests {

    private final String jsonApiRootPath;
    private final int appPort;

    public CreateUserOperationTests(String jsonApiRootPath,
                                    int appPort) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.appPort = appPort;
    }

    @Test
    public void test_createUser() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .body("""
                        {
                          "data": {
                            "type": "users",
                            "attributes": {
                              "fullName": "Alice Smith",
                              "email": "alice@smith.com",
                              "creditCardNumber": "999888777"
                            }
                          }
                        }
                        """)
                .post("http://localhost:" + appPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(201)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .header("Location", notNullValue())
                .body("data.type", equalTo("users"))
                .body("data.id", notNullValue())
                .body("data.attributes.fullName", equalTo("Alice Smith"))
                .body("data.attributes.email", equalTo("alice@smith.com"))
                .body("data.attributes.creditCardNumber", equalTo("999888777"));
    }

    @Test
    public void test_createUser_withRelationships() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .body("""
                        {
                          "data": {
                            "type": "users",
                            "attributes": {
                              "fullName": "Bob Jones",
                              "email": "bob@jones.com"
                            },
                            "relationships": {
                              "citizenships": {
                                "data": [
                                  { "type": "countries", "id": "US" },
                                  { "type": "countries", "id": "NO" }
                                ]
                              },
                              "placeOfBirth": {
                                "data": { "type": "countries", "id": "FI" }
                              }
                            }
                          }
                        }
                        """)
                .post("http://localhost:" + appPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(201)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .header("Location", notNullValue())
                .body("data.type", equalTo("users"))
                .body("data.id", notNullValue())
                .body("data.attributes.fullName", equalTo("Bob Jones"))
                .body("data.attributes.email", equalTo("bob@jones.com"));
    }

    @Test
    public void test_createUser_validationError_nullAttributes() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .body("""
                        {
                          "data": {
                            "type": "users"
                          }
                        }
                        """)
                .post("http://localhost:" + appPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(400)
                .body("errors[0].code", equalTo("GENERIC_REQUEST_ERROR"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].detail", equalTo("'attributes' is null"))
                .body("errors[0].id", notNullValue());
    }

    @Test
    public void test_createUser_validationError_nullFullName() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .body("""
                        {
                          "data": {
                            "type": "users",
                            "attributes": {
                              "email": "test@test.com"
                            }
                          }
                        }
                        """)
                .post("http://localhost:" + appPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(400)
                .body("errors[0].code", equalTo("GENERIC_REQUEST_ERROR"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].detail", equalTo("'attributes.fullName' is null"))
                .body("errors[0].id", notNullValue());
    }

    @Test
    public void test_createUser_validationError_invalidEmail() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .body("""
                        {
                          "data": {
                            "type": "users",
                            "attributes": {
                              "fullName": "Test User",
                              "email": "not-an-email"
                            }
                          }
                        }
                        """)
                .post("http://localhost:" + appPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(400)
                .body("errors[0].code", equalTo("GENERIC_REQUEST_ERROR"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].id", notNullValue());
    }

    @Test
    public void test_createUser_withRelatives() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .body("""
                        {
                          "data": {
                            "type": "users",
                            "attributes": {
                              "fullName": "Carol White",
                              "email": "carol@white.com"
                            },
                            "relationships": {
                              "relatives": {
                                "data": [
                                  {
                                    "type": "users",
                                    "id": "1",
                                    "meta": { "relationshipType": "BROTHER" }
                                  },
                                  {
                                    "type": "users",
                                    "id": "2",
                                    "meta": { "relationshipType": "WIFE" }
                                  }
                                ]
                              }
                            }
                          }
                        }
                        """)
                .post("http://localhost:" + appPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(201)
                .contentType(JsonApiMediaType.MEDIA_TYPE)
                .header("Location", notNullValue())
                .body("data.type", equalTo("users"))
                .body("data.id", notNullValue())
                .body("data.attributes.fullName", equalTo("Carol White"))
                .body("data.attributes.email", equalTo("carol@white.com"));
    }

    @Test
    public void test_createUser_validationError_invalidRelativesResourceType() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .body("""
                        {
                          "data": {
                            "type": "users",
                            "attributes": {
                              "fullName": "Test User",
                              "email": "test@user.com"
                            },
                            "relationships": {
                              "relatives": {
                                "data": [
                                  { "type": "wrong-type", "id": "1", "meta": { "relationshipType": "BROTHER" } }
                                ]
                              }
                            }
                          }
                        }
                        """)
                .post("http://localhost:" + appPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(400)
                .body("errors[0].code", equalTo("GENERIC_REQUEST_ERROR"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].detail", equalTo("resource type 'wrong-type' not supported, available resource types: [users]"))
                .body("errors[0].id", notNullValue());
    }

    @Test
    public void test_createUser_validationError_invalidRelationshipMetaType() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .body("""
                        {
                          "data": {
                            "type": "users",
                            "attributes": {
                              "fullName": "Test User",
                              "email": "test@user.com"
                            },
                            "relationships": {
                              "relatives": {
                                "data": [
                                  { "type": "users", "id": "1", "meta": { "relationshipType": "INVALID_TYPE" } }
                                ]
                              }
                            }
                          }
                        }
                        """)
                .post("http://localhost:" + appPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(400)
                .body("errors[0].code", equalTo("GENERIC_REQUEST_ERROR"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].detail", equalTo("Meta 'RelationshipType' object only accepts string values: HUSBAND, WIFE, SON, DAUGHTER, MOTHER, FATHER, BROTHER"))
                .body("errors[0].id", notNullValue());
    }

    @Test
    public void test_createUser_validationError_blankRelativeId() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .body("""
                        {
                          "data": {
                            "type": "users",
                            "attributes": {
                              "fullName": "Test User",
                              "email": "test@user.com"
                            },
                            "relationships": {
                              "relatives": {
                                "data": [
                                  { "type": "users", "id": "", "meta": { "relationshipType": "BROTHER" } }
                                ]
                              }
                            }
                          }
                        }
                        """)
                .post("http://localhost:" + appPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(400)
                .body("errors[0].code", equalTo("GENERIC_REQUEST_ERROR"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].detail", equalTo("resource id can't be blank"))
                .body("errors[0].id", notNullValue());
    }

    @Test
    public void test_createUser_validationError_invalidRelationshipType() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .body("""
                        {
                          "data": {
                            "type": "users",
                            "attributes": {
                              "fullName": "Test User",
                              "email": "test@user.com"
                            },
                            "relationships": {
                              "citizenships": {
                                "data": [
                                  { "type": "wrong-type", "id": "US" }
                                ]
                              }
                            }
                          }
                        }
                        """)
                .post("http://localhost:" + appPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(400)
                .body("errors[0].code", equalTo("GENERIC_REQUEST_ERROR"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].detail", equalTo("resource type 'wrong-type' not supported, available resource types: [countries]"))
                .body("errors[0].id", notNullValue());
    }

}
