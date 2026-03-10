package pro.api4.jsonapi4j.rest.quarkus.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static pro.api4.jsonapi4j.plugin.oas.init.JsonApiOasServletContainerInitializer.OAS_PLUGIN_PROPERTIES_ATT_NAME;
import static pro.api4.jsonapi4j.plugin.oas.init.JsonApiOasServletContainerInitializer.OAS_PLUGIN_ROOT_PATH_ATT_NAME;

@ApplicationScoped
public class QuarkusJsonApi4jOasServletContextListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(QuarkusJsonApi4jOasServletContextListener.class);

    @Inject
    Provider<QuarkusJsonApi4jProperties> jsonApi4jPropertiesProvider;

    @Inject
    Provider<QuarkusJsonApi4jOasProperties> quarkusOasPropertiesProvider;

    @Override
    public void contextInitialized(ServletContextEvent e) {
        log.info("Initializing OAS Servlet Context for JsonApi4j Quarkus extension...");
        ServletContext servletContext = e.getServletContext();

        servletContext.setAttribute(OAS_PLUGIN_ROOT_PATH_ATT_NAME, jsonApi4jPropertiesProvider.get().rootPath());
        log.info("JsonApi4j Root Path has been set as '{}' Servlet Context Attribute.", OAS_PLUGIN_ROOT_PATH_ATT_NAME);

        QuarkusJsonApi4jOasProperties quarkusOasProperties = quarkusOasPropertiesProvider.get();
        servletContext.setAttribute(OAS_PLUGIN_PROPERTIES_ATT_NAME, quarkusOasProperties.toOasProperties());
        log.info("QuarkusJsonApi4jOasProperties ('jsonapi4j.oas' prefix of Quarkus application properties) instance has been set as '{}' Servlet Context Attribute.", OAS_PLUGIN_PROPERTIES_ATT_NAME);

        log.info("Initializing OAS Servlet Context has been done.");
    }
}
