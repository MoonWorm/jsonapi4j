package pro.api4.jsonapi4j.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultJsonApi4jPropertiesTests {

    private final DefaultJsonApi4jProperties sut = new DefaultJsonApi4jProperties();

    @Test
    public void rootPath_notSet_returnsDefaultRootPath() {
        assertThat(sut.rootPath()).isEqualTo(JsonApi4jProperties.DEFAULT_ROOT_PATH);
    }

    @Test
    public void validation_notSet_returnsDefaultLimits() {
        assertThat(sut.validation()).isNotNull();
        assertThat(sut.validation().limitMaxValue()).isEqualTo(100L);
    }

    @Test
    public void meta_notSet_returnsDisabledMeta() {
        assertThat(sut.meta()).isNotNull();
        assertThat(sut.meta().enabled()).isFalse();
    }

}
