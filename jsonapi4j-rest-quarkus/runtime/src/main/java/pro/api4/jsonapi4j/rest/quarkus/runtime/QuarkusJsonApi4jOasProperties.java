package pro.api4.jsonapi4j.rest.quarkus.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import jakarta.enterprise.context.ApplicationScoped;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties.*;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;

import java.util.List;
import java.util.Optional;

import static io.smallrye.config.ConfigMapping.NamingStrategy.VERBATIM;
import static pro.api4.jsonapi4j.plugin.oas.config.OasProperties.Info.OAS_INFO_TITLE_DEFAULT_VALUE;
import static pro.api4.jsonapi4j.plugin.oas.config.OasProperties.Info.OAS_INFO_VERSION_DEFAULT_VALUE;
import static pro.api4.jsonapi4j.plugin.oas.config.OasProperties.ResponseHeader.DEFAULT_OAS_RESPONSE_HEADER_REQUIRED;
import static pro.api4.jsonapi4j.plugin.oas.config.OasProperties.ResponseHeader.DEFAULT_OAS_RESPONSE_HEADER_SCHEMA;
import static pro.api4.jsonapi4j.plugin.oas.config.OasProperties.Server.DEFAULT_OAS_SERVER_ENABLED;

@ApplicationScoped
@ConfigMapping(prefix = "jsonapi4j.oas", namingStrategy = VERBATIM)
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface QuarkusJsonApi4jOasProperties {

    /**
     * Enable/disable the JsonApi4j plugin. If enabled - OAS Spec endpoint will be exposed on 'oasRootPath'.
     * Enabled by default.
     * Example: `true`, `false`.
     */
    @WithDefault(OasProperties.DEFAULT_OAS_ENABLED)
    boolean enabled();

    /**
     * Root path where OAS servlet is mounted. Required.
     * Uses '/jsonapi/oas' by default.
     * Example: `/jsonapi/oas`, `/oas`.
     */
    @WithDefault(OasProperties.DEFAULT_OAS_ROOT_PATH)
    String oasRootPath();

    /**
     * OAS Info Section configurations. Optional.
     */
    Optional<QuarkusJsonApi4jOasInfoProperties> info();

    /**
     * Additional external documentation. Optional.
     */
    Optional<QuarkusJsonApi4jOasExternalDocsProperties> externalDocumentation();

    /**
     * OAuth 2 Security Scheme settings. Optional.
     */
    Optional<QuarkusJsonApi4jOasOAuth2Properties> oauth2();

    /**
     * An array of Server Objects, which provide connectivity information to a target server. Optional.
     */
    List<QuarkusJsonApi4jOasServersProperties> servers();

    /**
     * An array of custom response headers details per HTTP Status Code. Optional.
     */
    List<QuarkusJsonApi4jOasCustomResponseHeaderGroupProperties> customResponseHeaders();

    interface QuarkusJsonApi4jOasInfoProperties {

        /**
         * The title of the API. Required.
         * Example: `My Countries API`.
         */
        @WithDefault(OAS_INFO_TITLE_DEFAULT_VALUE)
        String title();

        /**
         * A description of the API. CommonMark syntax MAY be used for rich text representation.
         * Example: `My Countries API that can solve multiple problems ...`.
         */
        Optional<String> description();

        /**
         * The contact information for the exposed API. Optional.
         */
        Optional<QuarkusJsonApi4jOasInfoContactProperties> contact();

        /**
         * The version of the OpenAPI Document (which is distinct from the OpenAPI Specification version or the version of the API being described or the version of the OpenAPI Description).
         * Required.
         * Example: `1.0.0`.
         */
        @WithDefault(OAS_INFO_VERSION_DEFAULT_VALUE)
        String version();

        /**
         * A URI for the Terms of Service for the API. This MUST be in the form of a URI. Optional.
         */
        Optional<String> termsOfService();

        /**
         * The license information for the exposed API. Optional.
         */
        Optional<QuarkusJsonApi4jOasInfoLicenseProperties> license();

    }

    interface QuarkusJsonApi4jOasInfoContactProperties {
        /**
         * The identifying name of the contact person/organization. Optional.
         */
        Optional<String> name();

        /**
         * The URI for the contact information. This MUST be in the form of a URI. Optional.
         */
        Optional<String> url();

        /**
         * The email address of the contact person/organization. This MUST be in the form of an email address. Optional.
         */
        Optional<String> email();
    }

    interface QuarkusJsonApi4jOasInfoLicenseProperties {
        /**
         * The license name used for the API. Required.
         */
        String name();

        /**
         * A URI for the license used for the API. This MUST be in the form of a URI. The url field is mutually exclusive of the identifier field. Optional.
         */
        Optional<String> url();

        /**
         * An SPDX license expression for the API. The identifier field is mutually exclusive of the url field. Optional.
         */
        Optional<String> identifier();
    }

    interface QuarkusJsonApi4jOasExternalDocsProperties {

        /**
         * The URI for the target documentation. This MUST be in the form of a URI. Required.
         */
        String url();

        /**
         * A description of the target documentation. CommonMark syntax MAY be used for rich text representation. Optional.
         */
        Optional<String> description();

    }

    interface QuarkusJsonApi4jOasOAuth2Properties {

        /**
         * Client Credentials OAuth2 Grant Flow settings. Optional.
         */
        Optional<QuarkusJsonApi4jOasOAuth2ClientCredentialsProperties> clientCredentials();

        /**
         * Authorization Code (with PKCE) OAuth2 Grant Flow settings. Optional.
         */
        Optional<QuarkusJsonApi4jOasOAuth2AuthorizationCodeProperties> authorizationCodeWithPkce();

    }

    interface QuarkusJsonApi4jOasOAuth2ClientCredentialsProperties {

        /**
         * Security Scheme name. Required.
         */
        String name();

        /**
         * Security Scheme description. Optional.
         */
        Optional<String> description();

        /**
         * The token URL to be used for this flow. This MUST be in the form of a URL. The OAuth2 standard requires the use of TLS. Required.
         */
        String tokenUrl();

    }

    interface QuarkusJsonApi4jOasOAuth2AuthorizationCodeProperties {

        /**
         * Security Scheme name. Required.
         */
        String name();

        /**
         * Security Scheme description. Optional.
         */
        Optional<String> description();

        /**
         * The token URL to be used for this flow. This MUST be in the form of a URL. The OAuth2 standard requires the use of TLS. Required.
         */
        String tokenUrl();

        /**
         * The authorization URL to be used for this flow. This MUST be in the form of a URL. The OAuth2 standard requires the use of TLS. Only applicable for 'authorizationCodeWithPkce' flow. Otherwise - optional.
         */
        Optional<String> authorizationUrl();

        /**
         * The available scopes for the OAuth2 security scheme. A map between the scope name and a short description for it. The map MAY be empty.
         */
        List<QuarkusJsonApi4jOasOAuth2Scope> scopes();

    }

    interface QuarkusJsonApi4jOasOAuth2Scope {

        /**
         * OAuth 2 scope name. Required.
         */
        String name();

        /**
         * OAuth 2 scope description. Optional.
         */
        Optional<String> description();

    }

    interface QuarkusJsonApi4jOasServersProperties {

        /**
         * An optional string describing the host designated by the URL. CommonMark syntax MAY be used for rich text representation.
         */
        String name();

        /**
         * A URL to the target host. This URL supports Server Variables and MAY be relative, to indicate that the host location is relative to the location where the document containing the Server Object is being served. Variable substitutions will be made when a variable is named in {braces}. Required.
         */
        String url();

        /**
         * Whether this server is enabled or disabled. If disabled - the server will not be added into the resulting spec. Optional. Disable by default.
         */
        @WithDefault(DEFAULT_OAS_SERVER_ENABLED)
        boolean enabled();

    }

    interface QuarkusJsonApi4jOasCustomResponseHeaderGroupProperties {

        /**
         * HTTP Status which custom response headers are associated with. Required.
         */
        String httpStatusCode();

        /**
         * An array of custom response header details. Required.
         */
        List<QuarkusJsonApi4jOasCustomResponseHeaderProperties> headers();

    }

    interface QuarkusJsonApi4jOasCustomResponseHeaderProperties {
        /**
         * Custom Response Header name. Required.
         */
        String name();

        /**
         * Custom Response Header description. Optional.
         */
        Optional<String> description();

        /**
         * If header required or not. Optional. False by default.
         */
        @WithDefault(DEFAULT_OAS_RESPONSE_HEADER_REQUIRED)
        boolean required();

        /**
         * The schema defining the type used for the header. Required. 'string' by default.
         */
        @WithDefault(DEFAULT_OAS_RESPONSE_HEADER_SCHEMA)
        String schema();

        /**
         * Header example. Optional.
         */
        Optional<String> example();

    }

    default OasProperties toOasProperties() {
        DefaultOasProperties oasProperties = new DefaultOasProperties();
        oasProperties.setEnabled(enabled());
        oasProperties.setOasRootPath(oasRootPath());
        oasProperties.setInfo(info().map(qi -> {
            DefaultInfo di = new DefaultInfo();
            di.setTitle(qi.title());
            di.setDescription(qi.description().orElse(null));
            di.setContact(qi.contact().map(qic -> {
                DefaultContact c = new DefaultContact();
                c.setName(qic.name().orElse(null));
                c.setUrl(qic.url().orElse(null));
                c.setEmail(qic.email().orElse(null));
                return c;
            }).orElse(null));
            di.setTermsOfService(qi.termsOfService().orElse(null));
            di.setLicense(qi.license().map(qil -> {
                DefaultLicense l = new DefaultLicense();
                l.setName(qil.name());
                l.setUrl(qil.url().orElse(null));
                l.setIdentifier(qil.identifier().orElse(null));
                return l;
            }).orElse(null));
            di.setVersion(qi.version());
            return di;
        }).orElse(null));

        oasProperties.setExternalDocumentation(externalDocumentation().map(qed -> {
            DefaultExternalDocumentation ed = new DefaultExternalDocumentation();
            ed.setUrl(qed.url());
            ed.setDescription(qed.description().orElse(null));
            return ed;
        }).orElse(null));

        oasProperties.setOauth2(oauth2().map(qo -> {
            DefaultOAuth2 o = new DefaultOAuth2();
            o.setClientCredentials(qo.clientCredentials().map(cc -> {
                DefaultOAuth2GrantFlow of = new DefaultOAuth2GrantFlow();
                of.setName(cc.name());
                of.setDescription(cc.description().orElse(null));
                of.setTokenUrl(cc.tokenUrl());
                return of;
            }).orElse(null));
            o.setAuthorizationCodeWithPkce(qo.authorizationCodeWithPkce().map(ac -> {
                DefaultOAuth2GrantFlow of = new DefaultOAuth2GrantFlow();
                of.setName(ac.name());
                of.setDescription(ac.description().orElse(null));
                of.setTokenUrl(ac.tokenUrl());
                of.setAuthorizationUrl(ac.authorizationUrl().orElse(null));
                of.setScopes(ac.scopes().stream().map(qs -> {
                    DefaultOAuth2Scope s = new DefaultOAuth2Scope();
                    s.setName(qs.name());
                    s.setDescription(qs.description().orElse(null));
                    return s;
                }).toList());
                return of;
            }).orElse(null));
            return o;
        }).orElse(null));

        oasProperties.setServers(servers().stream().map(qs -> {
            DefaultServer s = new DefaultServer();
            s.setEnabled(qs.enabled());
            s.setName(qs.name());
            s.setUrl(qs.url());
            return s;
        }).toList());

        oasProperties.setCustomResponseHeaders(customResponseHeaders().stream().map(qcrhg -> {
            DefaultCustomResponseHeaderGroup crhg = new DefaultCustomResponseHeaderGroup();
            crhg.setHttpStatusCode(qcrhg.httpStatusCode());
            crhg.setHeaders(qcrhg.headers().stream().map(h -> {
                DefaultResponseHeader rh = new DefaultResponseHeader();
                rh.setName(h.name());
                rh.setDescription(h.description().orElse(null));
                rh.setSchema(h.schema());
                rh.setExample(h.example().orElse(null));
                rh.setRequired(h.required());
                return rh;
            }).toList());
            return crhg;
        }).toList());

        return oasProperties;
    }

}
