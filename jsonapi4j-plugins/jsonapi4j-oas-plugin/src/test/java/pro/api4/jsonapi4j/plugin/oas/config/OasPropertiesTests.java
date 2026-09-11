package pro.api4.jsonapi4j.plugin.oas.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pro.api4.jsonapi4j.config.DefaultJsonApi4jProperties;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.config.MetaConfigComposer;
import pro.api4.jsonapi4j.config.PropertiesValidationResult;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties.*;
import pro.api4.jsonapi4j.plugin.PluginRegistry;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class OasPropertiesTests {

    private final DefaultOasProperties sut = validProperties();

    @Nested
    class Enablement {

        @Test
        public void validate_pluginDisabled_returnsNoErrors() {
            sut.setEnabled(false);
            sut.setOasRootPath("oas/");

            assertThat(sut.validate().hasErrors()).isFalse();
        }

        @Test
        public void validate_validProperties_returnsNoErrors() {
            assertThat(sut.validate().hasErrors()).isFalse();
        }

        @Test
        public void validate_defaultProperties_returnsNoErrors() {
            assertThat(new DefaultOasProperties().validate().hasErrors()).isFalse();
        }

    }

    @Nested
    class OasRootPath {

        @ParameterizedTest
        @ValueSource(strings = {"jsonapi/oas", "/jsonapi/oas/", " "})
        public void validate_oasRootPathIsNotAServletMapping_reportsError(String oasRootPath) {
            sut.setOasRootPath(oasRootPath);

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.oas.oasRootPath");
        }

    }

    @Nested
    class Info {

        @Test
        public void validate_blankTitle_reportsError() {
            sut.getInfo().setTitle(" ");

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.oas.info.title");
        }

        @Test
        public void validate_malformedContactEmail_reportsError() {
            sut.getInfo().getContact().setEmail("john.doe.foo.bar");

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.oas.info.contact.email");
        }

        @Test
        public void validate_relativeContactUrl_reportsError() {
            sut.getInfo().getContact().setUrl("/john");

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.oas.info.contact.url");
        }

        @Test
        public void validate_licenseUrlAndIdentifier_reportsCrossPropertiesError() {
            sut.getInfo().getLicense().setIdentifier("Apache-2.0");

            PropertiesValidationResult result = sut.validate();

            assertThat(result.getPropertyErrors()).isEmpty();
            assertThat(result.getCrossPropertiesErrors()).hasSize(1);
        }

        @Test
        public void validate_extensionWithoutXPrefix_reportsError() {
            sut.getInfo().setExtensions(Map.of("build", "42"));
            sut.getInfo().setTermsOfService("https://foo.bar/terms");

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.oas.info.extensions.build");
        }

        @Test
        public void validate_extensionWithXPrefix_returnsNoErrors() {
            sut.getInfo().setExtensions(Map.of("x-build", "42"));

            assertThat(sut.validate().hasErrors()).isFalse();
        }

    }

    @Nested
    class ExternalDocumentation {

        @Test
        public void validate_externalDocumentationWithoutUrl_reportsError() {
            sut.getExternalDocumentation().setUrl(null);

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.oas.externalDocumentation.url");
        }

    }

    @Nested
    class Servers {

        @Test
        public void validate_enabledServerWithoutUrl_reportsError() {
            sut.getServers().get(0).setUrl(null);

            assertThat(sut.validate().getPropertyErrors()).containsOnlyKeys("jsonapi4j.oas.servers[0].url");
        }

        @Test
        public void validate_disabledServerWithoutUrl_returnsNoErrors() {
            sut.getServers().get(0).setEnabled(false);
            sut.getServers().get(0).setUrl(null);

            assertThat(sut.validate().hasErrors()).isFalse();
        }

    }

    @Nested
    class OAuth2 {

        @Test
        public void validate_clientCredentialsWithoutTokenUrl_reportsError() {
            sut.getOauth2().getClientCredentials().setTokenUrl(null);

            assertThat(sut.validate().getPropertyErrors())
                    .containsOnlyKeys("jsonapi4j.oas.oauth2.clientCredentials.tokenUrl");
        }

        @Test
        public void validate_authorizationCodeWithoutAuthorizationUrl_reportsError() {
            sut.getOauth2().getAuthorizationCodeWithPkce().setAuthorizationUrl(null);

            assertThat(sut.validate().getPropertyErrors())
                    .containsOnlyKeys("jsonapi4j.oas.oauth2.authorizationCodeWithPkce.authorizationUrl");
        }

        @Test
        public void validate_clientCredentialsWithoutAuthorizationUrl_returnsNoErrors() {
            sut.getOauth2().getClientCredentials().setAuthorizationUrl(null);

            assertThat(sut.validate().hasErrors()).isFalse();
        }

        @Test
        public void validate_blankScopeName_reportsError() {
            sut.getOauth2().getAuthorizationCodeWithPkce().getScopes().get(0).setName(" ");

            assertThat(sut.validate().getPropertyErrors())
                    .containsOnlyKeys("jsonapi4j.oas.oauth2.authorizationCodeWithPkce.scopes[0].name");
        }

        @Test
        public void validate_bothGrantFlowsShareTheSameName_reportsCrossPropertiesError() {
            sut.getOauth2().getClientCredentials().setName("OAuth2");
            sut.getOauth2().getAuthorizationCodeWithPkce().setName("OAuth2");

            PropertiesValidationResult result = sut.validate();

            assertThat(result.getPropertyErrors()).isEmpty();
            assertThat(result.getCrossPropertiesErrors()).hasSize(1);
        }

    }

    @Nested
    class CustomResponseHeaders {

        @ParameterizedTest
        @ValueSource(strings = {"4XX", "429 Too Many Requests", "42"})
        public void validate_httpStatusCodeIsNotThreeDigits_reportsError(String httpStatusCode) {
            sut.getCustomResponseHeaders().get(0).setHttpStatusCode(httpStatusCode);

            assertThat(sut.validate().getPropertyErrors())
                    .containsOnlyKeys("jsonapi4j.oas.customResponseHeaders[0].httpStatusCode");
        }

        @Test
        public void validate_unsupportedHeaderSchema_reportsError() {
            sut.getCustomResponseHeaders().get(0).getHeaders().get(0).setSchema("number");

            assertThat(sut.validate().getPropertyErrors())
                    .containsOnlyKeys("jsonapi4j.oas.customResponseHeaders[0].headers[0].schema");
        }

        @Test
        public void validate_groupWithoutHeaders_reportsError() {
            sut.getCustomResponseHeaders().get(0).setHeaders(List.of());

            assertThat(sut.validate().getPropertyErrors())
                    .containsOnlyKeys("jsonapi4j.oas.customResponseHeaders[0].headers");
        }

        @Test
        public void validate_sameHttpStatusCodeDeclaredTwice_reportsCrossPropertiesError() {
            sut.setCustomResponseHeaders(List.of(rateLimitHeaders(), rateLimitHeaders()));

            PropertiesValidationResult result = sut.validate();

            assertThat(result.getPropertyErrors()).isEmpty();
            assertThat(result.getCrossPropertiesErrors()).hasSize(1);
        }

    }

    @Nested
    class CrossCheckAgainstRootPath {

        @Test
        public void validateAgainst_oasRootPathNestedUnderRootPath_returnsNoErrors() {
            assertThat(sut.validateAgainst(rootProperties("/jsonapi")).hasErrors()).isFalse();
        }

        @Test
        public void validateAgainst_oasRootPathOutsideRootPath_returnsNoErrors() {
            sut.setOasRootPath("/openapi");

            assertThat(sut.validateAgainst(rootProperties("/jsonapi")).hasErrors()).isFalse();
        }

        @Test
        public void validateAgainst_oasRootPathEqualToRootPath_reportsCrossPropertiesError() {
            sut.setOasRootPath("/jsonapi");

            PropertiesValidationResult result = sut.validateAgainst(rootProperties("/jsonapi"));

            assertThat(result.getPropertyErrors()).isEmpty();
            assertThat(result.getCrossPropertiesErrors()).hasSize(1);
        }

        @Test
        public void validateAgainst_pluginDisabled_returnsNoErrors() {
            sut.setEnabled(false);
            sut.setOasRootPath("/jsonapi");

            assertThat(sut.validateAgainst(rootProperties("/jsonapi")).hasErrors()).isFalse();
        }

        @Test
        public void validateAgainst_noRootProperties_returnsNoErrors() {
            sut.setOasRootPath("/jsonapi");

            assertThat(sut.validateAgainst(null).hasErrors()).isFalse();
        }

    }

    private static JsonApi4jProperties rootProperties(String rootPath) {
        DefaultJsonApi4jProperties properties = new DefaultJsonApi4jProperties();
        properties.setRootPath(rootPath);
        return properties;
    }

    private static DefaultOasProperties validProperties() {
        DefaultOasProperties properties = new DefaultOasProperties();
        properties.setEnabled(true);
        properties.setOasRootPath("/jsonapi/oas");

        DefaultContact contact = new DefaultContact();
        contact.setName("John Doe");
        contact.setUrl("http://users.foo.bar/john");
        contact.setEmail("john.doe@foo.bar");

        DefaultLicense license = new DefaultLicense();
        license.setName("Apache 2.0");
        license.setUrl("https://www.apache.org/licenses/LICENSE-2.0");

        DefaultInfo info = new DefaultInfo();
        info.setTitle("JSON:API Users & Countries");
        info.setVersion("1.0.0");
        info.setContact(contact);
        info.setLicense(license);
        properties.setInfo(info);

        DefaultExternalDocumentation externalDocumentation = new DefaultExternalDocumentation();
        externalDocumentation.setUrl("http://foo.bar.docs/article-1");
        properties.setExternalDocumentation(externalDocumentation);

        DefaultOAuth2Scope scope = new DefaultOAuth2Scope();
        scope.setName("users.read");

        DefaultOAuth2GrantFlow clientCredentials = new DefaultOAuth2GrantFlow();
        clientCredentials.setName("Client_Credentials");
        clientCredentials.setTokenUrl("http://foo.bar/tokenUrl");

        DefaultOAuth2GrantFlow authorizationCode = new DefaultOAuth2GrantFlow();
        authorizationCode.setName("Authorization_Code_With_PKCE");
        authorizationCode.setTokenUrl("http://foo.bar/tokenUrl");
        authorizationCode.setAuthorizationUrl("http://foo.bar/authorizationUrl");
        authorizationCode.setScopes(List.of(scope));

        DefaultOAuth2 oauth2 = new DefaultOAuth2();
        oauth2.setClientCredentials(clientCredentials);
        oauth2.setAuthorizationCodeWithPkce(authorizationCode);
        properties.setOauth2(oauth2);

        DefaultServer server = new DefaultServer();
        server.setName("Localhost");
        server.setEnabled(true);
        server.setUrl("http://localhost:8080");
        properties.setServers(List.of(server));

        properties.setCustomResponseHeaders(List.of(rateLimitHeaders()));
        return properties;
    }

    private static DefaultCustomResponseHeaderGroup rateLimitHeaders() {
        DefaultResponseHeader header = new DefaultResponseHeader();
        header.setName("X-RateLimit-Remaining");
        header.setRequired(true);
        header.setSchema("integer");
        header.setExample("5");

        DefaultCustomResponseHeaderGroup group = new DefaultCustomResponseHeaderGroup();
        group.setHttpStatusCode("429");
        group.setHeaders(List.of(header));
        return group;
    }

}
