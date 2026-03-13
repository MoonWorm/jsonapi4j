package pro.api4.jsonapi4j.sampleapp.operations.user;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import pro.api4.jsonapi4j.principal.DefaultPrincipalResolver;
import pro.api4.jsonapi4j.sampleapp.IntegrationTestProfile;
import pro.api4.jsonapi4j.sampleapp.testsuite.user.ReadUserCitizenshipsOperationTests;

@QuarkusTest
@TestProfile(IntegrationTestProfile.class)
public class QuarkusReadUserCitizenshipsOperationTests extends ReadUserCitizenshipsOperationTests {

    public QuarkusReadUserCitizenshipsOperationTests(@ConfigProperty(name = "jsonapi4j.rootPath") String jsonApiRootPath,
                                                     @ConfigProperty(name = "quarkus.http.port") int appPort) {
        super(
                jsonApiRootPath,
                appPort,
                DefaultPrincipalResolver.DEFAULT_ACCESS_TIER_HEADER_NAME,
                DefaultPrincipalResolver.DEFAULT_SCOPES_HEADER_NAME,
                DefaultPrincipalResolver.DEFAULT_USER_ID_HEADER_NAME
        );
    }
}
