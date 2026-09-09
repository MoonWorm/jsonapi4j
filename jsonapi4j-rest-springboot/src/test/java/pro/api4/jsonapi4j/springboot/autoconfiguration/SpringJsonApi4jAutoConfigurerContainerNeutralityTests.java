package pro.api4.jsonapi4j.springboot.autoconfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.meta.context.MetaContext;
import pro.api4.jsonapi4j.plugin.ac.JsonApiAccessControlPlugin;
import pro.api4.jsonapi4j.plugin.cd.JsonApiCompoundDocsPlugin;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.plugin.sf.JsonApiSparseFieldsetsPlugin;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * A single Tomcat type referenced from the main configuration class used
 * to fail the whole auto-configuration on Jetty or Undertow - not just the Tomcat bean, because Spring resolves
 * every type in every bean method while introspecting the class, so unrelated beans died with it. That was found
 * by swapping the container by hand and could regress silently; these tests keep it honest in CI.
 */
class SpringJsonApi4jAutoConfigurerContainerNeutralityTests {

    private final WebApplicationContextRunner sut = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    SpringJsonApi4jAutoConfigurer.class
            ));

    @Test
    void autoConfiguration_withoutCatalinaOnTheClasspath_startsClean() {
        sut.withClassLoader(new FilteredClassLoader("org.apache.catalina"))
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void autoConfiguration_withoutCatalinaOnTheClasspath_stillRegistersUnrelatedBeans() {
        sut.withClassLoader(new FilteredClassLoader("org.apache.catalina"))
                .withPropertyValues("jsonapi4j.meta.enabled=true")
                .run(context -> assertThat(context)
                        .hasSingleBean(JsonApi4j.class)
                        .hasSingleBean(DomainRegistry.class)
                        .hasSingleBean(MetaContext.class)
                        .hasBean("jsonApi4jDispatcherServlet"));
    }

    @Test
    void autoConfiguration_withoutOptionalPluginsOnTheClasspath_startsClean() {
        sut.withClassLoader(new FilteredClassLoader(
                        JsonApiOasPlugin.class,
                        JsonApiAccessControlPlugin.class,
                        JsonApiSparseFieldsetsPlugin.class,
                        JsonApiCompoundDocsPlugin.class))
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(JsonApi4j.class));
    }
}
