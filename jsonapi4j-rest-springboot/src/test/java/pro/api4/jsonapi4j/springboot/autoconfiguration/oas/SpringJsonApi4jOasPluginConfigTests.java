package pro.api4.jsonapi4j.springboot.autoconfiguration.oas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;

import static org.assertj.core.api.Assertions.assertThat;

class SpringJsonApi4jOasPluginConfigTests {

    private final ApplicationContextRunner sut = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    SpringJsonApi4jOasPluginConfig.class
            ));

    @Test
    void jsonApiOasPlugin_defaults_isRegistered() {
        sut.run(context -> assertThat(context).hasSingleBean(JsonApiOasPlugin.class));
    }

    @Test
    void jsonApiOasPlugin_defaults_registersTheOasServlet() {
        sut.run(context -> assertThat(context)
                .hasBean("jsonApi4jOasServlet")
                .hasBean("jsonApi4jOasServletContextInitializer"));
    }

    @Test
    void jsonApiOasPlugin_disabledByProperty_isNotRegistered() {
        sut.withPropertyValues("jsonapi4j.oas.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(JsonApiOasPlugin.class));
    }

    @Test
    void jsonApiOasPlugin_pluginNotOnTheClasspath_startsWithoutIt() {
        sut.withClassLoader(new FilteredClassLoader(JsonApiOasPlugin.class))
                .run(context -> assertThat(context).hasNotFailed());
    }
}
