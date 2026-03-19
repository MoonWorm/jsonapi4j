package pro.api4.jsonapi4j.sampleapp.operations.domain.country;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import pro.api4.jsonapi4j.sampleapp.testsuite.domain.country.ReadCountryByIdOperationTests;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("domainTest")
@DirtiesContext
public class SpringReadCountryByIdOperationTests extends ReadCountryByIdOperationTests {

    public SpringReadCountryByIdOperationTests(@Value("${jsonapi4j.rootPath}") String jsonApiRootPath,
                                               @LocalServerPort int appPort) {
        super(jsonApiRootPath, appPort);
    }

}
