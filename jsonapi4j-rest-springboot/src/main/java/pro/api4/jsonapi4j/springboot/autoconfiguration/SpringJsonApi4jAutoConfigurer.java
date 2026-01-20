package pro.api4.jsonapi4j.springboot.autoconfiguration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.buf.EncodedSolidusHandling;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.plugin.ac.AccessControlEvaluator;
import pro.api4.jsonapi4j.plugin.ac.JsonApiAccessControlPlugin;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.plugin.oas.OasServlet;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.servlet.JsonApi4jDispatcherServlet;
import pro.api4.jsonapi4j.servlet.request.body.RequestBodyCachingFilter;
import pro.api4.jsonapi4j.servlet.response.errorhandling.ErrorHandlerFactoriesRegistry;
import pro.api4.jsonapi4j.servlet.response.errorhandling.JsonApi4jErrorHandlerFactoriesRegistry;
import pro.api4.jsonapi4j.servlet.response.errorhandling.impl.DefaultErrorHandlerFactory;
import pro.api4.jsonapi4j.servlet.response.errorhandling.impl.Jsr380ErrorHandlers;
import pro.api4.jsonapi4j.springboot.autoconfiguration.ac.SpringJsonApi4jAccessControlConfig;
import pro.api4.jsonapi4j.springboot.autoconfiguration.cd.SpringJsonApi4jCompoundDocsConfig;
import pro.api4.jsonapi4j.springboot.autoconfiguration.oas.SpringJsonApi4jOasConfig;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@Import(value = {
        SpringJsonApi4jAccessControlConfig.class,
        SpringJsonApi4jOasConfig.class,
        SpringJsonApi4jCompoundDocsConfig.class,
})
@ComponentScan(basePackages = {"pro.api4.jsonapi4j.springboot.autoconfiguration"})
public class SpringJsonApi4jAutoConfigurer {

    @Bean
    public List<JsonApi4jPlugin> defaultPlugins() {
        return List.of(
                new JsonApiAccessControlPlugin(),
                new JsonApiOasPlugin()
        );
    }

    @Bean
    public DomainRegistry jsonApi4jDomainRegistry(
            List<JsonApi4jPlugin> defaultPlugins,
            SpringContextJsonApi4jDomainScanner domainScanner
    ) {
        return DomainRegistry.builder(defaultPlugins)
                .resources(domainScanner.getResources())
                .relationships(domainScanner.getRelationships())
                .build();
    }

    @Bean
    public OperationsRegistry jsonApi4jOperationsRegistry(
            SpringContextJsonApi4jOperationsScanner operationsScanner,
            List<JsonApi4jPlugin> defaultPlugins
    ) {
        return OperationsRegistry.builder(defaultPlugins)
                .operations(operationsScanner.getOperations())
                .build();
    }

    @Bean("jsonApi4jExecutorService")
    public ExecutorService jsonApi4jExecutorService() {
        return Executors.newCachedThreadPool();
    }

    @Bean
    public JsonApi4j jsonApi4j(
            DomainRegistry domainRegistry,
            OperationsRegistry operationsRegistry,
            AccessControlEvaluator accessControlEvaluator,
            @Qualifier("jsonApi4jExecutorService") ExecutorService jsonApiExecutorService
    ) {
        return JsonApi4j.builder()
                .domainRegistry(domainRegistry)
                .operationsRegistry(operationsRegistry)
                .accessControlEvaluator(accessControlEvaluator)
                .executor(jsonApiExecutorService)
                .build();
    }

    @Bean(name = "jsonApi4jObjectMapper")
    public ObjectMapper jsonApi4jObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));
        return mapper;
    }

    @Bean
    public ErrorHandlerFactoriesRegistry jsonapi4jErrorHandlerFactoriesRegistry(
            SpringContextJsonApi4jErrorHandlerFactoriesScanner errorHandlerFactoriesScanner
    ) {
        JsonApi4jErrorHandlerFactoriesRegistry jsonapi4jErrorHandlerFactoriesRegistry
                = new JsonApi4jErrorHandlerFactoriesRegistry();
        jsonapi4jErrorHandlerFactoriesRegistry.registerAll(new DefaultErrorHandlerFactory());
        jsonapi4jErrorHandlerFactoriesRegistry.registerAll(new Jsr380ErrorHandlers());
        errorHandlerFactoriesScanner.getErrorHandlerFactories()
                .forEach(jsonapi4jErrorHandlerFactoriesRegistry::registerAll);
        return jsonapi4jErrorHandlerFactoriesRegistry;
    }

    @Bean(name = "jsonApi4jDispatcherServlet")
    public ServletRegistrationBean<?> jsonApi4jDispatcherServlet(
            JsonApi4jProperties properties,
            DomainRegistry domainRegistry,
            OperationsRegistry operationsRegistry,
            AccessControlEvaluator accessControlEvaluator,
            ErrorHandlerFactoriesRegistry errorHandlerFactory,
            @Qualifier("jsonApi4jObjectMapper") ObjectMapper objectMapper,
            @Qualifier("jsonApi4jExecutorService") ExecutorService executorService
    ) {
        String jsonapi4jRootPath = properties.getRootPath();

        String effectiveServletUrlMapping;
        if (StringUtils.isNotBlank(jsonapi4jRootPath) && jsonapi4jRootPath.trim().equals("/")) {
            effectiveServletUrlMapping = "/*";
        } else {
            effectiveServletUrlMapping = jsonapi4jRootPath + "/*";
        }

        ServletRegistrationBean<?> servletRegistration = new ServletRegistrationBean<>(
                new JsonApi4jDispatcherServlet(
                        domainRegistry,
                        operationsRegistry,
                        accessControlEvaluator, executorService,
                        errorHandlerFactory,
                        objectMapper
                ),
                effectiveServletUrlMapping
        );
        servletRegistration.setLoadOnStartup(1);
        return servletRegistration;
    }

    @Bean
    public FilterRegistrationBean<?> jsonApi4jRequestBodyCachingFilter(
            @Qualifier("jsonApi4jDispatcherServlet") ServletRegistrationBean<?> jsonApi4jDispatcherServlet
    ) {
        return new FilterRegistrationBean<>(
                new RequestBodyCachingFilter(),
                jsonApi4jDispatcherServlet
        );
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> jsonApi4jTomcatCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            //Configuring Tomcat to allow encoded slashes
            connector.setEncodedSolidusHandling(EncodedSolidusHandling.DECODE.getValue());
            //Configuring Tomcat to allow '[' and ']' chars in query params
            connector.setProperty("relaxedQueryChars", "[]");
        });
    }

    @Bean(name = "jsonApi4jOasServlet")
    public ServletRegistrationBean<?> jsonApi4jOasServlet(
            JsonApi4jProperties properties,
            DomainRegistry domainRegistry,
            OperationsRegistry operationsRegistry
    ) {
        String jsonapi4jRootPath = properties.getRootPath();

        String effectiveServletUrlMapping;
        if (StringUtils.isNotBlank(jsonapi4jRootPath) && jsonapi4jRootPath.trim().equals("/")) {
            effectiveServletUrlMapping = "/oas/*";
        } else {
            effectiveServletUrlMapping = jsonapi4jRootPath + "/oas/*";
        }

        ServletRegistrationBean<?> servletRegistration = new ServletRegistrationBean<>(
                new OasServlet(
                        domainRegistry,
                        operationsRegistry,
                        properties
                ),
                effectiveServletUrlMapping
        );
        servletRegistration.setLoadOnStartup(2);
        return servletRegistration;
    }

}
