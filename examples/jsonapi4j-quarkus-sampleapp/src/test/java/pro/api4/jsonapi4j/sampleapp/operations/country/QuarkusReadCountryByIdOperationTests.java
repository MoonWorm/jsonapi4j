package pro.api4.jsonapi4j.sampleapp.operations.country;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import pro.api4.jsonapi4j.sampleapp.IntegrationTestProfile;
import pro.api4.jsonapi4j.sampleapp.testsuite.country.ReadCountryByIdOperationTests;

@QuarkusTest
@TestProfile(IntegrationTestProfile.class)
public class QuarkusReadCountryByIdOperationTests extends ReadCountryByIdOperationTests {


    public QuarkusReadCountryByIdOperationTests(@ConfigProperty(name = "jsonapi4j.rootPath") String jsonApiRootPath,
                                                @ConfigProperty(name = "quarkus.http.port") int appPort) {
        super(jsonApiRootPath, appPort);
    }

}
