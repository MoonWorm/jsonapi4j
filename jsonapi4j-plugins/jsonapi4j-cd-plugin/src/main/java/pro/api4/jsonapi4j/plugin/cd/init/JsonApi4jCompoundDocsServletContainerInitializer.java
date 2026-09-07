package pro.api4.jsonapi4j.plugin.cd.init;

import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.compound.docs.DefaultDomainSettingsResolver;
import pro.api4.jsonapi4j.compound.docs.DomainSettingsResolver;
import pro.api4.jsonapi4j.compound.docs.cache.CompoundDocsResourceCache;
import pro.api4.jsonapi4j.compound.docs.cache.InMemoryCompoundDocsResourceCache;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.init.JsonApi4jPropertiesLoader;
import pro.api4.jsonapi4j.plugin.cd.CompoundDocsFilter;
import pro.api4.jsonapi4j.plugin.cd.JsonApiCompoundDocsPlugin;
import pro.api4.jsonapi4j.plugin.cd.config.CompoundDocsProperties;
import pro.api4.jsonapi4j.plugin.cd.config.DefaultCompoundDocsProperties;

import java.util.Map;
import java.util.Set;

import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.initJsonApi4jProperties;

@Slf4j
public class JsonApi4jCompoundDocsServletContainerInitializer implements ServletContainerInitializer {

    public static final String COMPOUND_DOCS_FILTER_NAME = "jsonapi4jCompoundDocsFilter";
    public static final String COMPOUND_DOCS_PLUGIN_PROPERTIES_ATT_NAME = "jsonApi4jCdPluginProperties";
    public static final String COMPOUND_DOCS_PLUGIN_DOMAIN_SETTINGS_RESOLVER_ATT_NAME = "jsonApi4jCdPluginDomainResolver";
    public static final String COMPOUND_DOCS_PLUGIN_CACHE_ATT_NAME = "jsonApi4jCdPluginCache";

    @Override
    public void onStartup(Set<Class<?>> hooks, ServletContext servletContext) {
        CompoundDocsProperties cdProperties = initCdProperties(servletContext);
        if (cdProperties.enabled()) {
            initDomainSettingsResolver(servletContext, cdProperties);
            initCache(servletContext, cdProperties);

            JsonApi4jProperties jsonApi4jProperties = initJsonApi4jProperties(servletContext);
            String rootPath = jsonApi4jProperties.rootPath();
            String dispatcherServletMapping = rootPath + "/*";

            registerCompoundDocsFilter(
                    servletContext,
                    dispatcherServletMapping
            );
        } else {
            log.info(
                    "{} is disabled. Not registering {}",
                    JsonApiCompoundDocsPlugin.class.getSimpleName(),
                    CompoundDocsFilter.class.getSimpleName()
            );
        }
    }

    private static void initCache(ServletContext servletContext, CompoundDocsProperties cdProperties) {
        if (!cdProperties.cacheEnabled()) {
            log.info(
                    "{} is disabled via configuration.",
                    CompoundDocsResourceCache.class.getSimpleName()
            );
            return;
        }
        if (servletContext.getAttribute(COMPOUND_DOCS_PLUGIN_CACHE_ATT_NAME) == null) {
            log.warn(
                    "{} is not found in servlet context. Composing a default {}.",
                    CompoundDocsResourceCache.class.getSimpleName(),
                    InMemoryCompoundDocsResourceCache.class.getSimpleName()
            );
            CompoundDocsResourceCache cache = new InMemoryCompoundDocsResourceCache(cdProperties.cacheMaxSize());
            servletContext.setAttribute(COMPOUND_DOCS_PLUGIN_CACHE_ATT_NAME, cache);
        }
    }

    private static void initDomainSettingsResolver(ServletContext servletContext, CompoundDocsProperties cdProperties) {
        if (servletContext.getAttribute(COMPOUND_DOCS_PLUGIN_DOMAIN_SETTINGS_RESOLVER_ATT_NAME) == null) {
            log.warn(
                    "{} is not found in servlet context. Composing a default one - {}",
                    DomainSettingsResolver.class.getSimpleName(),
                    DefaultDomainSettingsResolver.class.getSimpleName()
            );
            DomainSettingsResolver domainSettingsResolver = DefaultDomainSettingsResolver.from(
                    cdProperties.mapping(),
                    cdProperties.batchSizeMapping(),
                    cdProperties.defaultMaxBatchSize()
            );
            servletContext.setAttribute(COMPOUND_DOCS_PLUGIN_DOMAIN_SETTINGS_RESOLVER_ATT_NAME, domainSettingsResolver);
        }
    }

    private static CompoundDocsProperties initCdProperties(ServletContext servletContext) {
        CompoundDocsProperties cdProperties = (CompoundDocsProperties) servletContext.getAttribute(COMPOUND_DOCS_PLUGIN_PROPERTIES_ATT_NAME);
        if (cdProperties == null) {
            log.warn(
                    "{} are not found in servlet context. Reading from a config file...",
                    CompoundDocsProperties.class.getSimpleName()
            );
            cdProperties = readCdProperties(servletContext);
            servletContext.setAttribute(COMPOUND_DOCS_PLUGIN_PROPERTIES_ATT_NAME, cdProperties);
        }
        return cdProperties;
    }

    private static CompoundDocsProperties readCdProperties(ServletContext servletContext) {
        Map<String, Object> jsonApi4jPropertiesRaw = JsonApi4jPropertiesLoader.loadRawConfig(servletContext).getProperties();
        return DefaultCompoundDocsProperties.toCdProperties(jsonApi4jPropertiesRaw);
    }

    private static void registerCompoundDocsFilter(ServletContext servletContext,
                                            String rootPath) {

        FilterRegistration.Dynamic filter = servletContext.addFilter(
                COMPOUND_DOCS_FILTER_NAME,
                new CompoundDocsFilter()
        );
        if (filter == null) {
            log.info(
                    "{} is already registered. Skipping registration.",
                    CompoundDocsFilter.class.getSimpleName()
            );
            return;
        }
        filter.addMappingForUrlPatterns(
                null, // DispatcherType.REQUEST is used by default
                false, // supposed to be matched before any declared filter mappings of the ServletContext
                rootPath
        );
    }


}
