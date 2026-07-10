package pro.api4.jsonapi4j.sampleapp.operations;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import pro.api4.jsonapi4j.sampleapp.MetaTestProfile;
import pro.api4.jsonapi4j.sampleapp.testsuite.MetaApiTests;

@QuarkusTest
@TestProfile(MetaTestProfile.class)
public class QuarkusMetaApiTests extends MetaApiTests {

    public QuarkusMetaApiTests(@ConfigProperty(name = "jsonapi4j.rootPath") String jsonApiRootPath,
                              @ConfigProperty(name = "quarkus.http.port") int appPort) {
        super(jsonApiRootPath, appPort);
    }

}
