package pro.api4.jsonapi4j.sampleapp.operations;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import pro.api4.jsonapi4j.sampleapp.OasTestProfile;
import pro.api4.jsonapi4j.sampleapp.testsuite.OasDocumentTests;

@QuarkusTest
@TestProfile(OasTestProfile.class)
public class QuarkusOasDocumentTests extends OasDocumentTests {

    public QuarkusOasDocumentTests(@ConfigProperty(name = "jsonapi4j.oas.oasRootPath") String oasRootPath,
                                   @ConfigProperty(name = "quarkus.http.port") int appPort) {
        super(oasRootPath, appPort);
    }

}
