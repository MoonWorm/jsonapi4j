package pro.api4.jsonapi4j.plugin.oas.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.principal.tier.AccessTier;
import pro.api4.jsonapi4j.principal.tier.TierPublic;
import lombok.Data;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class OasProperties {

    public static final String OAS_PROPERTY_NAME = "oas";

    public static final String DEFAULT_OAS_ROOT_PATH = JsonApi4jProperties.JSONAPI4J_DEFAULT_ROOT_PATH + "/oas";

    private String oasRootPath = DEFAULT_OAS_ROOT_PATH;
    private Info info;
    private ExternalDocumentation externalDocumentation;
    private OAuth2 oauth2;
    private Map<String, Server> servers;
    private Map<String, Map<String, ResponseHeader>> customResponseHeaders;

    @Data
    public static class Info {
        private String title;
        private String description;
        private Contact contact;
        private String version;
        private String termsOfService;
        private License license;
        private Map<String, Object> extensions;
    }

    @Data
    public static class Contact {
        private String name;
        private String url;
        private String email;
    }

    @Data
    public static class License {
        private String name;
        private String url;
        private String identifier ;
    }

    @Data
    public static class ExternalDocumentation {
        private String url;
        private String description;
    }

    @Data
    public static class OAuth2 {
        private OAuth2GrantFlow clientCredentials;
        private OAuth2GrantFlow authorizationCodeWithPkce;
    }

    @Data
    public static class OAuth2GrantFlow {
        private String name;
        private String description;
        private String tokenUrl;
        // only required for Authorization Code grant
        private String authorizationUrl;
        private Map<String, OAuth2Scope> scopes;
    }

    @Data
    public static class OAuth2Scope {
        private String name;
        private String description;
        private AccessTier requiredAccessTier = new TierPublic();
    }

    @Data
    public static class Server {
        private String name;
        private String url;
        private boolean enabled;
    }

    @Data
    public static class ResponseHeader {
        private String description;
        private boolean required;
        private String schema;
        private String example;
    }

}
