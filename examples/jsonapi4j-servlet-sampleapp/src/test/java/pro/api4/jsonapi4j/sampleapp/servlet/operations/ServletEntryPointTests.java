package pro.api4.jsonapi4j.sampleapp.servlet.operations;

import org.junit.jupiter.api.extension.ExtendWith;
import pro.api4.jsonapi4j.sampleapp.servlet.EmbeddedJettyExtension;
import pro.api4.jsonapi4j.sampleapp.servlet.JettyTestConfig;
import pro.api4.jsonapi4j.sampleapp.testsuite.EntryPointTests;

@ExtendWith(EmbeddedJettyExtension.class)
@JettyTestConfig("/jsonapi4j-domainTest.yaml")
public class ServletEntryPointTests extends EntryPointTests {

    public ServletEntryPointTests() {
        super(EmbeddedJettyExtension.ROOT_PATH, EmbeddedJettyExtension.PORT);
    }

}
