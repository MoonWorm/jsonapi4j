package pro.api4.jsonapi4j.sampleapp.operations.domain.user;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import pro.api4.jsonapi4j.sampleapp.DomainTestProfile;
import pro.api4.jsonapi4j.sampleapp.testsuite.domain.user.DeleteUserCitizenshipsOperationTests;

@QuarkusTest
@TestProfile(DomainTestProfile.class)
public class QuarkusDeleteUserCitizenshipsOperationTests extends DeleteUserCitizenshipsOperationTests {

    public QuarkusDeleteUserCitizenshipsOperationTests(@ConfigProperty(name = "jsonapi4j.rootPath") String jsonApiRootPath,
                                                        @ConfigProperty(name = "quarkus.http.port") int appPort) {
        super(jsonApiRootPath, appPort);
    }

}
