package pro.api4.jsonapi4j.rest.quarkus.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.principal.PrincipalResolver;

import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.*;

@ApplicationScoped
public class JsonApi4jContextListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(JsonApi4jContextListener.class);

    @Inject
    Provider<JsonApi4j> jsonApi4j;
    @Inject
    Provider<ObjectMapper> objectMapper;
    @Inject
    Provider<PrincipalResolver> principalResolver;
    @Inject
    Provider<JsonApi4jProperties> properties;

    @Override
    public void contextInitialized(ServletContextEvent e) {
        log.info("Initializing Servlet Context for JsonApi4j Quarkus extension...");
        ServletContext servletContext = e.getServletContext();


        //TODO: load from Quarkus Props as raw Map
        servletContext.setAttribute(JSONAPI4J_PROPERTIES_ATT_NAME, properties.get());

        servletContext.setAttribute(JSONAPI4J_ATT_NAME, jsonApi4j.get());
        log.info("JsonApi4j instance has been set as '{}' Servlet Context Attribute.", JSONAPI4J_ATT_NAME);

        servletContext.setAttribute(OBJECT_MAPPER_ATT_NAME, objectMapper.get());
        log.info("Common ObjectMapper instance has been set as '{}' Servlet Context Attribute.", OBJECT_MAPPER_ATT_NAME);

        servletContext.setAttribute(PRINCIPAL_RESOLVER_ATT_NAME, principalResolver.get());
        log.info("PrincipalResolver instance has been set as '{}' Servlet Context Attribute.", PRINCIPAL_RESOLVER_ATT_NAME);
    }
}
