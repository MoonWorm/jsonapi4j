package pro.api4.jsonapi4j.plugin.cd.init;

import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.compound.docs.DefaultDomainUrlResolver;
import pro.api4.jsonapi4j.compound.docs.DomainUrlResolver;
import pro.api4.jsonapi4j.compound.docs.cache.CompoundDocsResourceCache;
import pro.api4.jsonapi4j.compound.docs.cache.InMemoryCompoundDocsResourceCache;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.init.JsonApi4jPropertiesLoader;
import pro.api4.jsonapi4j.plugin.cd.CompoundDocsFilter;
import pro.api4.jsonapi4j.plugin.cd.config.CompoundDocsProperties;
import pro.api4.jsonapi4j.plugin.cd.config.DefaultCompoundDocsProperties;

import java.util.Map;
import java.util.Set;

@Slf4j
public class JsonApi4jCompoundDocsServletContainerInitializer implements ServletContainerInitializer {

    public static final String COMPOUND_DOCS_FILTER_NAME = "jsonapi4jCompoundDocsFilter";
    public static final String COMPOUND_DOCS_PLUGIN_ROOT_PATH_ATT_NAME = "jsonApi4jCdPluginRootPath";
    public static final String COMPOUND_DOCS_PLUGIN_PROPERTIES_ATT_NAME = "jsonApi4jCdPluginProperties";
    public static final String COMPOUND_DOCS_PLUGIN_DOMAIN_URL_RESOLVER_ATT_NAME = "jsonApi4jCdPluginDomainResolver";
    public static final String COMPOUND_DOCS_PLUGIN_CACHE_ATT_NAME = "jsonApi4jCdPluginCache";

    @Override
    public void onStartup(Set<Class<?>> hooks, ServletContext servletContext) {
        CompoundDocsProperties cdProperties = initCdProperties(servletContext);
        if (cdProperties.enabled()) {
            initDomainUriResolver(servletContext, cdProperties);
            initCache(servletContext, cdProperties);

            String rootPath = initRootPath(servletContext);
            String dispatcherServletMapping = rootPath + "/*";

            registerCompoundDocsFilter(
                    servletContext,
                    dispatcherServletMapping
            );
        }
    }

    private void initCache(ServletContext servletContext, CompoundDocsProperties cdProperties) {
        boolean cacheEnabled = cdProperties.cache() != null
                ? cdProperties.cache().enabled()
                : Boolean.parseBoolean(CompoundDocsProperties.Cache.CD_CACHE_ENABLED_DEFAULT_VALUE);
        if (!cacheEnabled) {
            log.info("CD Cache is disabled via configuration.");
            return;
        }
        if (servletContext.getAttribute(COMPOUND_DOCS_PLUGIN_CACHE_ATT_NAME) == null) {
            log.warn("CD Cache is not found in servlet context. Composing a default InMemoryCompoundDocsResourceCache...");
            int maxSize = cdProperties.cache() != null
                    ? cdProperties.cache().maxSize()
                    : Integer.parseInt(CompoundDocsProperties.Cache.CD_CACHE_MAX_SIZE_DEFAULT_VALUE);
            CompoundDocsResourceCache cache = new InMemoryCompoundDocsResourceCache(maxSize);
            servletContext.setAttribute(COMPOUND_DOCS_PLUGIN_CACHE_ATT_NAME, cache);
        }
    }

    private void initDomainUriResolver(ServletContext servletContext, CompoundDocsProperties cdProperties) {
        if (servletContext.getAttribute(COMPOUND_DOCS_PLUGIN_DOMAIN_URL_RESOLVER_ATT_NAME) == null) {
            log.warn("CD Domain Url Resolver is not found in servlet context. Composing a default one...");
            DomainUrlResolver domainUrlResolver = DefaultDomainUrlResolver.from(cdProperties.mapping());
            servletContext.setAttribute(COMPOUND_DOCS_PLUGIN_DOMAIN_URL_RESOLVER_ATT_NAME, domainUrlResolver);
        }
    }

    private String initRootPath(ServletContext servletContext) {
        String rootPath = servletContext.getInitParameter(COMPOUND_DOCS_PLUGIN_ROOT_PATH_ATT_NAME);
        if (rootPath == null) {
            log.warn("Oas Root Path attribute is not set in servlet context. Reading from a JsonApi4j config file...");
            rootPath = readJsonApi4jProperties(servletContext).rootPath();
            servletContext.setAttribute(COMPOUND_DOCS_PLUGIN_ROOT_PATH_ATT_NAME, rootPath);
        }
        return rootPath;
    }

    private JsonApi4jProperties readJsonApi4jProperties(ServletContext servletContext) {
        return JsonApi4jPropertiesLoader.loadConfig(servletContext);
    }

    private CompoundDocsProperties initCdProperties(ServletContext servletContext) {
        CompoundDocsProperties cdProperties = (CompoundDocsProperties) servletContext.getAttribute(COMPOUND_DOCS_PLUGIN_PROPERTIES_ATT_NAME);
        if (cdProperties == null) {
            log.warn("CD Properties are not found in servlet context. Reading from a config file...");
            cdProperties = readCdProperties(servletContext);
            servletContext.setAttribute(COMPOUND_DOCS_PLUGIN_PROPERTIES_ATT_NAME, cdProperties);
        }
        return cdProperties;
    }

    private static CompoundDocsProperties readCdProperties(ServletContext servletContext) {
        Map<String, Object> jsonApi4jPropertiesRaw = JsonApi4jPropertiesLoader.loadConfigAsMap(servletContext);
        return DefaultCompoundDocsProperties.toCdProperties(jsonApi4jPropertiesRaw);
    }

    private void registerCompoundDocsFilter(ServletContext servletContext,
                                            String rootPath) {

        FilterRegistration.Dynamic filter = servletContext.addFilter(
                COMPOUND_DOCS_FILTER_NAME,
                new CompoundDocsFilter()
        );
        filter.addMappingForUrlPatterns(
                null, // DispatcherType.REQUEST is used by default
                false, // supposed to be matched before any declared filter mappings of the ServletContext
                rootPath
        );
    }


}
