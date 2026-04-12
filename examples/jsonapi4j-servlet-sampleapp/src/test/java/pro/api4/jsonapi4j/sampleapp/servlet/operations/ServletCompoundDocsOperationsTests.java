package pro.api4.jsonapi4j.sampleapp.servlet.operations;

import org.junit.jupiter.api.extension.ExtendWith;
import pro.api4.jsonapi4j.sampleapp.servlet.EmbeddedJettyExtension;
import pro.api4.jsonapi4j.sampleapp.servlet.JettyTestConfig;
import pro.api4.jsonapi4j.sampleapp.testsuite.CompoundDocsOperationsTests;

@ExtendWith(EmbeddedJettyExtension.class)
@JettyTestConfig("/jsonapi4j-compoundDocsTest.yaml")
public class ServletCompoundDocsOperationsTests extends CompoundDocsOperationsTests {

    public ServletCompoundDocsOperationsTests() {
        super(EmbeddedJettyExtension.ROOT_PATH, EmbeddedJettyExtension.PORT);
    }

}
