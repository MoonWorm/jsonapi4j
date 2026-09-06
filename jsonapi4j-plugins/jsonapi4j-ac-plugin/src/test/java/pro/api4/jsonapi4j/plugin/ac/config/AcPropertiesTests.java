package pro.api4.jsonapi4j.plugin.ac.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AcPropertiesTests {

    private final DefaultAcProperties sut = new DefaultAcProperties();

    @Test
    public void validate_defaultProperties_returnsNoErrors() {
        assertThat(sut.validate().hasErrors()).isFalse();
    }

    @Test
    public void validate_anonymizationReportIsNotSet_reportsError() {
        sut.setAnonymizationReport(null);

        assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.ac.anonymizationReport");
    }

    @Test
    public void validate_pluginDisabled_returnsNoErrors() {
        sut.setEnabled(false);
        sut.setAnonymizationReport(null);

        assertThat(sut.validate().hasErrors()).isFalse();
    }

}
