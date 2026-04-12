package pro.api4.jsonapi4j.sampleapp.servlet.operations;

import org.junit.jupiter.api.extension.ExtendWith;
import pro.api4.jsonapi4j.sampleapp.servlet.EmbeddedJettyExtension;
import pro.api4.jsonapi4j.sampleapp.servlet.JettyTestConfig;
import pro.api4.jsonapi4j.sampleapp.testsuite.SparseFieldsetsOperationsTests;

@ExtendWith(EmbeddedJettyExtension.class)
@JettyTestConfig("/jsonapi4j-sparseFieldsetsTest.yaml")
public class ServletSparseFieldsetsOperationsTests extends SparseFieldsetsOperationsTests {

    public ServletSparseFieldsetsOperationsTests() {
        super(EmbeddedJettyExtension.ROOT_PATH, EmbeddedJettyExtension.PORT);
    }

}
