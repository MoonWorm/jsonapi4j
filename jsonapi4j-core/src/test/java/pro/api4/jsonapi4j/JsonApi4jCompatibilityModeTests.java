package pro.api4.jsonapi4j;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.compatibility.JsonApi4jCompatibilityMode;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.operation.OperationsRegistry;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonApi4jCompatibilityModeTests {

    @Test
    public void builderWithoutMode_defaultsToStrictMode() {
        // given - when
        JsonApi4j sut = JsonApi4j.builder()
                .domainRegistry(DomainRegistry.empty())
                .operationsRegistry(OperationsRegistry.empty())
                .build();

        // then
        assertThat(sut.getCompatibilityMode()).isEqualTo(JsonApi4jCompatibilityMode.STRICT);
    }

    @Test
    public void builderWithMode_setsConfiguredCompatibilityMode() {
        // given - when
        JsonApi4j sut = JsonApi4j.builder()
                .domainRegistry(DomainRegistry.empty())
                .operationsRegistry(OperationsRegistry.empty())
                .compatibilityMode(JsonApi4jCompatibilityMode.LEGACY)
                .build();

        // then
        assertThat(sut.getCompatibilityMode()).isEqualTo(JsonApi4jCompatibilityMode.LEGACY);
    }
}
