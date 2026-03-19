package pro.api4.jsonapi4j.sampleapp.operations.domain.country;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import pro.api4.jsonapi4j.sampleapp.DomainTestProfile;
import pro.api4.jsonapi4j.sampleapp.testsuite.domain.country.ReadMultipleCountriesOperationTests;

@QuarkusTest
@TestProfile(DomainTestProfile.class)
public class QuarkusReadMultipleCountriesOperationTests extends ReadMultipleCountriesOperationTests {

    public QuarkusReadMultipleCountriesOperationTests(@ConfigProperty(name = "jsonapi4j.rootPath") String jsonApiRootPath,
                                                      @ConfigProperty(name = "quarkus.http.port") int appPort) {
        super(jsonApiRootPath, appPort);
    }

}
