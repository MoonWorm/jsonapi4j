package pro.api4.jsonapi4j.sampleapp.servlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer;

/**
 * JUnit 5 extension that starts an embedded Jetty server with the full JsonApi4j stack
 * before each test class and stops it after. This gives each test class a fresh server
 * with clean in-memory state (important for write operations like create/update/delete).
 * <p>
 * The config file is determined by the {@link JettyTestConfig} annotation on the test class.
 * If not present, defaults to {@code /jsonapi4j-domainTest.yaml}.
 */
public class EmbeddedJettyExtension implements BeforeAllCallback, AfterAllCallback {

    public static final String ROOT_PATH = "/jsonapi";
    public static final int PORT = 9595;

    private static final String DEFAULT_CONFIG = "/jsonapi4j-domainTest.yaml";

    private Server server;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        String configPath = resolveConfigPath(context);

        server = new Server(PORT);

        ServletContextHandler handler = new ServletContextHandler();
        handler.setContextPath("/");
        handler.setInitParameter("jsonapi4j.config", configPath);

        JsonApi4jServletContainerInitializer jsonApi4jInitializer = new JsonApi4jServletContainerInitializer();
        jsonApi4jInitializer.onStartup(null, handler.getServletContext());

        var plugins = ServletJsonapi4jSampleApp.initPlugins(handler.getServletContext());
        JsonApi4j jsonApi4j = ServletJsonapi4jSampleApp.createJsonApi4j(plugins);
        handler.setAttribute(JsonApi4jServletContainerInitializer.JSONAPI4J_ATT_NAME, jsonApi4j);

        server.setHandler(handler);
        server.start();
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    private String resolveConfigPath(ExtensionContext context) {
        return context.getTestClass()
                .map(cls -> cls.getAnnotation(JettyTestConfig.class))
                .map(JettyTestConfig::value)
                .orElse(DEFAULT_CONFIG);
    }

}
