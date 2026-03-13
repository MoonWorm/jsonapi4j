package pro.api4.jsonapi4j.sampleapp.operations.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import pro.api4.jsonapi4j.principal.DefaultPrincipalResolver;
import pro.api4.jsonapi4j.sampleapp.testsuite.user.ReadUserCitizenshipsOperationTests;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
public class SpringReadUserCitizenshipsOperationTests extends ReadUserCitizenshipsOperationTests {

    public SpringReadUserCitizenshipsOperationTests(@Value("${jsonapi4j.root-path}") String jsonApiRootPath,
                                                    @LocalServerPort int appPort) {
        super(
                jsonApiRootPath,
                appPort,
                DefaultPrincipalResolver.DEFAULT_ACCESS_TIER_HEADER_NAME,
                DefaultPrincipalResolver.DEFAULT_SCOPES_HEADER_NAME,
                DefaultPrincipalResolver.DEFAULT_USER_ID_HEADER_NAME
        );
    }
}
