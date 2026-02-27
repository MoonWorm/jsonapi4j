package pro.api4.jsonapi4j.request;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.compatibility.JsonApi4jCompatibilityMode;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonApiMediaTypeTests {

    @Test
    public void strictContentType_rejectsInvalidJsonApiParameters() {
        assertThat(JsonApiMediaType.isMatches(
                "application/vnd.api+json;charset=UTF-8",
                JsonApi4jCompatibilityMode.STRICT
        )).isFalse();
    }

    @Test
    public void strictContentType_acceptsAllowedJsonApiParameters() {
        assertThat(JsonApiMediaType.isMatches(
                "application/vnd.api+json;profile=\"https://example.com/p\"",
                JsonApi4jCompatibilityMode.STRICT
        )).isTrue();
    }

    @Test
    public void strictAccept_rejectsInvalidJsonApiParameters() {
        assertThat(JsonApiMediaType.isAccepted(
                "application/vnd.api+json;charset=UTF-8",
                JsonApi4jCompatibilityMode.STRICT
        )).isFalse();
    }

    @Test
    public void strictAccept_acceptsAllowedJsonApiParameters() {
        assertThat(JsonApiMediaType.isAccepted(
                "application/vnd.api+json;profile=\"https://example.com/p\"",
                JsonApi4jCompatibilityMode.STRICT
        )).isTrue();
    }

    @Test
    public void strictContentType_rejectsUnquotedProfileParameterValue() {
        assertThat(JsonApiMediaType.isMatches(
                "application/vnd.api+json;profile=https://example.com/p",
                JsonApi4jCompatibilityMode.STRICT
        )).isFalse();
    }

    @Test
    public void strictAccept_rejectsNonUriExtParameterValue() {
        assertThat(JsonApiMediaType.isAccepted(
                "application/vnd.api+json;ext=\"foo\"",
                JsonApi4jCompatibilityMode.STRICT
        )).isFalse();
    }

    @Test
    public void strictContentType_rejectsUnsupportedExtensionUri() {
        assertThat(JsonApiMediaType.isMatches(
                "application/vnd.api+json;ext=\"https://example.com/ext/foo\"",
                JsonApi4jCompatibilityMode.STRICT,
                Set.of("https://example.com/ext/bar"),
                Set.of()
        )).isFalse();
    }

    @Test
    public void strictContentType_acceptsSupportedExtensionUri() {
        assertThat(JsonApiMediaType.isMatches(
                "application/vnd.api+json;ext=\"https://example.com/ext/foo\"",
                JsonApi4jCompatibilityMode.STRICT,
                Set.of("https://example.com/ext/foo"),
                Set.of()
        )).isTrue();
    }

    @Test
    public void strictAccept_acceptsWhenAtLeastOneJsonApiInstanceHasSupportedExtensions() {
        assertThat(JsonApiMediaType.isAccepted(
                "application/vnd.api+json;ext=\"https://example.com/ext/unsupported\", application/vnd.api+json;ext=\"https://example.com/ext/supported\"",
                JsonApi4jCompatibilityMode.STRICT,
                Set.of("https://example.com/ext/supported"),
                Set.of()
        )).isTrue();
    }

    @Test
    public void strictAccept_rejectsWhenOnlyUnsupportedExtensionsAreRequested() {
        assertThat(JsonApiMediaType.isAccepted(
                "application/vnd.api+json;ext=\"https://example.com/ext/unsupported\"",
                JsonApi4jCompatibilityMode.STRICT,
                Set.of("https://example.com/ext/supported"),
                Set.of()
        )).isFalse();
    }

    @Test
    public void strictAccept_ignoresUnsupportedProfiles() {
        assertThat(JsonApiMediaType.isAccepted(
                "application/vnd.api+json;profile=\"https://example.com/profile/unknown\"",
                JsonApi4jCompatibilityMode.STRICT,
                Set.of(),
                Set.of("https://example.com/profile/known")
        )).isTrue();
    }

    @Test
    public void strictAccept_acceptsWildcardMediaType() {
        assertThat(JsonApiMediaType.isAccepted("*/*", JsonApi4jCompatibilityMode.STRICT)).isTrue();
        assertThat(JsonApiMediaType.isAccepted("application/*", JsonApi4jCompatibilityMode.STRICT)).isTrue();
    }

    @Test
    public void legacyMode_keepsTolerantBehavior() {
        assertThat(JsonApiMediaType.isMatches(
                "application/vnd.api+json;charset=UTF-8",
                JsonApi4jCompatibilityMode.LEGACY
        )).isTrue();
        assertThat(JsonApiMediaType.isAccepted(
                "application/vnd.api+json;charset=UTF-8",
                JsonApi4jCompatibilityMode.LEGACY
        )).isTrue();
    }
}
