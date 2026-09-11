package pro.api4.jsonapi4j.rest.quarkus.deployment;

import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * The framework produces its ExecutorService as a @DefaultBean and pairs it with a disposer that shuts the pool
 * down. An application supplying its own executor must still win, and its pool must stay outside the framework's
 * reach - a disposer that outlived its producer would shut down a pool the application may be sharing.
 */
class QuarkusJsonApi4jExecutorOverrideTests {

    static final ExecutorService APPLICATION_EXECUTOR = Executors.newSingleThreadExecutor();

    @RegisterExtension
    static final QuarkusUnitTest sut = new QuarkusUnitTest()
            .withApplicationRoot((JavaArchive jar) -> jar.addClass(ApplicationExecutorProducer.class));

    @Inject
    @Named("jsonApi4jExecutorService")
    ExecutorService executorService;

    @Test
    void executorService_applicationSuppliesItsOwn_replacesTheDefault() {
        assertThat(executorService).isSameAs(APPLICATION_EXECUTOR);
    }

    @Test
    void executorService_applicationSuppliesItsOwn_isNotShutDownByTheFramework() {
        assertThat(APPLICATION_EXECUTOR.isShutdown()).isFalse();
    }

    public static class ApplicationExecutorProducer {

        @Produces
        @Named("jsonApi4jExecutorService")
        @Singleton
        ExecutorService applicationExecutorService() {
            return APPLICATION_EXECUTOR;
        }
    }
}
