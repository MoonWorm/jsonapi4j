package pro.api4.jsonapi4j.springboot.autoconfiguration.cd;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import pro.api4.jsonapi4j.springboot.autoconfiguration.SpringJsonApi4jAutoConfigurer;
import pro.api4.jsonapi4j.plugin.cd.JsonApiCompoundDocsPlugin;
import pro.api4.jsonapi4j.compound.docs.cache.CompoundDocsResourceCache;
import pro.api4.jsonapi4j.compound.docs.DomainSettingsResolver;

import static org.assertj.core.api.Assertions.assertThat;

class SpringJsonApi4jCompoundDocsConfigTests {

    /*
     * Runs the real topology rather than this config alone: its filter registration depends on the dispatcher
     * servlet bean that SpringJsonApi4jAutoConfigurer owns, and that configurer is what @Imports this config in
     * production.
     */
    private final WebApplicationContextRunner sut = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    SpringJsonApi4jAutoConfigurer.class
            ));

    @Test
    void jsonApiCompoundDocsPlugin_defaults_isRegistered() {
        sut.run(context -> assertThat(context).hasSingleBean(JsonApiCompoundDocsPlugin.class));
    }

    @Test
    void jsonApiCompoundDocsPlugin_disabledByProperty_isNotRegistered() {
        sut.withPropertyValues("jsonapi4j.cd.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(JsonApiCompoundDocsPlugin.class));
    }

    @Test
    void jsonApiCompoundDocsPlugin_pluginNotOnTheClasspath_startsWithoutIt() {
        sut.withClassLoader(new FilteredClassLoader(JsonApiCompoundDocsPlugin.class))
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void jsonApi4jCompoundDocsResourceCache_defaults_isRegistered() {
        sut.run(context -> assertThat(context).hasSingleBean(CompoundDocsResourceCache.class));
    }

    @Test
    void jsonApi4jCompoundDocsResourceCache_cacheDisabledByProperty_isNotRegistered() {
        sut.withPropertyValues("jsonapi4j.cd.cache.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(CompoundDocsResourceCache.class));
    }

    @Test
    void jsonApi4jCdDomainSettingsResolver_userBean_backsOff() {
        DomainSettingsResolver userResolver = Mockito.mock(DomainSettingsResolver.class);

        sut.withBean("userDomainSettingsResolver", DomainSettingsResolver.class, () -> userResolver)
                .run(context -> assertThat(context.getBean(DomainSettingsResolver.class)).isSameAs(userResolver));
    }
}
