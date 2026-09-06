package pro.api4.jsonapi4j.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonApi4jPropertiesTests {

    private final DefaultJsonApi4jProperties sut = new DefaultJsonApi4jProperties();

    @Test
    public void validate_defaultProperties_returnsNoErrors() {
        assertThat(sut.validate().hasErrors()).isFalse();
    }

    @Nested
    class RootPath {

        @ParameterizedTest
        @ValueSource(strings = {"jsonapi", "/jsonapi/", " ", "/json*api", "/jsonapi/*"})
        public void validate_rootPathIsNotAServletMapping_reportsError(String rootPath) {
            sut.setRootPath(rootPath);

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.rootPath");
        }

        @ParameterizedTest
        @ValueSource(strings = {"/", "/jsonapi", "/api/v1/jsonapi"})
        public void validate_rootPathIsAServletMapping_returnsNoErrors(String rootPath) {
            sut.setRootPath(rootPath);

            assertThat(sut.validate().hasErrors()).isFalse();
        }

    }

    @Nested
    class Validation {

        @Test
        public void validate_validationIsNotSet_reportsError() {
            sut.setValidation(null);

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.validation");
        }

        @Test
        public void validate_nonPositiveMaxNumberFilterParams_reportsError() {
            sut.getValidation().setMaxNumberFilterParams(0);

            assertThat(sut.validate().getPropertyErrors())
                    .containsOnlyKeys("jsonapi4j.validation.maxNumberFilterParams");
        }

        @Test
        public void validate_nonPositiveLimitMaxValue_reportsError() {
            sut.getValidation().setLimitMaxValue(0);

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.validation.limitMaxValue");
        }

        @Test
        public void validate_everyLimitIsNonPositive_reportsAllOfThem() {
            sut.getValidation().setMaxNumberFilterParams(0);
            sut.getValidation().setMaxElementsInFilterParam(-1);
            sut.getValidation().setResourceIdMaxLength(0);
            sut.getValidation().setLimitMaxValue(0);
            sut.getValidation().setMaxElementsInIncludeParam(0);
            sut.getValidation().setMaxElementsInSortByParam(0);

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys(
                    "jsonapi4j.validation.maxNumberFilterParams",
                    "jsonapi4j.validation.maxElementsInFilterParam",
                    "jsonapi4j.validation.resourceIdMaxLength",
                    "jsonapi4j.validation.limitMaxValue",
                    "jsonapi4j.validation.maxElementsInIncludeParam",
                    "jsonapi4j.validation.maxElementsInSortByParam"
            );
        }

    }

    @Nested
    class Meta {

        @Test
        public void validate_metaIsNotSet_reportsError() {
            sut.setMeta(null);

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.meta");
        }

    }

    @Nested
    class PropertyPaths {

        @Test
        public void propertyPath_rootProperty_isPrefixedWithConfigPrefixOnly() {
            assertThat(sut.propertyPath("rootPath")).isEqualTo("jsonapi4j.rootPath");
        }

        @Test
        public void propertyPathPrefix_rootProperties_returnsConfigPrefix() {
            assertThat(sut.propertyPathPrefix()).isEqualTo("jsonapi4j");
        }

    }

}
