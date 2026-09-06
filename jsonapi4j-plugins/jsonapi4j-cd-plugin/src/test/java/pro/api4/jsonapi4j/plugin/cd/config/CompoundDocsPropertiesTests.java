package pro.api4.jsonapi4j.plugin.cd.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pro.api4.jsonapi4j.config.PluginPropertiesValidationResult;
import pro.api4.jsonapi4j.plugin.cd.config.DefaultCompoundDocsProperties.DefaultCache;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CompoundDocsPropertiesTests {

    private final DefaultCompoundDocsProperties sut = validProperties();

    @Nested
    class Enablement {

        @Test
        public void validate_pluginDisabled_returnsNoErrors() {
            sut.setEnabled(false);
            sut.setMaxHops(0);

            assertThat(sut.validate().hasErrors()).isFalse();
        }

        @Test
        public void validate_validProperties_returnsNoErrors() {
            assertThat(sut.validate().hasErrors()).isFalse();
        }

    }

    @Nested
    class Limits {

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        public void validate_nonPositiveMaxHops_reportsError(int maxHops) {
            sut.setMaxHops(maxHops);

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.cd.maxHops");
        }

        @Test
        public void validate_nonPositiveMaxIncludedResources_reportsError() {
            sut.setMaxIncludedResources(0);

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.cd.maxIncludedResources");
        }

        @Test
        public void validate_nonPositiveDefaultMaxBatchSize_reportsError() {
            sut.setDefaultMaxBatchSize(0);

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.cd.defaultMaxBatchSize");
        }

        @Test
        public void validate_severalInvalidProperties_reportsAllOfThem() {
            sut.setMaxHops(0);
            sut.setMaxIncludedResources(-1);

            assertThat(sut.validate().getPropertyErrors())
                    .containsOnlyKeys("jsonapi4j.cd.maxHops", "jsonapi4j.cd.maxIncludedResources");
        }

    }

    @Nested
    class Mapping {

        @ParameterizedTest
        @ValueSource(strings = {"/jsonapi", "users.foo.bar", ""})
        public void validate_mappedBaseUrlIsNotAbsolute_reportsError(String baseUrl) {
            sut.setMapping(Map.of("users", baseUrl));

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.cd.mapping.users");
        }

        @Test
        public void validate_blankMappedResourceType_reportsError() {
            sut.setMapping(Map.of(" ", "http://users.foo.bar/jsonapi"));

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.cd.mapping");
        }

        @Test
        public void validate_nonPositiveBatchSize_reportsError() {
            sut.setBatchSizeMapping(Map.of("users", 0));

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.cd.batchSizeMapping.users");
        }

        @Test
        public void validate_batchSizeIsNotSet_reportsError() {
            Map<String, Integer> batchSizeMapping = new LinkedHashMap<>();
            batchSizeMapping.put("users", null);
            sut.setBatchSizeMapping(batchSizeMapping);

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.cd.batchSizeMapping.users");
        }

    }

    @Nested
    class Cache {

        @Test
        public void validate_cacheIsNotConfigured_returnsNoErrors() {
            sut.setCache(null);

            assertThat(sut.validate().hasErrors()).isFalse();
        }

        @Test
        public void validate_enabledCacheWithNonPositiveMaxSize_reportsError() {
            sut.getCache().setMaxSize(0);

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.cd.cache.maxSize");
        }

        @Test
        public void validate_disabledCacheWithNonPositiveMaxSize_returnsNoErrors() {
            sut.getCache().setEnabled(false);
            sut.getCache().setMaxSize(0);

            assertThat(sut.validate().hasErrors()).isFalse();
        }

    }

    @Nested
    class Timeouts {

        @Test
        public void validate_totalTimeoutBelowConnectTimeout_reportsCrossPropertiesError() {
            sut.setHttpConnectTimeoutMs(5000);
            sut.setHttpTotalTimeoutMs(1000);

            PluginPropertiesValidationResult result = sut.validate();

            assertThat(result.getPropertyErrors()).isEmpty();
            assertThat(result.getCrossPropertiesErrors()).hasSize(1);
        }

        @Test
        public void validate_totalTimeoutEqualToConnectTimeout_returnsNoErrors() {
            sut.setHttpConnectTimeoutMs(5000);
            sut.setHttpTotalTimeoutMs(5000);

            assertThat(sut.validate().hasErrors()).isFalse();
        }

        @Test
        public void validate_nonPositiveConnectTimeout_reportsError() {
            sut.setHttpConnectTimeoutMs(0);

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.cd.httpConnectTimeoutMs");
        }

    }

    private static DefaultCompoundDocsProperties validProperties() {
        DefaultCompoundDocsProperties properties = new DefaultCompoundDocsProperties();
        properties.setEnabled(true);
        properties.setMapping(Map.of("users", "http://users.foo.bar/jsonapi"));
        properties.setBatchSizeMapping(Map.of("users", 20));
        properties.setCache(new DefaultCache());
        return properties;
    }

}
