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
import pro.api4.jsonapi4j.compound.docs.DomainSettings;
import pro.api4.jsonapi4j.compound.docs.DomainSettingsResolver;
import pro.api4.jsonapi4j.compound.docs.config.Deduplication;
import pro.api4.jsonapi4j.plugin.cd.config.CompoundDocsProperties;

import java.net.URI;

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

    @Test
    void jsonApi4jCdProperties_deduplicationSet_isBound() {
        sut.withPropertyValues("jsonapi4j.cd.deduplication=INCLUDED_ONLY")
                .run(context -> assertThat(context.getBean(CompoundDocsProperties.class).deduplication())
                        .isEqualTo(Deduplication.INCLUDED_ONLY));
    }

    @Test
    void jsonApi4jCdDomainSettingsResolver_nestedMapping_isBound() {
        sut.withPropertyValues(
                        "jsonapi4j.cd.mapping.orders.url=https://orders.internal/jsonapi",
                        "jsonapi4j.cd.mapping.orders.maxBatchSize=50",
                        "jsonapi4j.cd.mapping.orders.propagateCredentials=true",
                        "jsonapi4j.cd.mapping.countries.maxBatchSize=100"
                )
                .run(context -> {
                    assertThat(context.getBean(DomainSettingsResolver.class).resolveDomainSettings("orders"))
                            .contains(new DomainSettings(URI.create("https://orders.internal/jsonapi"), 50, true));
                    assertThat(context.getBean(DomainSettingsResolver.class).resolveDomainSettings("countries"))
                            .isEmpty();
                    assertThat(context.getBean(CompoundDocsProperties.class).maxBatchSize("countries"))
                            .isEqualTo(100);
                });
    }

}
