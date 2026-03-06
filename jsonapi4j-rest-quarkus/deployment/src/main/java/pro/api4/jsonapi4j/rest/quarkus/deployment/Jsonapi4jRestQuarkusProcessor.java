package pro.api4.jsonapi4j.rest.quarkus.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.undertow.deployment.FilterBuildItem;
import io.quarkus.undertow.deployment.IgnoredServletContainerInitializerBuildItem;
import io.quarkus.undertow.deployment.ListenerBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem;
import jakarta.servlet.DispatcherType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.api4.jsonapi4j.filter.cd.CompoundDocsFilter;
import pro.api4.jsonapi4j.filter.principal.PrincipalResolvingFilter;
import pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer;
import pro.api4.jsonapi4j.rest.quarkus.runtime.QuarkusJsonApi4jContextListener;
import pro.api4.jsonapi4j.rest.quarkus.runtime.QuarkusJsonApi4jDefaultBeans;
import pro.api4.jsonapi4j.rest.quarkus.runtime.QuarkusJsonApi4jProperties;
import pro.api4.jsonapi4j.servlet.JsonApi4jDispatcherServlet;

import static pro.api4.jsonapi4j.config.CompoundDocsProperties.JSONAPI4J_COMPOUND_DOCS_ENABLED_DEFAULT_VALUE;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.JSONAPI4J_COMPOUND_DOCS_FILTER_NAME;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.JSONAPI4J_DISPATCHER_SERVLET_NAME;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.JSONAPI4J_PRINCIPAL_RESOLVING_FILTER_NAME;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.JSONAPI4J_REQUEST_BODY_CACHING_FILTER_NAME;

class Jsonapi4jRestQuarkusProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(Jsonapi4jRestQuarkusProcessor.class);

    private static final String FEATURE = "jsonapi4j-rest-quarkus";
    private static final String AC_PLUGIN_BEANS_CLASSNAME = "pro.api4.jsonapi4j.rest.quarkus.runtime.QuarkusJsonApi4jAcPluginBeans";
    private static final String AC_PLUGIN_CLASSNAME = "pro.api4.jsonapi4j.plugin.ac.JsonApiAccessControlPlugin";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ServletBuildItem registerServlet(QuarkusJsonApi4jProperties props) {
        String mapping = toServletMapping(props.rootPath());
        LOG.info("Registering JsonApi4jDispatcherServlet on '{}'", mapping);
        return ServletBuildItem.builder(JSONAPI4J_DISPATCHER_SERVLET_NAME, JsonApi4jDispatcherServlet.class.getName())
                .addMapping(mapping)
                .setLoadOnStartup(1)
                .build();
    }

    @BuildStep
    void registerCompoundDocsFilter(QuarkusJsonApi4jProperties props,
                                    BuildProducer<FilterBuildItem> filters) {
        boolean compoundDocsEnabled = props == null || props.compoundDocs() == null
                ? Boolean.parseBoolean(JSONAPI4J_COMPOUND_DOCS_ENABLED_DEFAULT_VALUE)
                : props.compoundDocs().enabled();

        if (!compoundDocsEnabled) {
            LOG.info("Compound docs disabled, skipping CompoundDocsFilter registration");
            return;
        }

        String mapping = toServletMapping(props.rootPath());
        LOG.info("Registering CompoundDocsFilter on '{}'", mapping);
        filters.produce(
                FilterBuildItem.builder(JSONAPI4J_COMPOUND_DOCS_FILTER_NAME, CompoundDocsFilter.class.getName())
                        .addFilterUrlMapping(mapping, DispatcherType.REQUEST)
                        .setLoadOnStartup(1)
                        .build()
        );
    }

    @BuildStep
    FilterBuildItem registerPrincipalResolvingFilter(QuarkusJsonApi4jProperties props) {
        String mapping = toServletMapping(props.rootPath());
        LOG.info("Registering PrincipalResolvingFilter on '{}'", mapping);
        return FilterBuildItem.builder(JSONAPI4J_PRINCIPAL_RESOLVING_FILTER_NAME, PrincipalResolvingFilter.class.getName())
                .addFilterUrlMapping(mapping, DispatcherType.REQUEST)
                .setLoadOnStartup(1)
                .build();
    }

    @BuildStep
    FilterBuildItem registerRequestBodyCachingFilter(QuarkusJsonApi4jProperties props) {
        String mapping = toServletMapping(props.rootPath());
        LOG.info("Registering RequestBodyCachingFilter on '{}'", mapping);
        return FilterBuildItem.builder(
                        JSONAPI4J_REQUEST_BODY_CACHING_FILTER_NAME,
                        "pro.api4.jsonapi4j.servlet.request.body.RequestBodyCachingFilter"
                ).addFilterUrlMapping(mapping, DispatcherType.REQUEST)
                .setLoadOnStartup(1)
                .build();
    }

    @BuildStep
    AdditionalBeanBuildItem jsonapi4jCdiBeans() {
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClass(QuarkusJsonApi4jContextListener.class)
                .addBeanClass(QuarkusJsonApi4jDefaultBeans.class)
                .addBeanClass(QuarkusJsonApi4jProperties.class);

        boolean isAutoconfigureAcPlugin = isClassPresent(AC_PLUGIN_CLASSNAME);
        if (isAutoconfigureAcPlugin) {
            LOG.info("{} plugin class is present in classpath, registering AC plugin CDI producers", AC_PLUGIN_CLASSNAME);
            builder.addBeanClass(AC_PLUGIN_BEANS_CLASSNAME);
        } else {
            LOG.info("{} plugin class is not available in classpath, skipping AC plugin registration", AC_PLUGIN_CLASSNAME);
        }

        return builder.build();
    }

    @BuildStep
    ListenerBuildItem servletContextListener() {
        return new ListenerBuildItem(QuarkusJsonApi4jContextListener.class.getName());
    }

    @BuildStep
    IgnoredServletContainerInitializerBuildItem ignoreJsonApi4jSci() {
        return new IgnoredServletContainerInitializerBuildItem(JsonApi4jServletContainerInitializer.class.getName());
    }

    private static String toServletMapping(String rootPath) {
        if (rootPath == null) {
            return "/*";
        }
        String normalized = rootPath.trim();
        if (normalized.isEmpty() || "/".equals(normalized)) {
            return "/*";
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized.endsWith("/*") ? normalized : normalized + "/*";
    }

    private static boolean isClassPresent(String className) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Class.forName(className, false, classLoader);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

}
