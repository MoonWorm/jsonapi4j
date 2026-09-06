package pro.api4.jsonapi4j.plugin.sf.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SfPropertiesTests {

    private final DefaultSfProperties sut = new DefaultSfProperties();

    @Test
    public void validate_defaultProperties_returnsNoErrors() {
        assertThat(sut.validate().hasErrors()).isFalse();
    }

    @Test
    public void validate_requestedFieldsDontExistModeIsNotSet_reportsError() {
        sut.setRequestedFieldsDontExistMode(null);

        assertThat(sut.validate().getPropertyErrors())
                .containsOnlyKeys("jsonapi4j.sf.requestedFieldsDontExistMode");
    }

    @Test
    public void validate_pluginDisabled_returnsNoErrors() {
        sut.setEnabled(false);
        sut.setRequestedFieldsDontExistMode(null);

        assertThat(sut.validate().hasErrors()).isFalse();
    }

}
