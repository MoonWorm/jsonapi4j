package pro.api4.jsonapi4j.sampleapp.operations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import pro.api4.jsonapi4j.sampleapp.testsuite.AccessControlOperationsTests;

import static pro.api4.jsonapi4j.principal.DefaultPrincipalResolver.DEFAULT_ACCESS_TIER_HEADER_NAME;
import static pro.api4.jsonapi4j.principal.DefaultPrincipalResolver.DEFAULT_SCOPES_HEADER_NAME;
import static pro.api4.jsonapi4j.principal.DefaultPrincipalResolver.DEFAULT_USER_ID_HEADER_NAME;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("accessControlTests")
@DirtiesContext
public class SpringAccessControlOperationsTests extends AccessControlOperationsTests {

    public SpringAccessControlOperationsTests(@Value("${jsonapi4j.rootPath}") String jsonApiRootPath,
                                              @LocalServerPort int appPort) {
        super(
                jsonApiRootPath,
                appPort,
                DEFAULT_ACCESS_TIER_HEADER_NAME,
                DEFAULT_SCOPES_HEADER_NAME,
                DEFAULT_USER_ID_HEADER_NAME
        );
    }
}
