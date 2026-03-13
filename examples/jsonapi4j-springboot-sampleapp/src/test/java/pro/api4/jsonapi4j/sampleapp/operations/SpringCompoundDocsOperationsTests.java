package pro.api4.jsonapi4j.sampleapp.operations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import pro.api4.jsonapi4j.principal.DefaultPrincipalResolver;
import pro.api4.jsonapi4j.sampleapp.testsuite.CompoundDocsOperationsTests;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("compound-docs-test")
public class SpringCompoundDocsOperationsTests extends CompoundDocsOperationsTests {

    public SpringCompoundDocsOperationsTests(@Value("${jsonapi4j.root-path}") String jsonApiRootPath,
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
