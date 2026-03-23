package pro.api4.jsonapi4j.plugin.oas.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import pro.api4.jsonapi4j.config.JsonApi4jConfigReader;
import pro.api4.jsonapi4j.principal.tier.AccessTier;
import pro.api4.jsonapi4j.principal.tier.TierPublic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@Getter
@Setter
public class DefaultOasProperties implements OasProperties {

    private boolean enabled = Boolean.parseBoolean(DEFAULT_OAS_ENABLED);
    private String oasRootPath = DEFAULT_OAS_ROOT_PATH;
    private DefaultInfo info;
    private DefaultExternalDocumentation externalDocumentation;
    private DefaultOAuth2 oauth2;
    private List<DefaultServer> servers = new ArrayList<>();
    private List<DefaultCustomResponseHeaderGroup> customResponseHeaders = new ArrayList<>();

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public String oasRootPath() {
        return oasRootPath;
    }

    @Override
    public Info info() {
        return info;
    }

    @Override
    public ExternalDocumentation externalDocumentation() {
        return externalDocumentation;
    }

    @Override
    public OAuth2 oauth2() {
        return oauth2;
    }

    @Override
    public List<? extends Server> servers() {
        return servers;
    }

    @Override
    public List<? extends CustomResponseHeaderGroup> customResponseHeaders() {
        return customResponseHeaders;
    }

    @Getter
    @Setter
    public static class DefaultInfo implements Info {
        private String title = OAS_INFO_TITLE_DEFAULT_VALUE;
        private String description;
        private DefaultContact contact;
        private String version = OAS_INFO_VERSION_DEFAULT_VALUE;
        private String termsOfService;
        private DefaultLicense license;
        private Map<String, Object> extensions;

        @Override
        public String title() {
            return title;
        }

        @Override
        public String description() {
            return description;
        }

        @Override
        public Contact contact() {
            return contact;
        }

        @Override
        public String version() {
            return version;
        }

        @Override
        public String termsOfService() {
            return termsOfService;
        }

        @Override
        public License license() {
            return license;
        }

        @Override
        public Map<String, Object> extensions() {
            return extensions;
        }

    }

    @Getter
    @Setter
    public static class DefaultContact implements Contact {
        private String name;
        private String url;
        private String email;

        @Override
        public String name() {
            return name;
        }

        @Override
        public String url() {
            return url;
        }

        @Override
        public String email() {
            return email;
        }

    }

    @Getter
    @Setter
    public static class DefaultLicense implements License {
        private String name;
        private String url;
        private String identifier;

        @Override
        public String name() {
            return name;
        }

        @Override
        public String url() {
            return url;
        }

        @Override
        public String identifier() {
            return identifier;
        }

    }

    @Getter
    @Setter
    public static class DefaultExternalDocumentation implements ExternalDocumentation {
        private String url;
        private String description;

        @Override
        public String url() {
            return url;
        }

        @Override
        public String description() {
            return description;
        }

    }

    @Getter
    @Setter
    public static class DefaultOAuth2 implements OAuth2 {

        private DefaultOAuth2GrantFlow clientCredentials;
        private DefaultOAuth2GrantFlow authorizationCodeWithPkce;

        @Override
        public OAuth2GrantFlow clientCredentials() {
            return clientCredentials;
        }

        @Override
        public OAuth2GrantFlow authorizationCodeWithPkce() {
            return authorizationCodeWithPkce;
        }

    }

    @Getter
    @Setter
    public static class DefaultOAuth2GrantFlow implements OAuth2GrantFlow {
        private String name;
        private String description;
        private String tokenUrl;
        // only required for Authorization Code grant
        private String authorizationUrl;
        private List<DefaultOAuth2Scope> scopes = new ArrayList<>();

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return description;
        }

        @Override
        public String tokenUrl() {
            return tokenUrl;
        }

        @Override
        public String authorizationUrl() {
            return authorizationUrl;
        }

        @Override
        public List< ? extends OAuth2Scope> scopes() {
            return scopes;
        }

    }

    @Getter
    @Setter
    public static class DefaultOAuth2Scope implements OAuth2Scope {
        private String name;
        private String description;
        private AccessTier requiredAccessTier = new TierPublic();

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return description;
        }

        @Override
        public AccessTier requiredAccessTier() {
            return requiredAccessTier;
        }

    }

    @Getter
    @Setter
    public static class DefaultServer implements Server {
        private String name;
        private String url;
        private boolean enabled = Boolean.parseBoolean(DEFAULT_OAS_SERVER_ENABLED);

        @Override
        public String name() {
            return name;
        }

        @Override
        public String url() {
            return url;
        }

        @Override
        public boolean enabled() {
            return enabled;
        }

    }

    @ToString
    @Getter
    @Setter
    public static class DefaultCustomResponseHeaderGroup implements CustomResponseHeaderGroup {

        private String httpStatusCode;

        private List<DefaultResponseHeader> headers = new ArrayList<>();

        @Override
        public String httpStatusCode() {
            return httpStatusCode;
        }

        @Override
        public List<? extends ResponseHeader> headers() {
            return headers;
        }

    }

    @ToString
    @Getter
    @Setter
    public static class DefaultResponseHeader implements ResponseHeader {
        private String name;
        private String description;
        private boolean required = Boolean.parseBoolean(DEFAULT_OAS_RESPONSE_HEADER_REQUIRED);
        private String schema = DEFAULT_OAS_RESPONSE_HEADER_SCHEMA;
        private String example;

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return description;
        }

        @Override
        public boolean required() {
            return required;
        }

        @Override
        public String schema() {
            return schema;
        }

        @Override
        public String example() {
            return example;
        }

    }

    public static OasProperties toOasProperties(Map<String, Object> jsonApi4jPropertiesRaw) {
        Object oasPropertiesObject = jsonApi4jPropertiesRaw.get(OasProperties.OAS_PROPERTY_NAME);
        Map<String, Object> oasPropertiesRaw = Collections.emptyMap();
        if (oasPropertiesObject instanceof Map oasPropertiesMap) {
            //noinspection unchecked
            oasPropertiesRaw = oasPropertiesMap;
        }
        OasProperties oasProperties = new DefaultOasProperties();
        if (!oasPropertiesRaw.isEmpty()) {
            oasProperties = JsonApi4jConfigReader.convertToConfig(
                    oasPropertiesRaw,
                    DefaultOasProperties.class
            );
        }
        return oasProperties;
    }

}
