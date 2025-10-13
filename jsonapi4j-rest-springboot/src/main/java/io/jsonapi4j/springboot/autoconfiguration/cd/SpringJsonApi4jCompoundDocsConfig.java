package io.jsonapi4j.springboot.autoconfiguration.cd;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonapi4j.compound.docs.CompoundDocsResolver;
import io.jsonapi4j.compound.docs.CompoundDocsResolverConfig;
import io.jsonapi4j.config.JsonApi4jProperties;
import io.jsonapi4j.servlet.filter.cd.CompoundDocsFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static io.jsonapi4j.config.CompoundDocsProperties.JSONAPI4J_COMPOUND_DOCS_ERROR_STRATEGY_DEFAULT_VALUE;
import static io.jsonapi4j.config.CompoundDocsProperties.JSONAPI4J_COMPOUND_DOCS_MAX_HOPS_DEFAULT_VALUE;
import static java.util.stream.Collectors.toMap;

@ConditionalOnProperty(
        prefix = "jsonapi4j.compound-docs",
        name = "enabled",
        havingValue = "true"
)
@Configuration
public class SpringJsonApi4jCompoundDocsConfig {

    @Bean
    public CompoundDocsResolverConfig.DomainUrlResolver jsonapi4jCompoundDocsDomainUrlResolver(
            JsonApi4jProperties properties
    ) {
        Map<String, URI> mapping = properties.getCompoundDocs().getMapping()
                .entrySet()
                .stream()
                .collect(toMap(Map.Entry::getKey, e -> URI.create(e.getValue())));
        return new CompoundDocsResolverConfig.DefaultDomainUrlResolver(mapping);
    }

    @Bean
    public CompoundDocsResolverConfig jsonapi4jCompoundDocsResolverConfig(
            @Qualifier("jsonApi4jObjectMapper") ObjectMapper objectMapper,
            @Qualifier("jsonApi4jExecutorService") ExecutorService executorService,
            CompoundDocsResolverConfig.DomainUrlResolver domainUrlResolver,
            JsonApi4jProperties properties
    ) {
        return new CompoundDocsResolverConfig(
                objectMapper,
                domainUrlResolver,
                executorService,
                properties.getCompoundDocs() != null ?
                        properties.getCompoundDocs().getMaxHops() :
                        JSONAPI4J_COMPOUND_DOCS_MAX_HOPS_DEFAULT_VALUE,
                properties.getCompoundDocs() != null ?
                        properties.getCompoundDocs().getErrorStrategy() :
                        JSONAPI4J_COMPOUND_DOCS_ERROR_STRATEGY_DEFAULT_VALUE
        );
    }

    @Bean
    public CompoundDocsResolver jsonapi4jCompoundDocsResolver(CompoundDocsResolverConfig config) {
        return new CompoundDocsResolver(config);
    }

    @Bean
    public FilterRegistrationBean<?> jsonapi4jCompoundDocsFilter(
            @Qualifier("jsonApi4jDispatcherServlet") ServletRegistrationBean<?> jsonApi4jDispatcherServlet,
            CompoundDocsResolver compoundDocsResolver
    ) {
        return new FilterRegistrationBean<>(
                new CompoundDocsFilter(compoundDocsResolver),
                jsonApi4jDispatcherServlet
        );
    }

}
