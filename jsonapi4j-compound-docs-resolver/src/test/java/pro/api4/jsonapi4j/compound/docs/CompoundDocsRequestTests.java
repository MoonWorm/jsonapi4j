package pro.api4.jsonapi4j.compound.docs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CompoundDocsRequestTests {

    @Nested
    class Headers {

        @Test
        void getHeaders_nameInAnyCase_findsAllValues() {
            CompoundDocsRequest sut = request(Map.of("Accept-Language", List.of("de", "en")));

            assertThat(sut.getHeaders().get("accept-language")).containsExactly("de", "en");
        }

        @Test
        void isProcessable_disablingHeaderInAnyCase_isFalse() {
            CompoundDocsRequest sut = request(Map.of("X-Disable-Compound-Docs", List.of("TRUE")));

            assertThat(sut.isProcessable()).isFalse();
        }

        @Test
        void isProcessable_disablingHeaderNotTrue_isTrue() {
            CompoundDocsRequest sut = request(Map.of("x-disable-compound-docs", List.of("false")));

            assertThat(sut.isProcessable()).isTrue();
        }

    }

    private static CompoundDocsRequest request(Map<String, List<String>> headers) {
        return new CompoundDocsRequest("GET", List.of("relatives"), Map.of(), headers, "/users/1", Map.of());
    }

}
