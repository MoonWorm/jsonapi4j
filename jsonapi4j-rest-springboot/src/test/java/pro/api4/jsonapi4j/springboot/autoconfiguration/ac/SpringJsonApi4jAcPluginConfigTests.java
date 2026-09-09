package pro.api4.jsonapi4j.springboot.autoconfiguration.ac;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import pro.api4.jsonapi4j.plugin.ac.AccessControlEvaluator;
import pro.api4.jsonapi4j.plugin.ac.JsonApiAccessControlPlugin;

import static org.assertj.core.api.Assertions.assertThat;

class SpringJsonApi4jAcPluginConfigTests {

    private final ApplicationContextRunner sut = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    SpringJsonApi4jAcPluginConfig.class
            ));

    @Test
    void jsonApiAccessControlPlugin_defaults_isRegistered() {
        sut.run(context -> assertThat(context).hasSingleBean(JsonApiAccessControlPlugin.class));
    }

    @Test
    void jsonApiAccessControlPlugin_disabledByProperty_isNotRegistered() {
        sut.withPropertyValues("jsonapi4j.ac.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(JsonApiAccessControlPlugin.class));
    }

    @Test
    void jsonApiAccessControlPlugin_pluginNotOnTheClasspath_startsWithoutIt() {
        sut.withClassLoader(new FilteredClassLoader(JsonApiAccessControlPlugin.class))
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void jsonapi4jAccessControlEvaluator_userBean_backsOff() {
        AccessControlEvaluator userEvaluator = Mockito.mock(AccessControlEvaluator.class);

        sut.withBean("userAccessControlEvaluator", AccessControlEvaluator.class, () -> userEvaluator)
                .run(context -> assertThat(context.getBean(AccessControlEvaluator.class)).isSameAs(userEvaluator));
    }
}
