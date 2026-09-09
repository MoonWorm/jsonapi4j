package pro.api4.jsonapi4j.springboot.autoconfiguration.sf;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import pro.api4.jsonapi4j.plugin.sf.JsonApiSparseFieldsetsPlugin;

import static org.assertj.core.api.Assertions.assertThat;

class SpringJsonApi4jSfPluginConfigTests {

    private final ApplicationContextRunner sut = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    SpringJsonApi4jSfPluginConfig.class
            ));

    @Test
    void jsonApiSparseFieldsetsPlugin_defaults_isRegistered() {
        sut.run(context -> assertThat(context).hasSingleBean(JsonApiSparseFieldsetsPlugin.class));
    }

    @Test
    void jsonApiSparseFieldsetsPlugin_disabledByProperty_isNotRegistered() {
        sut.withPropertyValues("jsonapi4j.sf.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(JsonApiSparseFieldsetsPlugin.class));
    }

    @Test
    void jsonApiSparseFieldsetsPlugin_pluginNotOnTheClasspath_startsWithoutIt() {
        sut.withClassLoader(new FilteredClassLoader(JsonApiSparseFieldsetsPlugin.class))
                .run(context -> assertThat(context).hasNotFailed());
    }
}
