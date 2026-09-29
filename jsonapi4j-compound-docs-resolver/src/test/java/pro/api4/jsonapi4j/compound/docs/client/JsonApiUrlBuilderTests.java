package pro.api4.jsonapi4j.compound.docs.client;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class JsonApiUrlBuilderTests {

    private static final URI BASE_URL = URI.create("http://localhost:8080/jsonapi");

    @Nested
    class PlainValues {

        @Test
        public void build_plainValues_keepsReadableUrl() {
            String url = JsonApiUrlBuilder.from(BASE_URL)
                    .resourceType("users")
                    .filterParam("id", List.of("1", "2"))
                    .includeParam(List.of("relatives", "placeOfBirth"))
                    .fieldsParam("users", List.of("fullName", "email"))
                    .build();

            assertThat(url).isEqualTo(
                    "http://localhost:8080/jsonapi/users?filter[id]=1,2&include=relatives,placeOfBirth&fields[users]=fullName,email"
            );
        }

    }

    @Nested
    class Encoding {

        @Test
        public void build_customParamWithSpaceAndNonAscii_percentEncodesIt() {
            String url = JsonApiUrlBuilder.from(BASE_URL).resourceType("users").queryParam("q", List.of("a b é")).build();

            assertThat(url).isEqualTo("http://localhost:8080/jsonapi/users?q=a%20b%20%C3%A9");
        }

        @Test
        public void build_customParamWithAmpersand_cannotInjectAnotherParam() {
            String url = JsonApiUrlBuilder.from(BASE_URL)
                    .resourceType("users")
                    .queryParam("q", List.of("a&filter[id]=9"))
                    .build();

            assertThat(url).isEqualTo("http://localhost:8080/jsonapi/users?q=a%26filter%5Bid%5D%3D9");
        }

        @Test
        public void build_customParamNameWithReservedCharacters_percentEncodesIt() {
            String url = JsonApiUrlBuilder.from(BASE_URL).resourceType("users").queryParam("a=b&c", List.of("d")).build();

            assertThat(url).isEqualTo("http://localhost:8080/jsonapi/users?a%3Db%26c=d");
        }

        @Test
        public void build_idsContainingComma_encodesThemButKeepsSeparators() {
            String url = JsonApiUrlBuilder.from(BASE_URL).resourceType("users").filterParam("id", List.of("a,b", "c")).build();

            assertThat(url).isEqualTo("http://localhost:8080/jsonapi/users?filter[id]=a%2Cb,c");
        }

        @Test
        public void build_fieldsTypeWithReservedCharacters_percentEncodesIt() {
            String url = JsonApiUrlBuilder.from(BASE_URL).resourceType("users").fieldsParam("x&y", List.of("a")).build();

            assertThat(url).isEqualTo("http://localhost:8080/jsonapi/users?fields[x%26y]=a");
        }

        @Test
        public void build_resourceTypeWithSpace_percentEncodesPathSegment() {
            String url = JsonApiUrlBuilder.from(BASE_URL).resourceType("my users").build();

            assertThat(url).isEqualTo("http://localhost:8080/jsonapi/my%20users");
        }

        @Test
        public void build_hostileValuesEverywhere_producesParsableUri() {
            String url = JsonApiUrlBuilder.from(BASE_URL)
                    .resourceType("us ers")
                    .filterParam("id", List.of("1 2", "é"))
                    .includeParam(List.of("rel ation"))
                    .fieldsParams(Map.of("ty pe", List.of("f ield")))
                    .queryParams(Map.of("k ey", List.of("v alue#frag?x")))
                    .build();

            assertThatCode(() -> URI.create(url)).doesNotThrowAnyException();
        }

    }

    @Nested
    class MultiValuedParams {

        @Test
        public void build_multiValuedCustomParam_repeatsIt() {
            String url = JsonApiUrlBuilder.from(BASE_URL).resourceType("users").queryParam("tag", List.of("a", "b")).build();

            assertThat(url).isEqualTo("http://localhost:8080/jsonapi/users?tag=a&tag=b");
        }

        @Test
        public void build_customParamWithoutValues_omitsIt() {
            String url = JsonApiUrlBuilder.from(BASE_URL).resourceType("users").queryParam("tag", List.of()).build();

            assertThat(url).isEqualTo("http://localhost:8080/jsonapi/users");
        }

    }

}
