package pro.api4.jsonapi4j.sampleapp.servlet.operations;

import org.junit.jupiter.api.extension.ExtendWith;
import pro.api4.jsonapi4j.sampleapp.servlet.EmbeddedJettyExtension;
import pro.api4.jsonapi4j.sampleapp.servlet.JettyTestConfig;
import pro.api4.jsonapi4j.sampleapp.testsuite.AccessControlOperationsTests;

import static pro.api4.jsonapi4j.principal.DefaultPrincipalResolver.DEFAULT_ACCESS_TIER_HEADER_NAME;
import static pro.api4.jsonapi4j.principal.DefaultPrincipalResolver.DEFAULT_SCOPES_HEADER_NAME;
import static pro.api4.jsonapi4j.principal.DefaultPrincipalResolver.DEFAULT_USER_ID_HEADER_NAME;

@ExtendWith(EmbeddedJettyExtension.class)
@JettyTestConfig("/jsonapi4j-accessControlTest.yaml")
public class ServletAccessControlOperationsTests extends AccessControlOperationsTests {

    public ServletAccessControlOperationsTests() {
        super(
                EmbeddedJettyExtension.ROOT_PATH,
                EmbeddedJettyExtension.PORT,
                DEFAULT_ACCESS_TIER_HEADER_NAME,
                DEFAULT_SCOPES_HEADER_NAME,
                DEFAULT_USER_ID_HEADER_NAME
        );
    }

}
