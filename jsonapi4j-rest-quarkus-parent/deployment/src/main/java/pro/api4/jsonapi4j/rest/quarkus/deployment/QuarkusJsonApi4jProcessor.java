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
import pro.api4.jsonapi4j.filter.principal.PrincipalResolvingFilter;
import pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer;
import pro.api4.jsonapi4j.rest.quarkus.runtime.*;
import pro.api4.jsonapi4j.servlet.JsonApi4jDispatcherServlet;

import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.*;

class QuarkusJsonApi4jProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(QuarkusJsonApi4jProcessor.class);

    private static final String FEATURE = "jsonapi4j-rest-quarkus";

    private static final String AC_PLUGIN_CLASSNAME = "pro.api4.jsonapi4j.plugin.ac.JsonApiAccessControlPlugin";
    private static final String OAS_PLUGIN_CLASSNAME = "pro.api4.jsonapi4j.plugin.oas.init.JsonApiOasServletContainerInitializer";
    private static final String SF_PLUGIN_CLASSNAME = "pro.api4.jsonapi4j.plugin.sf.JsonApiSparseFieldsetsPlugin";
    private static final String CD_SERVLET_INITIALIZER_CLASSNAME = "pro.api4.jsonapi4j.plugin.cd.init.JsonApi4jCompoundDocsServletContainerInitializer";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ServletBuildItem registersJsonApi4jDispatcherServlet(QuarkusJsonApi4jProperties props) {
        String mapping = toServletMapping(props.rootPath());
        LOG.info("Registering JsonApi4jDispatcherServlet on '{}'", mapping);
        return ServletBuildItem.builder(JSONAPI4J_DISPATCHER_SERVLET_NAME, JsonApi4jDispatcherServlet.class.getName())
                .addMapping(mapping)
                .setLoadOnStartup(1)
                .build();
    }

    @BuildStep
    void registerOasServlet(QuarkusJsonApi4jOasProperties oasProperties,
                            BuildProducer<ServletBuildItem> servlets) {

        if (isOasPluginEnabled(oasProperties)) {
            String mapping = toServletMapping(oasProperties.oasRootPath());
            LOG.info("{} plugin is enabled in properties ('jsonapi4j.oas.enabled') and related classes are present in classpath, registering OAS Servlet", OAS_PLUGIN_CLASSNAME);
            servlets.produce(
                    ServletBuildItem.builder("jsonApi4jOasServlet", "pro.api4.jsonapi4j.plugin.oas.OasServlet")
                            .addMapping(mapping)
                            .setLoadOnStartup(2)
                            .build()
            );
        } else {
            LOG.info("{} plugin is either disabled in properties ('jsonapi4j.oas.enabled') or related classes are not available in classpath, skipping OAS Servlet registration", OAS_PLUGIN_CLASSNAME);
        }
    }

    @BuildStep
    void registerCompoundDocsFilter(QuarkusJsonApi4jProperties jsonApi4jProperties,
                                    QuarkusJsonApi4jCompoundDocsProperties cdProperties,
                                    BuildProducer<FilterBuildItem> filters) {
        if (!cdProperties.enabled()) {
            LOG.info("Compound docs disabled, skipping CompoundDocsFilter registration");
            return;
        }

        String mapping = toServletMapping(jsonApi4jProperties.rootPath());
        LOG.info("Registering CompoundDocsFilter on '{}'", mapping);
        filters.produce(
                FilterBuildItem.builder("jsonapi4jCompoundDocsFilter", "pro.api4.jsonapi4j.plugin.cd.CompoundDocsFilter")
                        .addFilterUrlMapping(mapping, DispatcherType.REQUEST)
                        .setLoadOnStartup(2)
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
    AdditionalBeanBuildItem jsonapi4jCdiBeans(QuarkusJsonApi4jOasProperties oasProperties,
                                              QuarkusJsonApi4jAcProperties acProperties,
                                              QuarkusJsonApi4jSfProperties sfProperties,
                                              QuarkusJsonApi4jCompoundDocsProperties cdProperties) {
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClass(QuarkusJsonApi4jDispatcherServletContextListener.class)
                .addBeanClass(QuarkusJsonApi4jDefaultBeans.class)
                .addBeanClass(QuarkusJsonApi4jProperties.class);

        if (isAcPluginEnabled(acProperties)) {
            LOG.info("{} plugin is enabled, registering AC-related CDI beans", AC_PLUGIN_CLASSNAME);
            builder.addBeanClass(QuarkusJsonApi4jAcProperties.class.getName());
            builder.addBeanClass(QuarkusJsonApi4jAcPluginBeans.class.getName());
        } else {
            LOG.info("{} plugin is disabled, skipping AC-related CDI beans registration", AC_PLUGIN_CLASSNAME);
        }

        if (isOasPluginEnabled(oasProperties)) {
            LOG.info("{} plugin is enabled, registering OAS-related CDI beans", OAS_PLUGIN_CLASSNAME);
            builder.addBeanClass(QuarkusJsonApi4jOasServletContextListener.class.getName());
            builder.addBeanClass(QuarkusJsonApi4jOasProperties.class.getName());
            builder.addBeanClass(QuarkusJsonApi4jOasPluginBeans.class.getName());
        } else {
            LOG.info("{} plugin is disabled, skipping OAS-related CDI beans registration", OAS_PLUGIN_CLASSNAME);
        }

        if (isSfPluginEnabled(sfProperties)) {
            LOG.info("{} plugin is enabled, registering SF-related CDI beans", SF_PLUGIN_CLASSNAME);
            builder.addBeanClass(QuarkusJsonApi4jSfProperties.class.getName());
            builder.addBeanClass(QuarkusJsonApi4jSfPluginBeans.class.getName());
        } else {
            LOG.info("{} plugin is disabled, skipping SF-related CDI beans registration", SF_PLUGIN_CLASSNAME);
        }

        if (isCdPluginEnabled(cdProperties)) {
            LOG.info("Compound Docs plugin is enabled, registering CD-related CDI beans");
            builder.addBeanClass(QuarkusJsonApi4jCompoundDocsProperties.class.getName());
            builder.addBeanClass(QuarkusJsonApi4jCompoundDocsServletContextListener.class.getName());
            builder.addBeanClass(QuarkusJsonApi4jCompoundDocsPluginBeans.class.getName());
        } else {
            LOG.info("Compound Docs plugin is disabled, skipping CD-related CDI beans registration");
        }

        return builder.build();
    }

    @BuildStep
    ListenerBuildItem jsonApi4jDispatcherServletContextListener() {
        return new ListenerBuildItem(QuarkusJsonApi4jDispatcherServletContextListener.class.getName());
    }

    @BuildStep
    void oasServletContextListener(BuildProducer<ListenerBuildItem> listeners,
                                   QuarkusJsonApi4jOasProperties oasProperties) {
        if (isOasPluginEnabled(oasProperties)) {
            listeners.produce(new ListenerBuildItem(QuarkusJsonApi4jOasServletContextListener.class.getName()));
        }
    }

    @BuildStep
    void cdServletContextListener(BuildProducer<ListenerBuildItem> listeners,
                                   QuarkusJsonApi4jCompoundDocsProperties cdProperties) {
        if (isCdPluginEnabled(cdProperties)) {
            listeners.produce(new ListenerBuildItem(QuarkusJsonApi4jCompoundDocsServletContextListener.class.getName()));
        }
    }

    @BuildStep
    IgnoredServletContainerInitializerBuildItem ignoreJsonApi4jDispatcherServletContextInitializer() {
        return new IgnoredServletContainerInitializerBuildItem(
                JsonApi4jServletContainerInitializer.class.getName()
        );
    }

    @BuildStep
    void ignoreJsonApi4OasServletContextInitializer(
            BuildProducer<IgnoredServletContainerInitializerBuildItem> producer
    ) {
        if (isClassPresent(OAS_PLUGIN_CLASSNAME)) {
            producer.produce(
                    new IgnoredServletContainerInitializerBuildItem("pro.api4.jsonapi4j.plugin.oas.init.JsonApiOasServletContainerInitializer")
            );
        }
    }

    @BuildStep
    void ignoreJsonApi4CdServletContextInitializer(
            BuildProducer<IgnoredServletContainerInitializerBuildItem> producer
    ) {
        if (isClassPresent(CD_SERVLET_INITIALIZER_CLASSNAME)) {
            producer.produce(
                    new IgnoredServletContainerInitializerBuildItem(CD_SERVLET_INITIALIZER_CLASSNAME)
            );
        }
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

    private static boolean isAcPluginEnabled(QuarkusJsonApi4jAcProperties acProperties) {
        boolean acPluginClassesPresentInClasspath = isClassPresent(AC_PLUGIN_CLASSNAME);
        boolean enabled = acProperties.enabled();
        return enabled && acPluginClassesPresentInClasspath;
    }

    private static boolean isOasPluginEnabled(QuarkusJsonApi4jOasProperties oasProperties) {
        boolean oasPluginClassesPresentInClasspath = isClassPresent(OAS_PLUGIN_CLASSNAME);
        boolean enabled = oasProperties.enabled();
        return enabled && oasPluginClassesPresentInClasspath;
    }

    private static boolean isSfPluginEnabled(QuarkusJsonApi4jSfProperties sfProperties) {
        boolean sfPluginClassesPresentInClasspath = isClassPresent(SF_PLUGIN_CLASSNAME);
        boolean enabled = sfProperties.enabled();
        return enabled && sfPluginClassesPresentInClasspath;
    }

    private static boolean isCdPluginEnabled(QuarkusJsonApi4jCompoundDocsProperties cdProperties) {
        boolean cdPluginClassesPresentInClasspath = isClassPresent(CD_SERVLET_INITIALIZER_CLASSNAME);
        boolean enabled = cdProperties.enabled();
        return enabled && cdPluginClassesPresentInClasspath;
    }

}
