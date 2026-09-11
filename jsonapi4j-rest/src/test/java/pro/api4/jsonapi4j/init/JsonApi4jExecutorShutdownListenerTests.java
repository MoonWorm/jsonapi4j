package pro.api4.jsonapi4j.init;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.EXECUTOR_SERVICE_ATT_NAME;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.FRAMEWORK_OWNED_EXECUTOR_ATT_NAME;

class JsonApi4jExecutorShutdownListenerTests {

    private final JsonApi4jExecutorShutdownListener sut = new JsonApi4jExecutorShutdownListener();

    private Map<String, Object> attributes;

    private ServletContextEvent event;

    @BeforeEach
    void setUp() {
        attributes = new HashMap<>();
        ServletContext servletContext = mock(ServletContext.class);
        when(servletContext.getAttribute(anyString())).thenAnswer(i -> attributes.get(i.getArgument(0)));
        doAnswer(i -> attributes.put(i.getArgument(0), i.getArgument(1)))
                .when(servletContext).setAttribute(anyString(), org.mockito.ArgumentMatchers.any());
        event = mock(ServletContextEvent.class);
        when(event.getServletContext()).thenReturn(servletContext);
    }

    @Nested
    class FrameworkOwnedExecutor {

        @Test
        void contextDestroyed_executorCreatedByTheFramework_shutsItDown() {
            ExecutorService executorService = Executors.newCachedThreadPool();
            attributes.put(EXECUTOR_SERVICE_ATT_NAME, executorService);
            attributes.put(FRAMEWORK_OWNED_EXECUTOR_ATT_NAME, executorService);

            sut.contextDestroyed(event);

            assertThat(executorService.isShutdown()).isTrue();
        }

        @Test
        void contextDestroyed_executorCreatedByTheFramework_awaitsTermination() {
            ExecutorService executorService = Executors.newCachedThreadPool();
            attributes.put(EXECUTOR_SERVICE_ATT_NAME, executorService);
            attributes.put(FRAMEWORK_OWNED_EXECUTOR_ATT_NAME, executorService);

            sut.contextDestroyed(event);

            assertThat(executorService.isTerminated()).isTrue();
        }
    }

    @Nested
    class ApplicationSuppliedExecutor {

        @Test
        void contextDestroyed_executorSuppliedByTheApplication_leavesItRunning() {
            ExecutorService executorService = Executors.newCachedThreadPool();
            attributes.put(EXECUTOR_SERVICE_ATT_NAME, executorService);

            sut.contextDestroyed(event);

            assertThat(executorService.isShutdown()).isFalse();
            executorService.shutdown();
        }

        @Test
        void contextDestroyed_frameworkExecutorReplacedAfterCreation_leavesTheReplacementRunning() {
            ExecutorService frameworkExecutor = Executors.newCachedThreadPool();
            ExecutorService replacement = Executors.newCachedThreadPool();
            attributes.put(FRAMEWORK_OWNED_EXECUTOR_ATT_NAME, frameworkExecutor);
            attributes.put(EXECUTOR_SERVICE_ATT_NAME, replacement);

            sut.contextDestroyed(event);

            assertThat(replacement.isShutdown()).isFalse();
            frameworkExecutor.shutdown();
            replacement.shutdown();
        }
    }

    @Nested
    class NothingToShutDown {

        @Test
        void contextDestroyed_noExecutorInTheContext_doesNotFail() {
            sut.contextDestroyed(event);

            assertThat(attributes).doesNotContainKey(EXECUTOR_SERVICE_ATT_NAME);
        }

        @Test
        void contextDestroyed_emptyContext_doesNotFail() {
            sut.contextDestroyed(event);

            assertThat(attributes).isEmpty();
        }
    }
}
