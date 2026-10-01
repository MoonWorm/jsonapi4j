package pro.api4.jsonapi4j.plugin.cd.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pro.api4.jsonapi4j.compound.docs.DomainSettings;
import pro.api4.jsonapi4j.config.DefaultJsonApi4jProperties;
import pro.api4.jsonapi4j.config.MetaConfigComposer;
import pro.api4.jsonapi4j.config.PropertiesValidationResult;
import pro.api4.jsonapi4j.plugin.cd.JsonApiCompoundDocsPlugin;
import pro.api4.jsonapi4j.plugin.cd.config.DefaultCompoundDocsProperties.DefaultCache;
import pro.api4.jsonapi4j.plugin.PluginRegistry;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        public void validate_nullDeduplication_reportsError() {
            sut.setDeduplication(null);

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.cd.deduplication");
        }

        @Test
        public void validate_nullUnsupportedIncludes_reportsError() {
            sut.setUnsupportedIncludes(null);

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.cd.unsupportedIncludes");
        }

        @Test
        public void credentialHeaders_notConfigured_coverAuthenticationCookiesAndPrincipalHeaders() {
            assertThat(sut.credentialHeaders()).containsExactly(
                    "Authorization",
                    "Cookie",
                    "Proxy-Authorization",
                    "X-Authenticated-User-Id",
                    "X-Authenticated-User-Granted-Scopes",
                    "X-Authenticated-Client-Entitlements"
            );
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
    class DefaultMapping {

        @Test
        public void defaultMapping_defaultKeySet_returnsItsUrl() {
            sut.setMapping(Map.of("users", mapping(USERS_URL, null, false), "default", mapping(APP_URL, null, false)));

            assertThat(sut.defaultMapping()).contains(APP_URL);
        }

        @Test
        public void defaultMapping_defaultKeyNotSet_returnsEmpty() {
            assertThat(sut.defaultMapping()).isEmpty();
        }

        @Test
        public void domainSettings_defaultKeySet_excludesIt() {
            sut.setMapping(Map.of("users", mapping(USERS_URL, null, false), "default", mapping(APP_URL, null, false)));

            assertThat(sut.domainSettings()).containsOnlyKeys("users");
        }

        @Test
        public void validate_defaultMappingIsNotAbsolute_reportsError() {
            sut.setMapping(Map.of("default", mapping("/jsonapi", null, false)));

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.cd.mapping.default.url");
        }

        @Test
        public void validate_defaultMappingWithMoreThanUrl_reportsError() {
            sut.setMapping(Map.of("default", mapping(APP_URL, 50, true)));

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.cd.mapping.default");
        }

    }

    @Nested
    class Mapping {

        @Test
        public void domainSettings_mappedType_carriesItsUrlBatchSizeAndCredentials() {
            sut.setMapping(Map.of(
                    "users", mapping(USERS_URL, 50, true),
                    "rates", mapping("https://partner.example.com/jsonapi", null, false)
            ));

            assertThat(sut.domainSettings()).isEqualTo(Map.of(
                    "users", new DomainSettings(URI.create(USERS_URL), 50, true),
                    "rates", new DomainSettings(URI.create("https://partner.example.com/jsonapi"), sut.defaultMaxBatchSize(), false)
            ));
        }

        @Test
        public void domainSettings_typeWithoutUrl_isLeftToThisApp() {
            sut.setMapping(Map.of("countries", mapping(null, 100, false)));

            assertThat(sut.domainSettings()).isEmpty();
            assertThat(sut.maxBatchSize("countries")).isEqualTo(100);
            assertThat(sut.maxBatchSize("currencies")).isEqualTo(sut.defaultMaxBatchSize());
        }

        @ParameterizedTest
        @ValueSource(strings = {"/jsonapi", "users.foo.bar", ""})
        public void validate_mappedBaseUrlIsNotAbsolute_reportsError(String baseUrl) {
            sut.setMapping(Map.of("users", mapping(baseUrl, null, false)));

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.cd.mapping.users.url");
        }

        @Test
        public void validate_blankMappedResourceType_reportsError() {
            sut.setMapping(Map.of(" ", mapping(USERS_URL, null, false)));

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.cd.mapping");
        }

        @Test
        public void validate_nonPositiveBatchSize_reportsError() {
            sut.setMapping(Map.of("users", mapping(null, 0, false)));

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.cd.mapping.users.maxBatchSize");
        }

        @Test
        public void validate_mappingWithNeitherUrlNorBatchSize_reportsError() {
            sut.setMapping(Map.of("users", mapping(null, null, false)));

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.cd.mapping.users");
        }

        @Test
        public void validate_credentialsForTypeServedByThisApp_reportsError() {
            sut.setMapping(Map.of("users", mapping(null, 20, true)));

            assertThat(sut.validate().getPropertyErrors())
                    .containsOnlyKeys("jsonapi4j.cd.mapping.users.propagateCredentials");
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

            PropertiesValidationResult result = sut.validate();

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

    @Nested
    class ReportedPropertyPaths {

        @Test
        public void validate_everyReportedPath_pointsAtARealConfigKey() {
            // given
            Map<String, Object> effectiveConfig = effectiveConfigOf(sut);
            breakEveryValue(sut);

            // when
            Set<String> reportedPaths = sut.validate().getPropertyErrors().keySet();

            // then
            assertThat(reportedPaths).isNotEmpty();
            reportedPaths.forEach(path ->
                    assertThat(resolves(effectiveConfig, path)).as(path).isTrue()
            );
        }

        private void breakEveryValue(DefaultCompoundDocsProperties properties) {
            properties.setMaxHops(0);
            properties.setMaxIncludedResources(0);
            properties.setDefaultMaxBatchSize(0);
            properties.setHttpConnectTimeoutMs(0);
            properties.setHttpTotalTimeoutMs(0);
            properties.setErrorStrategy(null);
            properties.setPropagation(null);
            properties.setDeduplication(null);
            properties.setUnsupportedIncludes(null);
            properties.setCredentialHeaders(null);
            properties.setMapping(Map.of("users", mapping("/jsonapi", 0, false)));
            properties.getCache().setMaxSize(0);
        }

    }

    private static Map<String, Object> effectiveConfigOf(CompoundDocsProperties properties) {
        return MetaConfigComposer.compose(
                new DefaultJsonApi4jProperties(),
                PluginRegistry.builder().register(new JsonApiCompoundDocsPlugin(properties)).build()
        );
    }

    private static boolean resolves(Map<String, Object> effectiveConfig, String reportedPath) {
        Object current = effectiveConfig;
        for (String segment : reportedPath.replaceFirst("^jsonapi4j\\.", "").split("\\.")) {
            Matcher indexed = INDEXED_SEGMENT.matcher(segment);
            String name = indexed.matches() ? indexed.group(1) : segment;
            if (!(current instanceof Map<?, ?> node) || !node.containsKey(name)) {
                return false;
            }
            current = node.get(name);
            if (indexed.matches()) {
                int index = Integer.parseInt(indexed.group(2));
                if (!(current instanceof List<?> elements) || index >= elements.size()) {
                    return false;
                }
                current = elements.get(index);
            }
        }
        return true;
    }

    private static final String USERS_URL = "http://users.foo.bar/jsonapi";
    private static final String APP_URL = "https://api.internal/jsonapi";

    private static DefaultCompoundDocsProperties.DefaultMapping mapping(String url,
                                                                        Integer maxBatchSize,
                                                                        boolean propagateCredentials) {
        return new DefaultCompoundDocsProperties.DefaultMapping(url, maxBatchSize, propagateCredentials);
    }

    private static final Pattern INDEXED_SEGMENT = Pattern.compile("(.+)\\[(\\d+)]");

    private static DefaultCompoundDocsProperties validProperties() {
        DefaultCompoundDocsProperties properties = new DefaultCompoundDocsProperties();
        properties.setEnabled(true);
        properties.setMapping(Map.of("users", mapping(USERS_URL, 20, false)));
        properties.setCache(new DefaultCache());
        return properties;
    }

}
