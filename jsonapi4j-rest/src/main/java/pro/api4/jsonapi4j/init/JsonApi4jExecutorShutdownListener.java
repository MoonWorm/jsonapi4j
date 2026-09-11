package pro.api4.jsonapi4j.init;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.EXECUTOR_SERVICE_ATT_NAME;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.FRAMEWORK_OWNED_EXECUTOR_ATT_NAME;

/**
 * Shuts down the {@link ExecutorService} the framework created for relationship resolution, when the servlet
 * context is destroyed.
 * <p>
 * The default pool is {@code Executors.newCachedThreadPool()}, whose threads are non-daemon and linger for a
 * keepalive period after their last task. In an executable jar that is invisible, because the JVM exits and
 * takes the threads with it. In a war deployed to a shared container it is not: on undeploy the threads outlive
 * the application, the container reports a webapp that "failed to stop" a thread it started, and the old webapp
 * classloader stays reachable through them - so repeated redeploys leak a classloader each time.
 * <p>
 * Only a framework-created pool is shut down. An application that supplies its own {@code ExecutorService}
 * through {@link JsonApi4jServletContainerInitializer#EXECUTOR_SERVICE_ATT_NAME} owns that pool's lifecycle and
 * may well be sharing it with the rest of the application, so shutting it down here would reach outside the
 * framework's own resources.
 */
@Slf4j
public class JsonApi4jExecutorShutdownListener implements ServletContextListener {

    static final long SHUTDOWN_TIMEOUT_SECONDS = 5;

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        ServletContext servletContext = event.getServletContext();

        Object frameworkOwned = servletContext.getAttribute(FRAMEWORK_OWNED_EXECUTOR_ATT_NAME);
        if (!(frameworkOwned instanceof ExecutorService)) {
            return;
        }

        if (frameworkOwned != servletContext.getAttribute(EXECUTOR_SERVICE_ATT_NAME)) {
            log.debug(
                    "The executor in the servlet context is not the one the framework created - leaving it to"
                            + " whoever put it there."
            );
            return;
        }
        ExecutorService executorService = (ExecutorService) frameworkOwned;

        log.info("Shutting down the JsonApi4j {}...", ExecutorService.class.getSimpleName());
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                log.warn(
                        "JsonApi4j {} did not terminate within {}s - cancelling in-flight tasks.",
                        ExecutorService.class.getSimpleName(),
                        SHUTDOWN_TIMEOUT_SECONDS
                );
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
