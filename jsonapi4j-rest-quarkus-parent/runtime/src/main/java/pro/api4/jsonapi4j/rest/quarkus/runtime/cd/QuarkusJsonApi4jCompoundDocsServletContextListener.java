package pro.api4.jsonapi4j.rest.quarkus.runtime.cd;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.api4.jsonapi4j.compound.docs.DomainUrlResolver;
import pro.api4.jsonapi4j.rest.quarkus.runtime.QuarkusJsonApi4jProperties;

import static pro.api4.jsonapi4j.plugin.cd.init.JsonApi4jCompoundDocsServletContainerInitializer.*;

@Singleton
public class QuarkusJsonApi4jCompoundDocsServletContextListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(QuarkusJsonApi4jCompoundDocsServletContextListener.class);

    @Inject
    Provider<QuarkusJsonApi4jProperties> jsonApi4jPropertiesProvider;

    @Inject
    Provider<QuarkusJsonApi4jCompoundDocsProperties> quarkusCdPropertiesProvider;

    @Inject
    Provider<DomainUrlResolver> domainUrlResolverProvider;

    @Override
    public void contextInitialized(ServletContextEvent e) {
        log.info("Initializing CD Servlet Context for JsonApi4j Quarkus extension...");
        ServletContext servletContext = e.getServletContext();

        servletContext.setAttribute(COMPOUND_DOCS_PLUGIN_ROOT_PATH_ATT_NAME, jsonApi4jPropertiesProvider.get().rootPath());
        log.info("JsonApi4j Root Path has been set as '{}' Servlet Context Attribute.", COMPOUND_DOCS_PLUGIN_ROOT_PATH_ATT_NAME);

        QuarkusJsonApi4jCompoundDocsProperties quarkusCdProperties = quarkusCdPropertiesProvider.get();
        servletContext.setAttribute(COMPOUND_DOCS_PLUGIN_PROPERTIES_ATT_NAME, quarkusCdProperties.toCdProperties());
        log.info("QuarkusJsonApi4jCompoundDocsProperties ('jsonapi4j.cd' prefix of Quarkus application properties) instance has been set as '{}' Servlet Context Attribute.", COMPOUND_DOCS_PLUGIN_PROPERTIES_ATT_NAME);

        servletContext.setAttribute(COMPOUND_DOCS_PLUGIN_DOMAIN_URL_RESOLVER_ATT_NAME, domainUrlResolverProvider.get());
        log.info("DomainUrlResolver has been set as '{}' Servlet Context Attribute.", COMPOUND_DOCS_PLUGIN_DOMAIN_URL_RESOLVER_ATT_NAME);

        log.info("Initializing CD Servlet Context has been done.");
    }
}
