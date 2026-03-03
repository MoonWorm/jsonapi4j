package pro.api4.jsonapi4j.rest.quarkus.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.undertow.deployment.IgnoredServletContainerInitializerBuildItem;
import io.quarkus.undertow.deployment.ListenerBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem;
import pro.api4.jsonapi4j.rest.quarkus.runtime.JsonApi4jContextListener;
import pro.api4.jsonapi4j.rest.quarkus.runtime.JsonApi4jDefaultBeans;

import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.JSONAPI4J_DISPATCHER_SERVLET_NAME;

class Jsonapi4jRestQuarkusProcessor {

    private static final String FEATURE = "jsonapi4j-rest-quarkus";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ServletBuildItem registerServlet() {
        return ServletBuildItem.builder(
                        JSONAPI4J_DISPATCHER_SERVLET_NAME,
                        "pro.api4.jsonapi4j.servlet.JsonApi4jDispatcherServlet")
                .addMapping("/jsonapi/*")
                .setLoadOnStartup(1)
                .build();
    }

    @BuildStep
    AdditionalBeanBuildItem jsonapi4jCdiBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(JsonApi4jContextListener.class)
                .addBeanClass(JsonApi4jDefaultBeans.class)
                .setUnremovable()
                .build();
    }

    @BuildStep
    ListenerBuildItem servletContextListener() {
        return new ListenerBuildItem("pro.api4.jsonapi4j.rest.quarkus.runtime.JsonApi4jContextListener");
    }

    @BuildStep
    IgnoredServletContainerInitializerBuildItem ignoreJsonApi4jSci() {
        return new IgnoredServletContainerInitializerBuildItem(
                "pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer"
        );
    }

}
