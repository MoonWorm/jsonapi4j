package pro.api4.jsonapi4j.plugin.oas.config;

import lombok.Getter;
import lombok.Setter;
import pro.api4.jsonapi4j.principal.tier.AccessTier;
import pro.api4.jsonapi4j.principal.tier.TierPublic;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class DefaultOasProperties implements OasProperties {

    private String oasRootPath = DEFAULT_OAS_ROOT_PATH;
    private DefaultInfo info;
    private DefaultExternalDocumentation externalDocumentation;
    private DefaultOAuth2 oauth2;
    private Map<String, DefaultServer> servers;
    private Map<String, Map<String, DefaultResponseHeader>> customResponseHeaders;

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
    public Map<String, ? extends Server> servers() {
        return servers;
    }

    @Override
    public Map<String, Map<String, DefaultResponseHeader>> customResponseHeaders() {
        return customResponseHeaders;
    }

    @Getter
    @Setter
    public static class DefaultInfo implements Info {
        private String title;
        private String description;
        private DefaultContact contact;
        private String version;
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
        private Map<String, DefaultOAuth2Scope> scopes;

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
        public Map<String, ? extends OAuth2Scope> scopes() {
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
        private boolean enabled;

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

    @Getter
    @Setter
    public static class DefaultResponseHeader implements ResponseHeader {
        private String description;
        private boolean required;
        private String schema;
        private String example;

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

}
