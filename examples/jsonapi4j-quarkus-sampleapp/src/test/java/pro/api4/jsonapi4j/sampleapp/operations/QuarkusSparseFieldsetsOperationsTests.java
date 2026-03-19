package pro.api4.jsonapi4j.sampleapp.operations;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import pro.api4.jsonapi4j.sampleapp.CompoundDocsTestProfile;
import pro.api4.jsonapi4j.sampleapp.testsuite.CompoundDocsOperationsTests;
import pro.api4.jsonapi4j.sampleapp.testsuite.SparseFieldsetsOperationsTests;

@QuarkusTest
@TestProfile(CompoundDocsTestProfile.class)
public class QuarkusSparseFieldsetsOperationsTests extends SparseFieldsetsOperationsTests {

    public QuarkusSparseFieldsetsOperationsTests(@ConfigProperty(name = "jsonapi4j.rootPath") String jsonApiRootPath,
                                                 @ConfigProperty(name = "quarkus.http.port") int appPort) {
        super(jsonApiRootPath, appPort);
    }
}
