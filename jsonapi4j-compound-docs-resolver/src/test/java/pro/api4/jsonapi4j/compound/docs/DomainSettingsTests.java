package pro.api4.jsonapi4j.compound.docs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainSettingsTests {

    private static final URI URL = URI.create("http://example.com");

    @Nested
    class OverHttp {

        @Test
        void overHttp_allSettings_setsThem() {
            DomainSettings.OverHttp settings = DomainSettings.overHttp(URL, 50, true);

            assertThat(settings.url()).isEqualTo(URL);
            assertThat(settings.maxBatchSize()).isEqualTo(50);
            assertThat(settings.propagateCredentials()).isTrue();
        }

        @Test
        void overHttp_urlOnly_usesDefaultBatchSizeWithoutCredentials() {
            DomainSettings.OverHttp settings = DomainSettings.overHttp(URL);

            assertThat(settings.maxBatchSize()).isEqualTo(DomainSettings.DEFAULT_MAX_BATCH_SIZE);
            assertThat(settings.propagateCredentials()).isFalse();
        }

        @Test
        void overHttp_urlAndBatchSize_isNotTrustedWithCredentials() {
            assertThat(DomainSettings.overHttp(URL, 10).propagateCredentials()).isFalse();
        }

        @Test
        void overHttp_nullUrl_throwsNpe() {
            assertThatThrownBy(() -> DomainSettings.overHttp(null, 10))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("url must not be null");
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        void overHttp_nonPositiveBatchSize_throwsIllegalArgument(int maxBatchSize) {
            assertThatThrownBy(() -> DomainSettings.overHttp(URL, maxBatchSize))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("maxBatchSize must be positive");
        }

    }

    @Nested
    class InProcess {

        @Test
        void inProcess_batchSize_setsIt() {
            assertThat(DomainSettings.inProcess(30).maxBatchSize()).isEqualTo(30);
        }

        @Test
        void inProcess_nonPositiveBatchSize_throwsIllegalArgument() {
            assertThatThrownBy(() -> DomainSettings.inProcess(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("maxBatchSize must be positive");
        }

    }

    @Test
    void defaultMaxBatchSize_is20() {
        assertThat(DomainSettings.DEFAULT_MAX_BATCH_SIZE).isEqualTo(20);
    }

}
