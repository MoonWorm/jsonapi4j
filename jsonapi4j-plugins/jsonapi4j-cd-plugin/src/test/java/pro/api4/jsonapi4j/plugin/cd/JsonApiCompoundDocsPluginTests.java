package pro.api4.jsonapi4j.plugin.cd;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;
import pro.api4.jsonapi4j.plugin.cd.config.DefaultCompoundDocsProperties;
import pro.api4.jsonapi4j.plugin.exception.PluginMisconfigurationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JsonApiCompoundDocsPluginTests {

    private final DefaultCompoundDocsProperties properties = new DefaultCompoundDocsProperties();
    private final JsonApiCompoundDocsPlugin sut = new JsonApiCompoundDocsPlugin(properties);

    @Nested
    class Enabled {

        @Test
        public void enabled_enabledProperties_returnsTrue() {
            properties.setEnabled(true);

            assertThat(sut.enabled()).isTrue();
        }

        @Test
        public void enabled_disabledProperties_returnsFalse() {
            properties.setEnabled(false);

            assertThat(sut.enabled()).isFalse();
        }

    }

    @Nested
    class ErrorHandlerFactory {

        @Test
        public void errorHandlerFactory_providesCompoundDocsErrorHandlers() {
            assertThat(sut.errorHandlerFactory()).isInstanceOf(CompoundDocsErrorHandlerFactory.class);
        }

    }

    @Nested
    class ExtractPluginInfoFromResource {

        @Test
        public void extractPluginInfoFromResource_resourceTypeNamedDefault_throwsPluginMisconfigurationException() {
            assertThatThrownBy(() -> sut.extractPluginInfoFromResource(new DefaultResource()))
                    .isInstanceOf(PluginMisconfigurationException.class)
                    .hasMessageContaining("'default'")
                    .hasMessageContaining("jsonapi4j.cd.mapping.default");
        }

        @Test
        public void extractPluginInfoFromResource_otherResourceType_returnsNull() {
            assertThat(sut.extractPluginInfoFromResource(new UsersResource())).isNull();
        }

    }

    @JsonApiResource(resourceType = "default")
    private static class DefaultResource implements Resource<String> {

        @Override
        public String resolveResourceId(String dataSourceDto) {
            return dataSourceDto;
        }

    }

    @JsonApiResource(resourceType = "users")
    private static class UsersResource implements Resource<String> {

        @Override
        public String resolveResourceId(String dataSourceDto) {
            return dataSourceDto;
        }

    }

}
