package pro.api4.jsonapi4j.sampleapp.servlet.operations;

import org.junit.jupiter.api.extension.ExtendWith;
import pro.api4.jsonapi4j.sampleapp.servlet.EmbeddedJettyExtension;
import pro.api4.jsonapi4j.sampleapp.servlet.JettyTestConfig;
import pro.api4.jsonapi4j.sampleapp.testsuite.OasDocumentTests;

@ExtendWith(EmbeddedJettyExtension.class)
@JettyTestConfig("/jsonapi4j-oasTest.yaml")
public class ServletOasDocumentTests extends OasDocumentTests {

    private static final String OAS_ROOT_PATH = EmbeddedJettyExtension.ROOT_PATH + "/oas";

    public ServletOasDocumentTests() {
        super(OAS_ROOT_PATH, EmbeddedJettyExtension.PORT);
    }

}
