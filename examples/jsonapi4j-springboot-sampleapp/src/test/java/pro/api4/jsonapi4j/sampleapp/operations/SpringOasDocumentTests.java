package pro.api4.jsonapi4j.sampleapp.operations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import pro.api4.jsonapi4j.sampleapp.testsuite.OasDocumentTests;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("oasTest")
@DirtiesContext
public class SpringOasDocumentTests extends OasDocumentTests {

    public SpringOasDocumentTests(@Value("${jsonapi4j.oas.oasRootPath}") String oasRootPath,
                                  @LocalServerPort int appPort) {
        super(oasRootPath, appPort);
    }

}
