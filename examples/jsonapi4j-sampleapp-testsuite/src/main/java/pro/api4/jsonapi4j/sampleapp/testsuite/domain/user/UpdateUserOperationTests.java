package pro.api4.jsonapi4j.sampleapp.testsuite.domain.user;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
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
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "1")
                .body("""
                        {
                          "data": {
                            "id": "1",
                            "type": "users",
                            "attributes": {
                              "fullName": "John Updated",
                              "email": "john.updated@doe.com"
                            }
                          }
                        }
                        """)
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(202);
    }

    @Test
    public void test_updateUser_attributesOnly_noRelationships() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "2")
                .body("""
                        {
                          "data": {
                            "id": "2",
                            "type": "users",
                            "attributes": {
                              "email": "jane.new@doe.com"
                            }
                          }
                        }
                        """)
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(202);
    }

    @Test
    public void test_updateUser_withRelationships() {
        given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .pathParam("userId", "3")
                .body("""
                        {
                          "data": {
                            "id": "3",
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
                        """)
                .patch("http://localhost:" + appPort + jsonApiRootPath + "/users/{userId}")
                .then()
                .statusCode(202);
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
                .statusCode(202);
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
                .body("errors[0].code", equalTo("GENERIC_REQUEST_ERROR"))
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
                .body("errors[0].code", equalTo("GENERIC_REQUEST_ERROR"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].detail", equalTo("resource type 'wrong-type' not supported, available resource types: [countries]"))
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
                .body("errors[0].code", equalTo("GENERIC_REQUEST_ERROR"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].detail", equalTo("resource type 'wrong-type' not supported, available resource types: [users]"))
                .body("errors[0].id", notNullValue());
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
                .body("errors[0].code", equalTo("GENERIC_REQUEST_ERROR"))
                .body("errors[0].status", equalTo("400"))
                .body("errors[0].detail", equalTo("Meta 'RelationshipType' object only accepts string values: HUSBAND, WIFE, SON, DAUGHTER, MOTHER, FATHER, BROTHER"))
                .body("errors[0].id", notNullValue());
    }

}
