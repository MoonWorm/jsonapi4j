package pro.api4.jsonapi4j.sampleapp.servlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.sampleapp.servlet.operations.cache.CacheDownstreamServlet;

import java.util.List;

import static pro.api4.jsonapi4j.sampleapp.servlet.ServletJsonapi4jSampleApp.*;

/**
 * JUnit 5 extension that starts an embedded Jetty server with the full JsonApi4j stack <em>before each test
 * method</em> and stops it after. Unlike {@link EmbeddedJettyExtension} (which is per-class), the per-method
 * lifecycle gives every test a fresh compound-docs resource cache — the servlet analog of Spring's
 * {@code @DirtiesContext(AFTER_EACH_TEST_METHOD)}.
 *
 * <p>In addition to the JsonApi4j stack, it mounts a {@link CacheDownstreamServlet} at {@code /test-api/*} to
 * act as the counting downstream target for CD resolution of the {@code countries}/{@code currencies} types.
 */
public class EmbeddedJettyCacheExtension implements BeforeEachCallback, AfterEachCallback {

    private static final String CONFIG = "/jsonapi4j-cacheTest.yaml";

    private Server server;

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        server = new Server(EmbeddedJettyExtension.PORT);

        ServletContextHandler handler = new ServletContextHandler();
        handler.setContextPath("/");
        handler.setInitParameter("jsonapi4j.config", CONFIG);

        JsonApi4jServletContainerInitializer jsonApi4jInitializer = new JsonApi4jServletContainerInitializer();

        List<JsonApi4jPlugin> plugins = ServletJsonapi4jSampleApp.initPlugins(handler.getServletContext());
        initMetaContext(plugins, handler.getServletContext());
        initDomainRegistry(plugins, handler.getServletContext());
        initOperationRegistry(plugins, handler.getServletContext());

        jsonApi4jInitializer.onStartup(null, handler.getServletContext());

        // Downstream counting target for CD resolution — served by the same Jetty at /test-api/*
        handler.addServlet(new ServletHolder(new CacheDownstreamServlet()), "/test-api/*");

        server.setHandler(handler);
        server.start();
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (server != null) {
            server.stop();
        }
    }

}
