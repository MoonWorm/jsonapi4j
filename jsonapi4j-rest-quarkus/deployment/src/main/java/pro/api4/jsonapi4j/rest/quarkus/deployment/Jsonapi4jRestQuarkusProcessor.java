package pro.api4.jsonapi4j.rest.quarkus.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem;
import io.quarkus.undertow.deployment.ServletContextAttributeBuildItem;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.rest.quarkus.runtime.JsonApi4jRecorder;

import java.util.Collections;

import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.*;

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

    /*@Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    ServletContextAttributeBuildItem servletContextAttributeObjectMapper(JsonApi4jRecorder recorder) {
        return new ServletContextAttributeBuildItem(
                OBJECT_MAPPER_ATT_NAME,
                recorder.createObjectMapper()
        );
    }

    @BuildStep
    ServletContextAttributeBuildItem servletContextAttributeJsonApi4j() {
        JsonApi4j jsonApi4j = JsonApi4j.builder()
                .domainRegistry(DomainRegistry.empty())
                .operationsRegistry(OperationsRegistry.empty())
                //.executor(Executors.newCachedThreadPool())
                .plugins(Collections.emptyList())
                .build();
        return new ServletContextAttributeBuildItem(JSONAPI4J_ATT_NAME, jsonApi4j);
    }*/

}
