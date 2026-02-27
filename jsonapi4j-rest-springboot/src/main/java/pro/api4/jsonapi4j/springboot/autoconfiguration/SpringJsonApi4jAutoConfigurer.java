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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.filter.principal.PrincipalResolvingFilter;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.principal.DefaultPrincipalResolver;
import pro.api4.jsonapi4j.principal.PrincipalResolver;
import pro.api4.jsonapi4j.principal.tier.AccessTierRegistry;
import pro.api4.jsonapi4j.principal.tier.DefaultAccessTierRegistry;
import pro.api4.jsonapi4j.servlet.JsonApi4jDispatcherServlet;
import pro.api4.jsonapi4j.servlet.request.body.RequestBodyCachingFilter;
import pro.api4.jsonapi4j.springboot.autoconfiguration.ac.SpringJsonApi4jAcPluginConfig;
import pro.api4.jsonapi4j.springboot.autoconfiguration.cd.SpringJsonApi4jCompoundDocsConfig;
import pro.api4.jsonapi4j.springboot.autoconfiguration.oas.SpringJsonApi4jOasPluginConfig;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.*;

@Configuration
@Import(value = {
        SpringJsonApi4jAcPluginConfig.class,
        SpringJsonApi4jOasPluginConfig.class,
        SpringJsonApi4jCompoundDocsConfig.class,
})
@ComponentScan(basePackages = {"pro.api4.jsonapi4j.springboot.autoconfiguration"})
public class SpringJsonApi4jAutoConfigurer {

    @Bean
    public AccessTierRegistry jsonapi4jAccessTierRegistry() {
        return new DefaultAccessTierRegistry();
    }

    @Bean
    public PrincipalResolver jsonapi4jPrincipalResolver(
            AccessTierRegistry accessTierRegistry
    ) {
        return new DefaultPrincipalResolver(accessTierRegistry);
    }

    @Bean
    public FilterRegistrationBean<?> jsonapi4jPrincipalResolvingFilter(
            @Qualifier("jsonApi4jDispatcherServlet") ServletRegistrationBean<?> jsonApi4jDispatcherServlet
    ) {
        return new FilterRegistrationBean<>(
                new PrincipalResolvingFilter(),
                jsonApi4jDispatcherServlet
        );
    }

    @Bean
    public List<JsonApi4jPlugin> defaultPlugins(ObjectProvider<List<JsonApi4jPlugin>> pluginsProvider) {
        List<JsonApi4jPlugin> plugins = pluginsProvider.getIfAvailable();
        return plugins == null ? Collections.emptyList() : plugins;
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
            JsonApi4jProperties properties,
            DomainRegistry domainRegistry,
            OperationsRegistry operationsRegistry,
            List<JsonApi4jPlugin> defaultPlugins,
            @Qualifier("jsonApi4jExecutorService") ExecutorService jsonApiExecutorService
    ) {
        return JsonApi4j.builder()
                .domainRegistry(domainRegistry)
                .operationsRegistry(operationsRegistry)
                .plugins(defaultPlugins)
                .executor(jsonApiExecutorService)
                .compatibilityMode(properties.getCompatibility().resolveMode())
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
    public ServletContextInitializer jsonApi4jServletContextInitializer(
            JsonApi4jProperties properties,
            JsonApi4j jsonApi4j,
            @Qualifier("jsonApi4jObjectMapper") ObjectMapper objectMapper,
            @Qualifier("jsonApi4jExecutorService") ExecutorService executorService,
            PrincipalResolver jsonApi4jPrincipalResolver
    ) {
        return servletContext -> {
            servletContext.setAttribute(JSONAPI4J_PROPERTIES_ATT_NAME, properties);
            servletContext.setAttribute(JSONAPI4J_ATT_NAME, jsonApi4j);
            servletContext.setAttribute(OBJECT_MAPPER_ATT_NAME, objectMapper);
            servletContext.setAttribute(EXECUTOR_SERVICE_ATT_NAME, executorService);

            servletContext.setAttribute(PRINCIPAL_RESOLVER_ATT_NAME, jsonApi4jPrincipalResolver);
        };
    }

    @Bean(name = "jsonApi4jDispatcherServlet")
    public ServletRegistrationBean<?> jsonApi4jDispatcherServlet(JsonApi4jProperties properties) {
        String jsonapi4jRootPath = properties.getRootPath();

        String effectiveServletUrlMapping;
        if (StringUtils.isNotBlank(jsonapi4jRootPath) && jsonapi4jRootPath.trim().equals("/")) {
            effectiveServletUrlMapping = "/*";
        } else {
            effectiveServletUrlMapping = jsonapi4jRootPath + "/*";
        }

        ServletRegistrationBean<?> servletRegistration = new ServletRegistrationBean<>(
                new JsonApi4jDispatcherServlet(),
                effectiveServletUrlMapping
        );
        servletRegistration.setLoadOnStartup(1);
        servletRegistration.setName(JSONAPI4J_DISPATCHER_SERVLET_NAME);
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

}
