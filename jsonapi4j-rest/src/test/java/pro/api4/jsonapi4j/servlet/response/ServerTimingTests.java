package pro.api4.jsonapi4j.servlet.response;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ServerTimingTests {

    private final HttpServletResponse response = mock(HttpServletResponse.class);

    @Nested
    class Enabled {

        private final ServerTiming sut = ServerTiming.of(true);

        @Test
        void add_startedEarlier_addsMetricWithElapsedMillisecondsToTenth() {
            ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);

            sut.add(response, "primary", System.nanoTime() - 12_440_000);

            verify(response).addHeader(eq("Server-Timing"), value.capture());
            assertThat(value.getValue()).matches("primary;dur=\\d+\\.\\d");
            assertThat(Double.parseDouble(value.getValue().substring("primary;dur=".length()))).isGreaterThanOrEqualTo(12.4);
        }

        @Test
        void addAll_severalDurations_addsOneMetricEachInKeyOrder() {
            sut.addAll(response, "includes-", Map.of(
                    "users", Duration.ofMillis(5),
                    "countries", Duration.ofNanos(3_050_000)
            ));

            InOrder inOrder = inOrder(response);
            inOrder.verify(response).addHeader("Server-Timing", "includes-countries;dur=3.1");
            inOrder.verify(response).addHeader("Server-Timing", "includes-users;dur=5.0");
        }

        @Test
        void addAll_nameWithNonTokenCharacters_replacesThem() {
            sut.addAll(response, "includes-", Map.of("user profiles/é", Duration.ofMillis(3)));

            verify(response).addHeader("Server-Timing", "includes-user_profiles__;dur=3.0");
        }

    }

    @Nested
    class Disabled {

        private final ServerTiming sut = ServerTiming.of(false);

        @Test
        void add_disabled_addsNothing() {
            sut.add(response, "primary", System.nanoTime());

            verifyNoInteractions(response);
        }

        @Test
        void addAll_disabled_addsNothing() {
            sut.addAll(response, "includes-", Map.of("countries", Duration.ofMillis(3)));

            verifyNoInteractions(response);
        }

    }

}
