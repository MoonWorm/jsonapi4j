package pro.api4.jsonapi4j.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PropertiesValidationResultTests {

    private final PropertiesValidationResult.PropertiesValidationResultBuilder sut =
            PropertiesValidationResult.builder();

    @Nested
    class Errors {

        @Test
        public void hasErrors_noErrorsAdded_returnsFalse() {
            assertThat(sut.build().hasErrors()).isFalse();
        }

        @Test
        public void hasErrors_onlyCrossPropertiesError_returnsTrue() {
            assertThat(sut.addCrossPropertiesError("inconsistent").build().hasErrors()).isTrue();
        }

        @Test
        public void addPropertyError_samePathTwice_keepsBothErrors() {
            PropertiesValidationResult result = sut
                    .addPropertyError("jsonapi4j.cd.mapping.users", "must be set")
                    .addPropertyError("jsonapi4j.cd.mapping.users", "must be an absolute URL")
                    .build();

            assertThat(result.getPropertyErrors())
                    .containsExactly(entry("jsonapi4j.cd.mapping.users", List.of("must be set", "must be an absolute URL")));
        }

        @Test
        public void addPropertyError_blankErrorMessage_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> sut.addPropertyError("jsonapi4j.cd.maxHops", " "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("error message shouldn't be blank");
        }

        @Test
        public void empty_always_hasNoErrors() {
            assertThat(PropertiesValidationResult.empty().hasErrors()).isFalse();
        }

    }

    @Nested
    class RequireNotBlank {

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        public void requireNotBlank_blankValue_reportsError(String value) {
            PropertiesValidationResult result = sut.requireNotBlank("jsonapi4j.oas.info.title", value).build();

            assertThat(result.getPropertyErrors())
                    .containsExactly(entry("jsonapi4j.oas.info.title", List.of("must be set to a non-blank value")));
        }

        @Test
        public void requireNotBlank_valueSet_reportsNothing() {
            assertThat(sut.requireNotBlank("jsonapi4j.oas.info.title", "My API").build().hasErrors()).isFalse();
        }

    }

    @Nested
    class RequirePositive {

        @Test
        public void requirePositive_zero_reportsError() {
            PropertiesValidationResult result = sut.requirePositive("jsonapi4j.cd.maxHops", 0).build();

            assertThat(result.getPropertyErrors())
                    .containsExactly(entry("jsonapi4j.cd.maxHops", List.of("must be greater than 0, but was 0")));
        }

        @Test
        public void requirePositive_null_reportsMissingValue() {
            PropertiesValidationResult result = sut.requirePositive("jsonapi4j.cd.maxHops", null).build();

            assertThat(result.getPropertyErrors())
                    .containsExactly(entry("jsonapi4j.cd.maxHops", List.of("must be set")));
        }

        @Test
        public void requirePositive_positiveValue_reportsNothing() {
            assertThat(sut.requirePositive("jsonapi4j.cd.maxHops", 2).build().hasErrors()).isFalse();
        }

    }

    @Nested
    class RequireHttpUrl {

        @ParameterizedTest
        @ValueSource(strings = {"/jsonapi", "users.foo.bar", "ftp://users.foo.bar", "http:///jsonapi"})
        public void requireHttpUrl_notAnAbsoluteHttpUrl_reportsError(String value) {
            PropertiesValidationResult result = sut.requireHttpUrl("jsonapi4j.cd.mapping.users", value).build();

            assertThat(result.getPropertyErrors()).containsOnlyKeys("jsonapi4j.cd.mapping.users");
        }

        @ParameterizedTest
        @ValueSource(strings = {"http://localhost:8080/jsonapi", "https://users.foo.bar/jsonapi", "HTTPS://users.foo.bar"})
        public void requireHttpUrl_absoluteHttpUrl_reportsNothing(String value) {
            assertThat(sut.requireHttpUrl("jsonapi4j.cd.mapping.users", value).build().hasErrors()).isFalse();
        }

        @Test
        public void requireHttpUrl_malformedUrl_reportsError() {
            PropertiesValidationResult result = sut.requireHttpUrl("jsonapi4j.cd.mapping.users", "http://foo bar").build();

            assertThat(result.getPropertyErrors()).containsOnlyKeys("jsonapi4j.cd.mapping.users");
        }

    }

    @Nested
    class RequireOneOfIgnoringCase {

        @Test
        public void requireOneOfIgnoringCase_unsupportedValue_reportsError() {
            PropertiesValidationResult result = sut
                    .requireOneOfIgnoringCase("jsonapi4j.oas.schema", "number", List.of("string", "integer"))
                    .build();

            assertThat(result.getPropertyErrors())
                    .containsExactly(entry("jsonapi4j.oas.schema", List.of("must be one of [string, integer], but was 'number'")));
        }

        @Test
        public void requireOneOfIgnoringCase_supportedValueInAnotherCase_reportsNothing() {
            PropertiesValidationResult result = sut
                    .requireOneOfIgnoringCase("jsonapi4j.oas.schema", "Integer", List.of("string", "integer"))
                    .build();

            assertThat(result.hasErrors()).isFalse();
        }

    }

    @Nested
    class AddAll {

        @Test
        public void addAll_otherResultWithBothErrorKinds_absorbsAllOfThem() {
            PropertiesValidationResult other = PropertiesValidationResult.builder()
                    .addPropertyError("jsonapi4j.oas.info.title", "must be set to a non-blank value")
                    .addCrossPropertiesError("flows share a name")
                    .build();

            PropertiesValidationResult result = sut
                    .addPropertyError("jsonapi4j.oas.oasRootPath", "must start with '/'")
                    .addAll(other)
                    .build();

            assertThat(result.getPropertyErrors())
                    .containsOnlyKeys("jsonapi4j.oas.oasRootPath", "jsonapi4j.oas.info.title");
            assertThat(result.getCrossPropertiesErrors()).containsExactly("flows share a name");
        }

        @Test
        public void addAll_bothResultsReportTheSamePath_keepsBothErrors() {
            PropertiesValidationResult other = PropertiesValidationResult.builder()
                    .addPropertyError("jsonapi4j.cd.mapping.users", "must be an absolute URL")
                    .build();

            PropertiesValidationResult result = sut
                    .addPropertyError("jsonapi4j.cd.mapping.users", "must be set")
                    .addAll(other)
                    .build();

            assertThat(result.getPropertyErrors())
                    .containsExactly(entry("jsonapi4j.cd.mapping.users", List.of("must be set", "must be an absolute URL")));
        }

        @Test
        public void addAll_emptyResult_reportsNothing() {
            assertThat(sut.addAll(PropertiesValidationResult.empty()).build().hasErrors()).isFalse();
        }

    }

    @Nested
    class RequireServletPath {

        @ParameterizedTest
        @ValueSource(strings = {"jsonapi", "/jsonapi/", " ", "/jsonapi/*", "/json?api"})
        public void requireServletPath_notMountable_reportsError(String value) {
            PropertiesValidationResult result = sut.requireServletPath("jsonapi4j.rootPath", value).build();

            assertThat(result.getPropertyErrors()).containsOnlyKeys("jsonapi4j.rootPath");
        }

        @ParameterizedTest
        @ValueSource(strings = {"/", "/jsonapi", "/api/v1/jsonapi"})
        public void requireServletPath_mountable_reportsNothing(String value) {
            assertThat(sut.requireServletPath("jsonapi4j.rootPath", value).build().hasErrors()).isFalse();
        }

    }

    @Nested
    class ToString {

        @Test
        public void toString_propertyAndCrossPropertiesErrors_rendersBothSections() {
            PropertiesValidationResult result = sut
                    .addPropertyError("jsonapi4j.cd.maxHops", "must be greater than 0, but was 0")
                    .addCrossPropertiesError("'jsonapi4j.cd.httpTotalTimeoutMs' must not be less than 'jsonapi4j.cd.httpConnectTimeoutMs'")
                    .build();

            assertThat(result).hasToString("""
                    Property errors:
                      - 'jsonapi4j.cd.maxHops': must be greater than 0, but was 0
                    Cross-properties errors:
                      - 'jsonapi4j.cd.httpTotalTimeoutMs' must not be less than 'jsonapi4j.cd.httpConnectTimeoutMs'
                    """);
        }

        @Test
        public void toString_noErrors_rendersNothing() {
            assertThat(sut.build()).hasToString("");
        }

    }

}
