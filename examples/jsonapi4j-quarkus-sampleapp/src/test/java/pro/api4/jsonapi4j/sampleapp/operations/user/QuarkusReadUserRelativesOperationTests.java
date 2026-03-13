package pro.api4.jsonapi4j.sampleapp.operations.user;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import pro.api4.jsonapi4j.sampleapp.IntegrationTestProfile;
import pro.api4.jsonapi4j.sampleapp.testsuite.user.ReadUserRelativesOperationTests;

@QuarkusTest
@TestProfile(IntegrationTestProfile.class)
public class QuarkusReadUserRelativesOperationTests extends ReadUserRelativesOperationTests {

    public QuarkusReadUserRelativesOperationTests(@ConfigProperty(name = "jsonapi4j.rootPath") String jsonApiRootPath,
                                                  @ConfigProperty(name = "quarkus.http.port") int appPort) {
        super(jsonApiRootPath, appPort);
    }

}
