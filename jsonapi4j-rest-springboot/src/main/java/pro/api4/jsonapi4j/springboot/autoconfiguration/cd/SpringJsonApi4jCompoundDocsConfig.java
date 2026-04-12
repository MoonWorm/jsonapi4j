package pro.api4.jsonapi4j.springboot.autoconfiguration.cd;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pro.api4.jsonapi4j.compound.docs.DefaultDomainUrlResolver;
import pro.api4.jsonapi4j.compound.docs.DomainUrlResolver;
import pro.api4.jsonapi4j.compound.docs.cache.CompoundDocsResourceCache;
import pro.api4.jsonapi4j.compound.docs.cache.InMemoryCompoundDocsResourceCache;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.plugin.cd.CompoundDocsFilter;
import pro.api4.jsonapi4j.plugin.cd.JsonApiCompoundDocsPlugin;
import pro.api4.jsonapi4j.plugin.cd.config.CompoundDocsProperties;

import static pro.api4.jsonapi4j.plugin.cd.init.JsonApi4jCompoundDocsServletContainerInitializer.*;

@ConditionalOnProperty(
        prefix = "jsonapi4j.cd",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@Configuration
public class SpringJsonApi4jCompoundDocsConfig {

    @Bean
    public JsonApiCompoundDocsPlugin jsonApiCompoundDocsPlugin() {
        return new JsonApiCompoundDocsPlugin();
    }

    @ConditionalOnMissingBean(DomainUrlResolver.class)
    @Bean
    public DomainUrlResolver jsonApi4jCdDomainUrlResolver(CompoundDocsProperties cdProperties) {
        return DefaultDomainUrlResolver.from(cdProperties.mapping());
    }

    @Bean
    @ConditionalOnMissingBean(CompoundDocsResourceCache.class)
    @ConditionalOnProperty(name = "jsonapi4j.cd.cache.enabled", matchIfMissing = true)
    public CompoundDocsResourceCache jsonApi4jCompoundDocsResourceCache(CompoundDocsProperties cdProperties) {
        int maxSize = cdProperties.cache() != null
                ? cdProperties.cache().maxSize()
                : Integer.parseInt(CompoundDocsProperties.Cache.CD_CACHE_MAX_SIZE_DEFAULT_VALUE);
        return new InMemoryCompoundDocsResourceCache(maxSize);
    }

    @Bean
    public ServletContextInitializer jsonApi4jCdServletContextInitializer(
            JsonApi4jProperties jsonApi4jProperties,
            CompoundDocsProperties cdProperties,
            DomainUrlResolver domainUrlResolver,
            @Autowired(required = false) CompoundDocsResourceCache cache
    ) {
        return servletContext -> {
            servletContext.setAttribute(COMPOUND_DOCS_PLUGIN_ROOT_PATH_ATT_NAME, jsonApi4jProperties.rootPath());
            servletContext.setAttribute(COMPOUND_DOCS_PLUGIN_PROPERTIES_ATT_NAME, cdProperties);
            servletContext.setAttribute(COMPOUND_DOCS_PLUGIN_DOMAIN_URL_RESOLVER_ATT_NAME, domainUrlResolver);
            servletContext.setAttribute(COMPOUND_DOCS_PLUGIN_CACHE_ATT_NAME, cache);
        };
    }

    @Bean
    public FilterRegistrationBean<?> jsonapi4jCdFilter(
            @Qualifier("jsonApi4jDispatcherServlet") ServletRegistrationBean<?> jsonApi4jDispatcherServlet
    ) {
        return new FilterRegistrationBean<>(
                new CompoundDocsFilter(),
                jsonApi4jDispatcherServlet
        );
    }

}
