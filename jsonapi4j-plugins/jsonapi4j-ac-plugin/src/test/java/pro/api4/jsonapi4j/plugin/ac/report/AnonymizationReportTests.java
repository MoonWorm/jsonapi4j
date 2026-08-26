package pro.api4.jsonapi4j.plugin.ac.report;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.model.document.error.AuthErrorCodes;
import pro.api4.jsonapi4j.plugin.ac.AnonymizationResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static pro.api4.jsonapi4j.plugin.ac.config.AnonymizationReportLevel.FIELDS;
import static pro.api4.jsonapi4j.plugin.ac.config.AnonymizationReportLevel.FIELDS_AND_REASONS;
import static pro.api4.jsonapi4j.plugin.ac.config.AnonymizationReportLevel.INDICATOR;
import static pro.api4.jsonapi4j.plugin.ac.config.AnonymizationReportLevel.NONE;

class AnonymizationReportTests {

    private static AnonymizationResult<String> hid(String... paths) {
        Map<String, pro.api4.jsonapi4j.model.document.error.ErrorCode> fields = new LinkedHashMap<>();
        for (String path : paths) {
            fields.put(path, AuthErrorCodes.INSUFFICIENT_SCOPES);
        }
        return new AnonymizationResult<>("kept", false, fields);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> fieldsOf(Map<String, Object> report) {
        return (List<Map<String, Object>>) report.get("fields");
    }

    @Nested
    class Levels {

        @Test
        void of_none_saysNothingAtAll() {
            assertThat(AnonymizationReport.of(hid("zip"), NONE)).isNull();
        }

        @Test
        void of_indicator_saysSomethingWasHiddenWithoutNamingIt() {
            Map<String, Object> actualResult = AnonymizationReport.of(hid("zip"), INDICATOR);

            assertThat(actualResult).containsEntry("anonymized", true);
            assertThat(actualResult).doesNotContainKey("fields");
        }

        @Test
        void of_fields_namesThePathsWithoutTheReason() {
            Map<String, Object> actualResult = AnonymizationReport.of(hid("zip", "addresses[0].doorCode"), FIELDS);

            assertThat(fieldsOf(actualResult)).extracting(f -> f.get("path"))
                    .containsExactly("zip", "addresses[0].doorCode");
            assertThat(fieldsOf(actualResult)).allSatisfy(f -> assertThat(f).doesNotContainKey("reason"));
        }

        @Test
        void of_fieldsAndReasons_addsTheRefusingCode() {
            Map<String, Object> actualResult = AnonymizationReport.of(hid("zip"), FIELDS_AND_REASONS);

            assertThat(fieldsOf(actualResult).getFirst()).containsEntry("reason", "INSUFFICIENT_SCOPES");
        }

        @Test
        void of_nothingHidden_saysNothingEvenAtTheHighestLevel() {
            assertThat(AnonymizationReport.of(new AnonymizationResult<>("kept"), FIELDS_AND_REASONS)).isNull();
        }

        @Test
        void of_wholeObjectDenied_saysSoWithoutListingPaths() {
            AnonymizationResult<String> result =
                    new AnonymizationResult<>(null, true, Map.of("", AuthErrorCodes.FORBIDDEN));

            Map<String, Object> actualResult = AnonymizationReport.of(result, FIELDS_AND_REASONS);

            assertThat(actualResult).containsEntry("fullyAnonymized", true);
            assertThat(actualResult).doesNotContainKey("fields");
        }

    }

    @Nested
    @SuppressWarnings("unchecked")
    class Merging {

        @Test
        void mergeInto_noMeta_createsOne() {
            Object actualResult = AnonymizationReport.mergeInto(null, Map.of("anonymized", true));

            assertThat(actualResult).isInstanceOf(Map.class);
            assertThat((Map<String, Object>) actualResult).containsKey("accessControl");
        }

        @Test
        void mergeInto_existingMeta_keepsWhatWasThere() {
            Object actualResult = AnonymizationReport.mergeInto(
                    Map.of("internalRef", "internal-1"), Map.of("anonymized", true));

            assertThat((Map<String, Object>) actualResult)
                    .containsEntry("internalRef", "internal-1")
                    .containsKey("accessControl");
        }

        @Test
        void mergeInto_existingMeta_isNotModified() {
            Map<String, Object> original = new LinkedHashMap<>();
            original.put("internalRef", "internal-1");

            AnonymizationReport.mergeInto(original, Map.of("anonymized", true));

            assertThat(original).containsOnlyKeys("internalRef");
        }

        @Test
        void mergeInto_metaThatIsNotAMap_isLeftAlone() {
            Object applicationMeta = "a meta of the application's own making";

            Object actualResult = AnonymizationReport.mergeInto(applicationMeta, Map.of("anonymized", true));

            assertThat(actualResult).isSameAs(applicationMeta);
        }

        @Test
        void mergeInto_nothingToReport_returnsTheMetaUnchanged() {
            Map<String, Object> meta = Map.of("internalRef", "internal-1");

            assertThat(AnonymizationReport.mergeInto(meta, null)).isSameAs(meta);
        }

    }

}
