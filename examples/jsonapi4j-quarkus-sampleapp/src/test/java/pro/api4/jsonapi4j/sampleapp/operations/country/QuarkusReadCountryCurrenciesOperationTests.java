package pro.api4.jsonapi4j.sampleapp.operations.country;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;
import pro.api4.jsonapi4j.sampleapp.IntegrationTestProfile;
import pro.api4.jsonapi4j.sampleapp.testsuite.country.ReadCountryCurrenciesOperationTests;
import pro.api4.jsonapi4j.sampleapp.util.ResourceUtil;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;

@QuarkusTest
@TestProfile(IntegrationTestProfile.class)
public class QuarkusReadCountryCurrenciesOperationTests extends ReadCountryCurrenciesOperationTests {

    public QuarkusReadCountryCurrenciesOperationTests(@ConfigProperty(name = "jsonapi4j.rootPath") String jsonApiRootPath,
                                                      @ConfigProperty(name = "quarkus.http.port") int appPort) {
        super(jsonApiRootPath, appPort);
    }

}
