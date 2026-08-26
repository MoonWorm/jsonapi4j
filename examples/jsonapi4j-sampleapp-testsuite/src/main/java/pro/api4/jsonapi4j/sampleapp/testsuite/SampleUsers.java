package pro.api4.jsonapi4j.sampleapp.testsuite;

import pro.api4.jsonapi4j.request.JsonApiMediaType;

import static io.restassured.RestAssured.given;

public final class SampleUsers {

    private SampleUsers() {
    }

    public static String createUser(String jsonApiRootPath, int appPort) {
        return createUser(jsonApiRootPath, appPort, "");
    }

    public static String createUser(String jsonApiRootPath, int appPort, String relationshipsJson) {
        String relationships = relationshipsJson.isEmpty() ? "" : ",\n    \"relationships\": " + relationshipsJson;
        return given()
                .header("Content-Type", JsonApiMediaType.MEDIA_TYPE)
                .body("""
                        {
                          "data": {
                            "type": "users",
                            "attributes": {
                              "fullName": "Temporary Subject",
                              "email": "temporary@subject.com",
                              "creditCardNumber": "000000000"
                            }%s
                          }
                        }
                        """.formatted(relationships))
                .post("http://localhost:" + appPort + jsonApiRootPath + "/users")
                .then()
                .statusCode(201)
                .extract()
                .path("data.id");
    }

}
