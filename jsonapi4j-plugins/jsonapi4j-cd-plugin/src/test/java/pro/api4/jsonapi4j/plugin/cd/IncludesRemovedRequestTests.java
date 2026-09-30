package pro.api4.jsonapi4j.plugin.cd;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IncludesRemovedRequestTests {

    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Nested
    class Parameters {

        @Test
        void getParameterMap_someIncludesRemoved_keepsTheOthersAndOtherParameters() {
            when(request.getParameterMap()).thenReturn(Map.of(
                    "include", new String[]{"placeOfBirth,foo", "relatives"},
                    "fields[users]", new String[]{"fullName"}
            ));

            IncludesRemovedRequest sut = new IncludesRemovedRequest(request, Set.of("foo"));

            assertThat(sut.getParameterValues("include")).containsExactly("placeOfBirth,relatives");
            assertThat(sut.getParameter("fields[users]")).isEqualTo("fullName");
        }

        @Test
        void getParameterMap_everyIncludeRemoved_dropsTheParameter() {
            when(request.getParameterMap()).thenReturn(Map.of("include", new String[]{"foo"}));

            IncludesRemovedRequest sut = new IncludesRemovedRequest(request, Set.of("foo"));

            assertThat(sut.getParameterMap()).doesNotContainKey("include");
            assertThat(sut.getParameter("include")).isNull();
        }

    }

}
