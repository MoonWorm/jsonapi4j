package pro.api4.jsonapi4j.rest.quarkus.deployment;

import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.plugin.PluginRegistry;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * Boots a real augmentation so the build steps run the way Quarkus runs them. Until now nothing exercised this
 * module at all: a build step that stopped producing its BuildItem, or an extension that failed to augment, would
 * only have surfaced when someone started an application.
 */
class QuarkusJsonApi4jProcessorTests {

    @RegisterExtension
    static final QuarkusUnitTest sut = new QuarkusUnitTest()
            .withEmptyApplication();

    @Inject
    JsonApi4j jsonApi4j;

    @Inject
    DomainRegistry domainRegistry;

    @Inject
    OperationsRegistry operationsRegistry;

    @Inject
    PluginRegistry pluginRegistry;

    @Test
    void augmentation_emptyApplication_producesTheFrameworkBeans() {
        assertThat(jsonApi4j).isNotNull();
        assertThat(domainRegistry).isNotNull();
        assertThat(operationsRegistry).isNotNull();
        assertThat(pluginRegistry).isNotNull();
    }

    @Test
    void augmentation_emptyApplication_registersTheBuiltInMetaDomain() {
        assertThat(jsonApi4j.getDomainRegistry().getResources())
                .extracting(registered -> registered.getResourceType().getType())
                .contains("resources", "relationships", "operations", "plugins");
    }

    @Test
    void augmentation_emptyApplication_appliesTheDefaultRootPath() {
        assertThat(jsonApi4j.getProperties().rootPath()).isEqualTo("/jsonapi");
    }
}
