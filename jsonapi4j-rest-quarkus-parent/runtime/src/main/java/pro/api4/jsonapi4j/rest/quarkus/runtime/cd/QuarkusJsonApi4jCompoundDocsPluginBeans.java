package pro.api4.jsonapi4j.rest.quarkus.runtime.cd;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.api4.jsonapi4j.compound.docs.DefaultDomainUrlResolver;
import pro.api4.jsonapi4j.compound.docs.DomainUrlResolver;
import pro.api4.jsonapi4j.compound.docs.cache.CompoundDocsResourceCache;
import pro.api4.jsonapi4j.compound.docs.cache.InMemoryCompoundDocsResourceCache;
import pro.api4.jsonapi4j.plugin.cd.JsonApiCompoundDocsPlugin;
import pro.api4.jsonapi4j.rest.quarkus.runtime.cd.QuarkusJsonApi4jCompoundDocsProperties.CacheConfig;

import static pro.api4.jsonapi4j.plugin.cd.config.CompoundDocsProperties.Cache.CD_CACHE_MAX_SIZE_DEFAULT_VALUE;

/**
 * Optional beans that are only registered when jsonapi4j-oas-plugin is available in the app classpath.
 */
@IfBuildProperty(name = "jsonapi4j.cd.enabled", stringValue = "true")
public class QuarkusJsonApi4jCompoundDocsPluginBeans {

    private static final Logger LOG = LoggerFactory.getLogger(QuarkusJsonApi4jCompoundDocsPluginBeans.class);

    @Produces
    @Singleton
    @DefaultBean
    JsonApiCompoundDocsPlugin jsonApiCdPlugin() {
        LOG.info("CD Plugin Enabled. Composing {}...", JsonApiCompoundDocsPlugin.class.getSimpleName());
        return new JsonApiCompoundDocsPlugin();
    }

    @Produces
    @Singleton
    @DefaultBean
    DomainUrlResolver jsonApiCdDomainUrlResolver(QuarkusJsonApi4jCompoundDocsProperties cdProperties) {
        LOG.info("CD Plugin Enabled. Composing {}...", DomainUrlResolver.class.getSimpleName());
        return DefaultDomainUrlResolver.from(cdProperties.mapping());
    }

    @Produces
    @Singleton
    @DefaultBean
    @IfBuildProperty(name = "jsonapi4j.cd.cache.enabled", stringValue = "true")
    CompoundDocsResourceCache jsonApi4jCompoundDocsResourceCache(
            QuarkusJsonApi4jCompoundDocsProperties cdProperties
    ) {
        LOG.info("CD Plugin Enabled. Composing default {}...", CompoundDocsResourceCache.class.getSimpleName());
        return new InMemoryCompoundDocsResourceCache(
                cdProperties.cache()
                        .map(CacheConfig::maxSize)
                        .orElse(Integer.parseInt(CD_CACHE_MAX_SIZE_DEFAULT_VALUE))
        );
    }

}
