package pro.api4.jsonapi4j.rest.quarkus.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem;
import pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer;

class Jsonapi4jRestQuarkusProcessor {

    private static final String FEATURE = "jsonapi4j-rest-quarkus";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ServletBuildItem registerServlet() {
        return ServletBuildItem.builder(
                        JsonApi4jServletContainerInitializer.JSONAPI4J_DISPATCHER_SERVLET_NAME,
                        "pro.api4.jsonapi4j.servlet.JsonApi4jDispatcherServlet")
                .addMapping("/jsonapi/*")
                .build();
    }

}
