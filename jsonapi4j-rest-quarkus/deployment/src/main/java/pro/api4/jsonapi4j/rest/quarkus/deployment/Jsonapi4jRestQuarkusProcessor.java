package pro.api4.jsonapi4j.rest.quarkus.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.undertow.deployment.FilterBuildItem;
import io.quarkus.undertow.deployment.IgnoredServletContainerInitializerBuildItem;
import io.quarkus.undertow.deployment.ListenerBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem;
import jakarta.servlet.DispatcherType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.api4.jsonapi4j.rest.quarkus.runtime.QuarkusJsonApi4jContextListener;
import pro.api4.jsonapi4j.rest.quarkus.runtime.QuarkusJsonApi4jDefaultBeans;
import pro.api4.jsonapi4j.rest.quarkus.runtime.QuarkusJsonApi4jProperties;

import java.util.Collections;
import java.util.List;

import static pro.api4.jsonapi4j.config.CompoundDocsProperties.JSONAPI4J_COMPOUND_DOCS_ENABLED_DEFAULT_VALUE;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.JSONAPI4J_COMPOUND_DOCS_FILTER_NAME;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.JSONAPI4J_DISPATCHER_SERVLET_NAME;

class Jsonapi4jRestQuarkusProcessor {

    private static final Logger log = LoggerFactory.getLogger(Jsonapi4jRestQuarkusProcessor.class);

    private static final String FEATURE = "jsonapi4j-rest-quarkus";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ServletBuildItem registerServlet(QuarkusJsonApi4jProperties props) {
        log.info("Registering JsonApi4jDispatcherServlet...");
        return ServletBuildItem.builder(
                        JSONAPI4J_DISPATCHER_SERVLET_NAME,
                        "pro.api4.jsonapi4j.servlet.JsonApi4jDispatcherServlet")
                .addMapping(props.rootPath() + "/*")
                .setLoadOnStartup(1)
                .build();
    }

    @BuildStep
    List<FilterBuildItem> registerCompoundDocsFilter(QuarkusJsonApi4jProperties props) {
        boolean compoundDocsEnabled = props == null || props.compoundDocs() == null
                ? Boolean.parseBoolean(JSONAPI4J_COMPOUND_DOCS_ENABLED_DEFAULT_VALUE)
                : props.compoundDocs().enabled();
        if (compoundDocsEnabled) {
            log.info("Compound Docs feature enabled - registering CompoundDocsFilter...");
            return List.of(
                    FilterBuildItem.builder(
                                    JSONAPI4J_COMPOUND_DOCS_FILTER_NAME,
                                    "pro.api4.jsonapi4j.filter.cd.CompoundDocsFilter")
                            .addFilterUrlMapping(props.rootPath() + "/*", DispatcherType.REQUEST)
                            .setLoadOnStartup(1)
                            .build()
            );
        }
        log.info("Compound Docs feature disabled - not registering CompoundDocsFilter...");
        return Collections.emptyList();
    }

    @BuildStep
    AdditionalBeanBuildItem jsonapi4jCdiBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(QuarkusJsonApi4jContextListener.class)
                .addBeanClass(QuarkusJsonApi4jDefaultBeans.class)
                .addBeanClass(QuarkusJsonApi4jProperties.class)
                .setUnremovable()
                .build();
    }

    @BuildStep
    ListenerBuildItem servletContextListener() {
        return new ListenerBuildItem("pro.api4.jsonapi4j.rest.quarkus.runtime.QuarkusJsonApi4jContextListener");
    }

    @BuildStep
    IgnoredServletContainerInitializerBuildItem ignoreJsonApi4jSci() {
        return new IgnoredServletContainerInitializerBuildItem(
                "pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer"
        );
    }

}
