package pro.api4.jsonapi4j.compound.docs;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainSettingsTests {

    private static final URI URL = URI.create("http://example.com");

    @Test
    void constructor_validInputs_setsFields() {
        DomainSettings settings = new DomainSettings(URL, 50);

        assertThat(settings.url()).isEqualTo(URL);
        assertThat(settings.maxBatchSize()).isEqualTo(50);
    }

    @Test
    void of_usesDefaultMaxBatchSize() {
        DomainSettings settings = DomainSettings.of(URL);

        assertThat(settings.url()).isEqualTo(URL);
        assertThat(settings.maxBatchSize()).isEqualTo(DomainSettings.DEFAULT_MAX_BATCH_SIZE);
    }

    @Test
    void defaultMaxBatchSize_is20() {
        assertThat(DomainSettings.DEFAULT_MAX_BATCH_SIZE).isEqualTo(20);
    }

    @Test
    void constructor_nullUrl_throwsNpe() {
        assertThatThrownBy(() -> new DomainSettings(null, 10))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("url must not be null");
    }

    @Test
    void constructor_zeroBatchSize_throwsIllegalArgument() {
        assertThatThrownBy(() -> new DomainSettings(URL, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxBatchSize must be positive");
    }

    @Test
    void constructor_negativeBatchSize_throwsIllegalArgument() {
        assertThatThrownBy(() -> new DomainSettings(URL, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxBatchSize must be positive");
    }
}
