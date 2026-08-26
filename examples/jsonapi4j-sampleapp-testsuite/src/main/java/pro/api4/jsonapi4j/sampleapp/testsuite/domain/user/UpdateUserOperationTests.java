package pro.api4.jsonapi4j.sampleapp.testsuite.domain.user;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import static io.restassured.RestAssured.given;
import static pro.api4.jsonapi4j.sampleapp.testsuite.SampleUsers.createUser;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;

public abstract class UpdateUserOperationTests {

    private final String jsonApiRootPath;
    private final int appPort;

    public UpdateUserOperationTests(String jsonApiRootPath,
                                    int appPort) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.appPort = appPort;
    }

    @Test
    public void test_updateUser_attributes() {
        String userId = createUser(jsonApiRootPath, appPort);

        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", userId)
                .body("""
                        {
                          "data": {
                            "id": "%s",
                            "type": "users",
                            "attributes": {
                              "fullName": "John Updated",
                              "email": "john.updated@doe.com"
                            }
                          }
                        }
                        """.formatted(userId))
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(204);
    }

    @Test
    public void test_updateUser_attributesOnly_noRelationships() {
        String userId = createUser(jsonApiRootPath, appPort);

        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", userId)
                .body("""
                        {
                          "data": {
                            "id": "%s",
                            "type": "users",
                            "attributes": {
                              "email": "jane.new@doe.com"
                            }
                          }
                        }
                        """.formatted(userId))
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(204);
    }

    @Test
    public void test_updateUser_withRelationships() {
        String userId = createUser(jsonApiRootPath, appPort);

        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", userId)
                .body("""
                        {
                          "data": {
                            "id": "%s",
                            "type": "users",
                            "attributes": {
                              "fullName": "Jack Updated",
                              "email": "jack.updated@doe.com"
                            },
                            "relationships": {
                              "citizenships": {
                                "data": [
                                  { "type": "countries", "id": "NO" }
                                ]
                              },
                              "placeOfBirth": {
                                "data": { "type": "countries", "id": "US" }
                              }
                            }
                          }
                        }
                        """.formatted(userId))
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(204);
    }

    @Test
    public void test_updateUser_withRelatives() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "4")
                .body("""
                        {
                          "data": {
                            "id": "4",
                            "type": "users",
                            "relationships": {
                              "relatives": {
                                "data": [
                                  {
                                    "type": "users",
                                    "id": "1",
                                    "meta": { "relationshipType": "FATHER" }
                                  },
                                  {
                                    "type": "users",
                                    "id": "3",
                                    "meta": { "relationshipType": "BROTHER" }
                                  }
                                ]
                              }
                            }
                          }
                        }
                        """)
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(204);
    }

    @Test
    public void test_updateUser_validationError_invalidEmail() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "1")
                .body("""
                        {
                          "data": {
                            "id": "1",
                            "type": "users",
                            "attributes": {
                              "email": "not-an-email"
                            }
                          }
                        }
                        """)
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(400)
                .body("errors[0].code", equalTo("VALUE_INVALID_FORMAT"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].id", notNullValue());
    }

    @Test
    public void test_updateUser_validationError_invalidRelationshipType() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "1")
                .body("""
                        {
                          "data": {
                            "id": "1",
                            "type": "users",
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
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(400)
                .body("errors[0].code", equalTo("INVALID_ENUM_VALUE"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].detail", equalTo("'wrong-type' value is not allowed, available values: [countries]"))
                .body("errors[0].source.pointer", equalTo("/data/relationships/citizenships/data/0/type"))
                .body("errors[0].id", notNullValue());
    }

    @Test
    public void test_updateUser_validationError_invalidRelativesResourceType() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "1")
                .body("""
                        {
                          "data": {
                            "id": "1",
                            "type": "users",
                            "relationships": {
                              "relatives": {
                                "data": [
                                  { "type": "wrong-type", "id": "2", "meta": { "relationshipType": "BROTHER" } }
                                ]
                              }
                            }
                          }
                        }
                        """)
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(400)
                .body("errors[0].code", equalTo("INVALID_ENUM_VALUE"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].detail", equalTo("'wrong-type' value is not allowed, available values: [users]"))
                .body("errors[0].source.pointer", equalTo("/data/relationships/relatives/data/0/type"))
                .body("errors[0].id", notNullValue());
    }

    @Test
    public void test_updateUser_validationError_multipleErrors_invalidCitizenshipAndPlaceOfBirthType() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "1")
                .body("""
                        {
                          "data": {
                            "id": "1",
                            "type": "users",
                            "relationships": {
                              "citizenships": {
                                "data": [
                                  { "type": "wrong-type", "id": "US" }
                                ]
                              },
                              "placeOfBirth": {
                                "data": { "type": "wrong-type", "id": "FI" }
                              }
                            }
                          }
                        }
                        """)
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(400)
                .body("errors", hasSize(2))
                .body("errors[0].code", equalTo("INVALID_ENUM_VALUE"))
                .body("errors[0].source.pointer", equalTo("/data/relationships/placeOfBirth/data/type"))
                .body("errors[1].code", equalTo("INVALID_ENUM_VALUE"))
                .body("errors[1].source.pointer", equalTo("/data/relationships/citizenships/data/0/type"));
    }

    @Test
    public void test_updateUser_validationError_invalidRelationshipMetaType() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "1")
                .body("""
                        {
                          "data": {
                            "id": "1",
                            "type": "users",
                            "relationships": {
                              "relatives": {
                                "data": [
                                  { "type": "users", "id": "2", "meta": { "relationshipType": "INVALID_TYPE" } }
                                ]
                              }
                            }
                          }
                        }
                        """)
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(400)
                .body("errors[0].code", equalTo("INVALID_ENUM_VALUE"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].detail", equalTo("Meta 'relationshipType' object only accepts string values: HUSBAND, WIFE, SON, DAUGHTER, MOTHER, FATHER, BROTHER"))
                .body("errors[0].id", notNullValue());
    }

    private String givenAUser(String creditCardNumber) {
        return given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .body("""
                        {
                          "data": {
                            "type": "users",
                            "attributes": {
                              "fullName": "Partial Update",
                              "email": "partial@update.com",
                              "creditCardNumber": "%s"
                            },
                            "relationships": {
                              "citizenships": { "data": [ { "type": "countries", "id": "US" } ] },
                              "placeOfBirth": { "data": { "type": "countries", "id": "NO" } }
                            }
                          }
                        }
                        """.formatted(creditCardNumber))
                .post("http://localhost:" + appPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(201)
                .extract()
                .path("data.id");
    }

    private void patchUser(String userId, String body) {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", userId)
                .body(body)
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(204);
    }

    @Test
    public void test_updateUser_omittedAttribute_keepsItsCurrentValue() {
        String userId = givenAUser("555456789");

        patchUser(userId, """
                {
                  "data": {
                    "id": "%s",
                    "type": "users",
                    "attributes": {
                      "email": "renamed@update.com"
                    }
                  }
                }
                """.formatted(userId));

        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", userId)
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .body("data.attributes.email", equalTo("renamed@update.com"))
                .body("data.attributes.fullName", equalTo("Partial Update"))
                .body("data.attributes.creditCardNumber", equalTo("555456789"));
    }

    @Test
    public void test_updateUser_explicitNullAttribute_clearsIt() {
        String userId = givenAUser("444456789");

        patchUser(userId, """
                {
                  "data": {
                    "id": "%s",
                    "type": "users",
                    "attributes": {
                      "creditCardNumber": null
                    }
                  }
                }
                """.formatted(userId));

        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", userId)
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .body("data.attributes", not(hasKey("creditCardNumber")))
                .body("data.attributes.email", equalTo("partial@update.com"));
    }

    @Test
    public void test_updateUser_explicitNullOnRequiredAttribute_isRejected() {
        String userId = givenAUser("333456789");

        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", userId)
                .body("""
                        {
                          "data": {
                            "id": "%s",
                            "type": "users",
                            "attributes": {
                              "email": null
                            }
                          }
                        }
                        """.formatted(userId))
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(400)
                .body("errors[0].code", equalTo("VALUE_IS_ABSENT"))
                .body("errors[0].source.pointer", equalTo("/data/attributes/email"))
                .body("errors[0].id", notNullValue());
    }

    @Test
    public void test_updateUser_emptyToManyRelationship_clearsIt() {
        String userId = givenAUser("222456789");

        patchUser(userId, """
                {
                  "data": {
                    "id": "%s",
                    "type": "users",
                    "relationships": {
                      "citizenships": { "data": [] }
                    }
                  }
                }
                """.formatted(userId));

        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", userId)
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/citizenships")
                .then()
                .statusCode(200)
                .body("data", empty());
    }

    @Test
    public void test_updateUser_nullToOneRelationship_clearsItAndKeepsTheUser() {
        String userId = givenAUser("111456789");

        patchUser(userId, """
                {
                  "data": {
                    "id": "%s",
                    "type": "users",
                    "relationships": {
                      "placeOfBirth": { "data": null }
                    }
                  }
                }
                """.formatted(userId));

        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", userId)
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .body("data.id", equalTo(userId));

        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", userId)
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}/relationships/placeOfBirth")
                .then()
                .statusCode(200)
                .body("data", nullValue());
    }


    @Test
    public void test_updateUser_typeNotBelongingToTheEndpoint_isConflict() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "1")
                .body("""
                        {
                          "data": {
                            "id": "1",
                            "type": "countries",
                            "attributes": { "email": "john@doe.com" }
                          }
                        }
                        """)
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(409)
                .body("errors[0].code", equalTo("CONFLICT"))
                .body("errors[0].status", equalTo("409"));
    }

    @Test
    public void test_updateUser_idNotMatchingThePath_isConflict() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "1")
                .body("""
                        {
                          "data": {
                            "id": "2",
                            "type": "users",
                            "attributes": { "email": "john@doe.com" }
                          }
                        }
                        """)
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(409)
                .body("errors[0].code", equalTo("CONFLICT"))
                .body("errors[0].status", equalTo("409"));
    }

    @Test
    public void test_updateUser_addresses_replaceInFull() {
        String userId = givenAUser("222456789");

        patchUser(userId, """
                {
                  "data": {
                    "id": "%s",
                    "type": "users",
                    "attributes": {
                      "addresses": [ { "city": "Tromso", "zip": "9008", "doorCode": "4321" } ]
                    }
                  }
                }
                """.formatted(userId));

        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", userId)
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .body("data.attributes.addresses", hasSize(1))
                .body("data.attributes.addresses[0].city", equalTo("Tromso"))
                .body("data.attributes.addresses[0].doorCode", equalTo("4321"));
    }

    @Test
    public void test_updateUser_emptyAddresses_removesThemAll() {
        String userId = givenAUser("222456789");

        patchUser(userId, """
                {
                  "data": {
                    "id": "%s",
                    "type": "users",
                    "attributes": { "addresses": [] }
                  }
                }
                """.formatted(userId));

        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", userId)
                .get("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(200)
                .body("data.attributes.addresses", empty());
    }
}
