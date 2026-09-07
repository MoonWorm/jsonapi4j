package pro.api4.jsonapi4j.servlet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class ServletMappingsTests {

    @ParameterizedTest
    @CsvSource({
            "/jsonapi,        /jsonapi/*",
            "/api/v1/jsonapi, /api/v1/jsonapi/*",
            "jsonapi,         /jsonapi/*",
            "'  /jsonapi  ',  /jsonapi/*"
    })
    public void toMapping_rootPath_appendsMatchAll(String rootPath, String expected) {
        assertThat(ServletMappings.toMapping(rootPath)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "/"})
    public void toMapping_applicationRoot_mapsEverything(String rootPath) {
        assertThat(ServletMappings.toMapping(rootPath)).isEqualTo("/*");
    }

    @Test
    public void toMapping_alreadyAMapping_isNotAppendedTwice() {
        assertThat(ServletMappings.toMapping("/jsonapi/*")).isEqualTo("/jsonapi/*");
    }

}
