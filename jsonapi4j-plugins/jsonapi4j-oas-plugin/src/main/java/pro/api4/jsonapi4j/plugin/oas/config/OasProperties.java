package pro.api4.jsonapi4j.plugin.oas.config;

import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.principal.tier.AccessTier;

import java.util.Map;

public interface OasProperties {

    String OAS_PROPERTY_NAME = "oas";
    String DEFAULT_OAS_ROOT_PATH = JsonApi4jProperties.JSONAPI4J_DEFAULT_ROOT_PATH + "/oas";

    default String oasRootPath() {
        return DEFAULT_OAS_ROOT_PATH;
    }

    Info info();

    ExternalDocumentation externalDocumentation();

    OAuth2 oauth2();

    Map<String, ? extends Server> servers();

    <T extends ResponseHeader> Map<String, Map<String, T>> customResponseHeaders();

    interface Info {

        String title();

        String description();

        Contact contact();

        String version();

        String termsOfService();

        License license();

        Map<String, Object> extensions();

    }

    interface Contact {

        String name();

        String url();

        String email();

    }

    interface License {

        String name();

        String url();

        String identifier();

    }

    interface ExternalDocumentation {

        String url();

        String description();

    }

    interface OAuth2 {

        OAuth2GrantFlow clientCredentials();

        OAuth2GrantFlow authorizationCodeWithPkce();

    }

    interface OAuth2GrantFlow {

        String name();

        String description();

        String tokenUrl();

        // only required for Authorization Code grant
        String authorizationUrl();

        Map<String, ? extends OAuth2Scope> scopes();

    }

    interface OAuth2Scope {

        String name();

        String description();

        AccessTier requiredAccessTier();

    }

    interface Server {

        String name();

        String url();

        boolean enabled();

    }

    interface ResponseHeader {

        String description();

        boolean required();

        String schema();

        String example();

    }

}
