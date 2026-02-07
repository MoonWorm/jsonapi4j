package pro.api4.jsonapi4j.plugin.oas.init;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.init.JsonApi4jPropertiesLoader;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.plugin.oas.OasServlet;

import java.util.Set;

import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.initDomainRegistry;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.initOperationRegistry;

@Slf4j
public class JsonApiOasServletContainerInitializer implements ServletContainerInitializer {

    public static final String JSONAPI4J_OAS_SERVLET_NAME = "jsonApi4jOasServlet";

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext servletContext) throws ServletException {
        JsonApi4jProperties properties = JsonApi4jPropertiesLoader.loadConfig(servletContext);
        DomainRegistry domainRegistry = initDomainRegistry(servletContext);
        OperationsRegistry operationsRegistry = initOperationRegistry(servletContext);
        registerOasServlet(
                servletContext,
                properties,
                domainRegistry,
                operationsRegistry
        );
    }

    private void registerOasServlet(ServletContext servletContext,
                                    JsonApi4jProperties properties,
                                    DomainRegistry domainRegistry,
                                    OperationsRegistry operationsRegistry) {
        ServletRegistration.Dynamic oasServlet = servletContext.addServlet(
                JSONAPI4J_OAS_SERVLET_NAME,
                new OasServlet(
                        domainRegistry,
                        operationsRegistry,
                        properties
                )
        );
        oasServlet.addMapping(properties.getOas().getOasRootPath() + "/*");
    }

}
