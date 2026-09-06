package pro.api4.jsonapi4j.sampleapp.servlet;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer;
import pro.api4.jsonapi4j.plugin.cd.init.JsonApi4jCompoundDocsServletContainerInitializer;
import pro.api4.jsonapi4j.plugin.oas.init.JsonApiOasServletContainerInitializer;

import java.nio.file.Files;
import java.util.List;
import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.JSONAPI4J_DISPATCHER_SERVLET_NAME;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.JSONAPI4J_PRINCIPAL_RESOLVING_FILTER_NAME;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.JSONAPI4J_REQUEST_BODY_CACHING_FILTER_NAME;
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
        webApp.setBaseResource(Resource.newResource(Files.createTempDirectory("jsonapi4j-sci-test")));
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
