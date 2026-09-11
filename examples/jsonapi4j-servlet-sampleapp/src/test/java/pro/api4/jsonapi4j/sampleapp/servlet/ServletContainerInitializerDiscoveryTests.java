package pro.api4.jsonapi4j.sampleapp.servlet;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.eclipse.jetty.ee11.annotations.AnnotationConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer;
import pro.api4.jsonapi4j.plugin.cd.init.JsonApi4jCompoundDocsServletContainerInitializer;
import pro.api4.jsonapi4j.config.DefaultJsonApi4jProperties;
import pro.api4.jsonapi4j.plugin.PluginRegistry;
import pro.api4.jsonapi4j.principal.PrincipalResolver;
import pro.api4.jsonapi4j.plugin.oas.init.JsonApiOasServletContainerInitializer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.EXECUTOR_SERVICE_ATT_NAME;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.JSONAPI4J_DISPATCHER_SERVLET_NAME;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.JSONAPI4J_PRINCIPAL_RESOLVING_FILTER_NAME;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.JSONAPI4J_REQUEST_BODY_CACHING_FILTER_NAME;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.JSONAPI4J_PROPERTIES_ATT_NAME;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.PRINCIPAL_RESOLVER_ATT_NAME;
import static pro.api4.jsonapi4j.plugin.cd.init.JsonApi4jCompoundDocsServletContainerInitializer.COMPOUND_DOCS_FILTER_NAME;
import static pro.api4.jsonapi4j.plugin.oas.init.JsonApiOasServletContainerInitializer.JSONAPI4J_OAS_SERVLET_NAME;

/**
 * Covers the deployment path the other tests skip.
 * <p>
 * Every sample wires the initializers by hand — the embedded {@code ServletContextHandler} they use does not scan
 * {@code META-INF/services}, and Spring and Quarkus bring their own mechanisms. So the service files, and the
 * behaviour of {@code onStartup} when a container rather than a sample calls it, went unexercised: a service file
 * naming a class that no longer exists, or an initializer that only works because the sample pre-wired something,
 * would look exactly like a passing build.
 */
class ServletContainerInitializerDiscoveryTests {

    private static final int PORT = 9596;

    private Server server;

    @AfterEach
    void stopServer() throws Exception {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    /**
     * Starts Jetty the way an application server starts a war: no manual wiring, initializers found by scanning.
     */
    private ServletContext startScannedWebApp() throws Exception {
        server = new Server(PORT);

        WebAppContext webApp = new WebAppContext();
        webApp.setContextPath("/");
        webApp.setBaseResourceAsPath(Files.createTempDirectory("jsonapi4j-sci-test"));
        // the sample's own config: the only one with both plugins on, so both initializers have work to do
        webApp.setInitParameter("jsonapi4j.config", "/jsonapi4j.yaml");
        // added to the defaults, not replacing them: AnnotationConfiguration relies on the standard ones having
        // built the classpath metadata it scans
        webApp.addConfiguration(new AnnotationConfiguration());
        // under surefire everything sits on the system classpath, which Jetty treats as container rather than
        // webapp classes, so scanning has to be pointed at it explicitly
        webApp.setAttribute(
                "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*jsonapi4j.*"
        );

        server.setHandler(webApp);
        server.start();

        return webApp.getServletContext();
    }

    /**
     * The same scanned deployment, plus the one hook a war has: a listener contributing the registries. Listeners run
     * after every {@code ServletContainerInitializer}, so this is the earliest application code can speak.
     */
    private void startScannedWebAppWithListener() throws Exception {
        startScannedWebAppWithListener(servletContext -> { });
    }

    private void startScannedWebAppWithListener(Consumer<ServletContext> extraContributions) throws Exception {
        server = new Server(PORT);

        WebAppContext webApp = new WebAppContext();
        webApp.setContextPath("/");
        webApp.setBaseResourceAsPath(Files.createTempDirectory("jsonapi4j-war-test"));
        webApp.setInitParameter("jsonapi4j.config", "/jsonapi4j.yaml");
        webApp.addConfiguration(new AnnotationConfiguration());
        webApp.setAttribute(
                "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*jsonapi4j.*"
        );
        webApp.addEventListener(new ServletContextListener() {
            @Override
            public void contextInitialized(ServletContextEvent event) {
                ServletContext servletContext = event.getServletContext();
                PluginRegistry plugins = ServletJsonapi4jSampleApp.initPluginRegistry(servletContext);
                ServletJsonapi4jSampleApp.initMetaContext(plugins, servletContext);
                ServletJsonapi4jSampleApp.initDomainRegistry(plugins, servletContext);
                ServletJsonapi4jSampleApp.initOperationRegistry(plugins, servletContext);
                extraContributions.accept(servletContext);
            }
        });

        server.setHandler(webApp);
        server.start();
    }

    private HttpResponse<String> get(String path) throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + PORT + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    @Nested
    class ServiceFiles {

        @Test
        void serviceLoader_findsEveryInitializerTheJarsDeclare() {
            List<Class<?>> discovered = ServiceLoader.load(ServletContainerInitializer.class)
                    .stream()
                    .map(ServiceLoader.Provider::type)
                    .collect(java.util.stream.Collectors.toList());

            assertThat(discovered).contains(
                    JsonApi4jServletContainerInitializer.class,
                    JsonApiOasServletContainerInitializer.class,
                    JsonApi4jCompoundDocsServletContainerInitializer.class
            );
        }

    }

    @Nested
    class ScannedDeployment {

        @Test
        void scannedWebApp_registersTheOasServlet() throws Exception {
            ServletContext servletContext = startScannedWebApp();

            assertThat(servletContext.getServletRegistration(JSONAPI4J_OAS_SERVLET_NAME)).isNotNull();
        }

        @Test
        void scannedWebApp_registersTheCompoundDocsFilter() throws Exception {
            ServletContext servletContext = startScannedWebApp();

            assertThat(servletContext.getFilterRegistration(COMPOUND_DOCS_FILTER_NAME)).isNotNull();
        }

        @Test
        void scannedWebApp_mapsTheOasServletOnTheConfiguredPath() throws Exception {
            ServletContext servletContext = startScannedWebApp();

            assertThat(servletContext.getServletRegistration(JSONAPI4J_OAS_SERVLET_NAME).getMappings())
                    .containsExactly("/jsonapi/oas/*");
        }

    }

    /**
     * A war deployed to an external container: the container runs the initializers, then a listener contributes the
     * domain. The registries have to survive that ordering and reach the dispatcher servlet.
     */
    @Nested
    class WarStyleDeployment {

        @Test
        void listenerSuppliedRegistries_reachTheDispatcherServlet() throws Exception {
            startScannedWebAppWithListener();

            HttpResponse<String> response = get("/jsonapi/users");

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("\"type\":\"users\"");
        }

        /**
         * The servlet mapping is fixed from {@code rootPath} while the initializer runs, but {@code rootPath} is read
         * again for every link the API generates. Configuration arriving after deployment would leave requests
         * succeeding while every link pointed somewhere unmapped, so the mismatch has to be refused outright.
         */
        @Test
        void rootPathSuppliedAfterDeployment_isRefusedRatherThanServedWithBrokenLinks() throws Exception {
            startScannedWebAppWithListener(servletContext -> {
                DefaultJsonApi4jProperties lateProperties = new DefaultJsonApi4jProperties();
                lateProperties.setRootPath("/moved");
                servletContext.setAttribute(JSONAPI4J_PROPERTIES_ATT_NAME, lateProperties);
            });

            HttpResponse<String> response = get("/jsonapi/users");

            // Containers render a servlet that failed to initialize differently - Jetty answers 404 - so the
            // assertion is that the endpoint refuses to serve, not that it picks a particular status.
            assertThat(response.statusCode()).isIn(404, 503);
            assertThat(response.body()).doesNotContain("\"type\":\"users\"");
        }

        /**
         * The initializer installs a default {@code PrincipalResolver} during deployment, before any listener runs.
         * That default must still be replaceable: the filter reads the attribute when it initializes, which happens
         * after listeners, so a listener that overwrites it wins.
         */
        @Test
        void listenerSuppliedPrincipalResolver_replacesTheEagerDefault() throws Exception {
            AtomicBoolean resolverInvoked = new AtomicBoolean(false);
            startScannedWebAppWithListener(servletContext -> {
                PrincipalResolver recordingResolver = servletRequest -> {
                    resolverInvoked.set(true);
                    return null;
                };
                servletContext.setAttribute(PRINCIPAL_RESOLVER_ATT_NAME, recordingResolver);
            });

            HttpResponse<String> response = get("/jsonapi/users");

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(resolverInvoked).isTrue();
        }

    }

    /**
     * The framework's default executor is a cached thread pool, whose threads are non-daemon and outlive their last
     * task. In a war that is a leak with teeth: the threads survive undeploy, and the dead webapp's classloader stays
     * reachable through them. Nothing about it is visible in an executable jar, where the JVM exits regardless, so it
     * has to be pinned down in a real container.
     */
    @Nested
    class Undeploy {

        @Test
        void undeploy_executorCreatedByTheFramework_isShutDown() throws Exception {
            ServletContext servletContext = startScannedWebApp();
            ExecutorService executorService = (ExecutorService) servletContext.getAttribute(EXECUTOR_SERVICE_ATT_NAME);
            assertThat(executorService).isNotNull();

            server.stop();

            assertThat(executorService.isShutdown()).isTrue();
        }

        @Test
        void undeploy_executorSuppliedByTheApplication_isLeftRunning() throws Exception {
            ExecutorService applicationExecutor = java.util.concurrent.Executors.newCachedThreadPool();
            startScannedWebAppWithListener(servletContext ->
                    servletContext.setAttribute(EXECUTOR_SERVICE_ATT_NAME, applicationExecutor));

            server.stop();

            assertThat(applicationExecutor.isShutdown()).isFalse();
            applicationExecutor.shutdown();
        }
    }

    /**
     * A container that scans and an application that also wires by hand both register during startup, under the same
     * names. The servlet API answers the second registration with {@code null}, which used to be dereferenced on the
     * next line.
     */
    @Nested
    class RepeatedRegistration {

        @Test
        void onStartupCalledTwice_registersOnceAndDoesNotFail() {
            ServletContextHandler handler = new ServletContextHandler();
            handler.setContextPath("/");
            handler.setInitParameter("jsonapi4j.config", "/jsonapi4j.yaml");
            ServletContext servletContext = handler.getServletContext();

            runEveryInitializer(servletContext);
            runEveryInitializer(servletContext);

            assertThat(servletContext.getServletRegistrations()).containsKeys(
                    JSONAPI4J_OAS_SERVLET_NAME,
                    JSONAPI4J_DISPATCHER_SERVLET_NAME
            );
            assertThat(servletContext.getFilterRegistrations()).containsKeys(
                    COMPOUND_DOCS_FILTER_NAME,
                    JSONAPI4J_PRINCIPAL_RESOLVING_FILTER_NAME,
                    JSONAPI4J_REQUEST_BODY_CACHING_FILTER_NAME
            );
        }

        private void runEveryInitializer(ServletContext servletContext) {
            new JsonApi4jServletContainerInitializer().onStartup(null, servletContext);
            new JsonApiOasServletContainerInitializer().onStartup(null, servletContext);
            new JsonApi4jCompoundDocsServletContainerInitializer().onStartup(null, servletContext);
        }

    }

}
