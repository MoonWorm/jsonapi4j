package pro.api4.jsonapi4j.sampleapp.servlet.operations.domain.user;

import org.junit.jupiter.api.extension.ExtendWith;
import pro.api4.jsonapi4j.sampleapp.servlet.EmbeddedJettyExtension;
import pro.api4.jsonapi4j.sampleapp.servlet.JettyTestConfig;
import pro.api4.jsonapi4j.sampleapp.testsuite.domain.user.ReadUserByIdOperationTests;

@ExtendWith(EmbeddedJettyExtension.class)
@JettyTestConfig("/jsonapi4j-domainTest.yaml")
public class ServletReadUserByIdOperationTests extends ReadUserByIdOperationTests {

    public ServletReadUserByIdOperationTests() {
        super(EmbeddedJettyExtension.ROOT_PATH, EmbeddedJettyExtension.PORT);
    }

}
