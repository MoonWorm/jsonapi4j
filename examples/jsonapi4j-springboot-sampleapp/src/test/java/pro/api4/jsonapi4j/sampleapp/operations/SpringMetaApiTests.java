package pro.api4.jsonapi4j.sampleapp.operations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import pro.api4.jsonapi4j.sampleapp.testsuite.MetaApiTests;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("metaTest")
@DirtiesContext
public class SpringMetaApiTests extends MetaApiTests {

    public SpringMetaApiTests(@Value("${jsonapi4j.rootPath}") String jsonApiRootPath,
                              @LocalServerPort int appPort) {
        super(jsonApiRootPath, appPort);
    }

}
