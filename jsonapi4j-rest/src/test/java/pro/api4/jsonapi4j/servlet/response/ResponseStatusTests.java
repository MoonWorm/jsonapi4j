package pro.api4.jsonapi4j.servlet.response;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.http.HttpStatusCodes;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseStatusTests {

    @Nested
    class Clear {

        @Test
        void clear_overriddenStatus_discardsIt() {
            ResponseStatus.overrideResponseStatus(HttpStatusCodes.SC_400_BAD_REQUEST);

            ResponseStatus.clear();

            assertThat(ResponseStatus.getOverriddenStatus()).isEmpty();
        }

    }

}
