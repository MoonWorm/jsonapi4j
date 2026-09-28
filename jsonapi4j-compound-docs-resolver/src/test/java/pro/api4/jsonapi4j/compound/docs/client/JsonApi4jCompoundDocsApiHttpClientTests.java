package pro.api4.jsonapi4j.compound.docs.client;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonApi4jCompoundDocsApiHttpClientTests {

    @Nested
    class IsPropagatable {

        @ParameterizedTest
        @ValueSource(strings = {"Forwarded", "X-Forwarded-For", "x-forwarded-for", "X-Real-IP"})
        public void isPropagatable_clientAddressHeader_returnsFalse(String header) {
            assertThat(JsonApi4jCompoundDocsApiHttpClient.isPropagatable(header)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"Host", "Connection", "Content-Length", "Expect", "Upgrade"})
        public void isPropagatable_restrictedHeader_returnsFalse(String header) {
            assertThat(JsonApi4jCompoundDocsApiHttpClient.isPropagatable(header)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Authorization",
                "Accept-Language",
                "traceparent",
                "X-Forwarded-Proto",
                "X-Forwarded-Host",
                "X-Forwarded-User",
                "X-Forwarded-Client-Cert"
        })
        public void isPropagatable_regularHeader_returnsTrue(String header) {
            assertThat(JsonApi4jCompoundDocsApiHttpClient.isPropagatable(header)).isTrue();
        }

    }

}
