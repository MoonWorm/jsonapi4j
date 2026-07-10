package pro.api4.jsonapi4j.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseUrlsTests {

    @Test
    void stripTrailingSlash_removesSingleTrailingSlashAndWhitespace() {
        assertThat(BaseUrls.stripTrailingSlash("http://localhost:8080/")).isEqualTo("http://localhost:8080");
        assertThat(BaseUrls.stripTrailingSlash("  http://localhost:8080  ")).isEqualTo("http://localhost:8080");
        assertThat(BaseUrls.stripTrailingSlash("http://localhost:8080")).isEqualTo("http://localhost:8080");
        assertThat(BaseUrls.stripTrailingSlash(null)).isNull();
    }

    @Test
    void join_singleSeparatorBetweenBaseAndPath() {
        assertThat(BaseUrls.join("http://localhost:8080", "/jsonapi")).isEqualTo("http://localhost:8080/jsonapi");
        assertThat(BaseUrls.join("http://localhost:8080/", "jsonapi")).isEqualTo("http://localhost:8080/jsonapi");
        assertThat(BaseUrls.join("http://localhost:8080", "jsonapi")).isEqualTo("http://localhost:8080/jsonapi");
    }

    @Test
    void join_blankPath_returnsBareBase() {
        assertThat(BaseUrls.join("http://localhost:8080/", null)).isEqualTo("http://localhost:8080");
        assertThat(BaseUrls.join("http://localhost:8080", "  ")).isEqualTo("http://localhost:8080");
    }

    @Test
    void join_blankBase_returnsNormalizedPath() {
        assertThat(BaseUrls.join(null, "jsonapi")).isEqualTo("/jsonapi");
        assertThat(BaseUrls.join(null, "/jsonapi")).isEqualTo("/jsonapi");
    }
}
