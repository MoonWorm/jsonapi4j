package pro.api4.jsonapi4j.sampleapp.operations;

import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.nio.charset.StandardCharsets;

import static io.restassured.config.DecoderConfig.decoderConfig;

public abstract class RestAssuredUtf8TestBase {

    @BeforeAll
    public static void configureResponseCharset() {
        RestAssured.config = RestAssuredConfig.config()
                .decoderConfig(decoderConfig().defaultContentCharset(StandardCharsets.UTF_8.name()));
    }

    @AfterAll
    public static void resetResponseCharset() {
        RestAssured.reset();
    }
}
