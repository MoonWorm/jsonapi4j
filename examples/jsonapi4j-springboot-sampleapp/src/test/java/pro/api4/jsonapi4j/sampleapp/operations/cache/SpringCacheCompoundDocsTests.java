package pro.api4.jsonapi4j.sampleapp.operations.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import pro.api4.jsonapi4j.sampleapp.testsuite.CacheCompoundDocsTests;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("cacheTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SpringCacheCompoundDocsTests extends CacheCompoundDocsTests {

    @Autowired
    public SpringCacheCompoundDocsTests(@Value("${jsonapi4j.rootPath}") String jsonApiRootPath,
                                        @LocalServerPort int serverPort,
                                        InvocationTracker invocationTracker) {
        super(jsonApiRootPath, serverPort, invocationTracker);
    }

}
