package pro.api4.jsonapi4j.plugin.cd;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.plugin.cd.config.DefaultCompoundDocsProperties;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonApiCompoundDocsPluginTests {

    private final DefaultCompoundDocsProperties properties = new DefaultCompoundDocsProperties();
    private final JsonApiCompoundDocsPlugin sut = new JsonApiCompoundDocsPlugin(properties);

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
