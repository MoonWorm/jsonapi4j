package pro.api4.jsonapi4j.rest.quarkus.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class Jsonapi4jRestQuarkusResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/jsonapi4j-rest-quarkus")
                .then()
                .statusCode(200)
                .body(is("Hello jsonapi4j-rest-quarkus"));
    }
}
