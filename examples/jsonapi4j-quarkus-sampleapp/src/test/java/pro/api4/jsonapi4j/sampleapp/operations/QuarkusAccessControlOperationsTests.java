package pro.api4.jsonapi4j.sampleapp.operations;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import pro.api4.jsonapi4j.sampleapp.AccessControlTestProfile;
import pro.api4.jsonapi4j.sampleapp.testsuite.AccessControlOperationsTests;

import static pro.api4.jsonapi4j.principal.DefaultPrincipalResolver.DEFAULT_ACCESS_TIER_HEADER_NAME;
import static pro.api4.jsonapi4j.principal.DefaultPrincipalResolver.DEFAULT_SCOPES_HEADER_NAME;
import static pro.api4.jsonapi4j.principal.DefaultPrincipalResolver.DEFAULT_USER_ID_HEADER_NAME;

@QuarkusTest
@TestProfile(AccessControlTestProfile.class)
public class QuarkusAccessControlOperationsTests extends AccessControlOperationsTests {

    public QuarkusAccessControlOperationsTests(@ConfigProperty(name = "jsonapi4j.rootPath") String jsonApiRootPath,
                                               @ConfigProperty(name = "quarkus.http.port") int appPort) {
        super(
                jsonApiRootPath,
                appPort,
                DEFAULT_ACCESS_TIER_HEADER_NAME,
                DEFAULT_SCOPES_HEADER_NAME,
                DEFAULT_USER_ID_HEADER_NAME
        );
    }
}
