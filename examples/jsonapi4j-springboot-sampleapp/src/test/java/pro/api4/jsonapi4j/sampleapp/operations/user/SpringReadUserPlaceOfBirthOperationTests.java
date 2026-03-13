package pro.api4.jsonapi4j.sampleapp.operations.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import pro.api4.jsonapi4j.sampleapp.testsuite.user.ReadUserPlaceOfBirthOperationTests;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
public class SpringReadUserPlaceOfBirthOperationTests extends ReadUserPlaceOfBirthOperationTests {

    public SpringReadUserPlaceOfBirthOperationTests(@Value("${jsonapi4j.root-path}") String jsonApiRootPath,
                                                    @LocalServerPort int appPort) {
        super(jsonApiRootPath, appPort);
    }

}
