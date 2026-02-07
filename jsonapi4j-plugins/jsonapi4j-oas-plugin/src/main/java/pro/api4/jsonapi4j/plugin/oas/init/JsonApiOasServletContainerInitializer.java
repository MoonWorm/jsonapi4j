package pro.api4.jsonapi4j.plugin.oas.init;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.config.JsonApi4jConfigReader;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.init.JsonApi4jPropertiesLoader;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.plugin.oas.OasServlet;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static pro.api4.jsonapi4j.config.JsonApi4jProperties.JSONAPI4J_DEFAULT_ROOT_PATH;
import static pro.api4.jsonapi4j.config.JsonApi4jProperties.ROOT_PATH_PROPERTY_NAME;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.initDomainRegistry;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.initOperationRegistry;

@Slf4j
public class JsonApiOasServletContainerInitializer implements ServletContainerInitializer {

    public static final String JSONAPI4J_OAS_SERVLET_NAME = "jsonApi4jOasServlet";

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext servletContext) throws ServletException {
        Map<String, Object> jsonApiPropertiesRaw = JsonApi4jPropertiesLoader.loadConfigAsMap(servletContext);
        DomainRegistry domainRegistry = initDomainRegistry(servletContext);
        OperationsRegistry operationsRegistry = initOperationRegistry(servletContext);
        registerOasServlet(
                servletContext,
                jsonApiPropertiesRaw,
                domainRegistry,
                operationsRegistry
        );
    }

    private void registerOasServlet(ServletContext servletContext,
                                    Map<String, Object> jsonApiPropertiesRaw,
                                    DomainRegistry domainRegistry,
                                    OperationsRegistry operationsRegistry) {
        String rootPath = String.valueOf(jsonApiPropertiesRaw.getOrDefault(ROOT_PATH_PROPERTY_NAME, JSONAPI4J_DEFAULT_ROOT_PATH));
        Object oasPropertiesObject = jsonApiPropertiesRaw.get(OasProperties.OAS_PROPERTY_NAME);
        Map<String, Object> oasPropertiesRaw = Collections.emptyMap();
        if (oasPropertiesObject instanceof Map oasPropertiesMap) {
            //noinspection unchecked
            oasPropertiesRaw = oasPropertiesMap;
        }
        OasProperties oasProperties = JsonApi4jConfigReader.convertToConfig(
                oasPropertiesRaw,
                OasProperties.class
        );
        ServletRegistration.Dynamic oasServlet = servletContext.addServlet(
                JSONAPI4J_OAS_SERVLET_NAME,
                new OasServlet(
                        domainRegistry,
                        operationsRegistry,
                        rootPath,
                        oasProperties
                )
        );
        oasServlet.addMapping(oasProperties.getOasRootPath() + "/*");
    }

}
