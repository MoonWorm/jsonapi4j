package pro.api4.jsonapi4j.plugin.oas.config;

import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.principal.tier.AccessTier;

import java.util.List;
import java.util.Map;

public interface OasProperties {

    String DEFAULT_OAS_ENABLED = "true";

    String OAS_PROPERTY_NAME = "oas";
    String DEFAULT_OAS_ROOT_PATH = JsonApi4jProperties.JSONAPI4J_DEFAULT_ROOT_PATH + "/oas";

    default boolean enabled() {
        return Boolean.parseBoolean(DEFAULT_OAS_ENABLED);
    }

    default String oasRootPath() {
        return DEFAULT_OAS_ROOT_PATH;
    }

    Info info();

    ExternalDocumentation externalDocumentation();

    OAuth2 oauth2();

    List<? extends Server> servers();

    List<? extends CustomResponseHeaderGroup> customResponseHeaders();

    interface Info {

        String OAS_INFO_TITLE_DEFAULT_VALUE = "JsonApi4j API Sample Title";
        String OAS_INFO_VERSION_DEFAULT_VALUE = "1.0.0";

        default String title() {
            return OAS_INFO_TITLE_DEFAULT_VALUE;
        }

        String description();

        Contact contact();

        default String version() {
            return OAS_INFO_VERSION_DEFAULT_VALUE;
        }

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

        List<? extends OAuth2Scope> scopes();

    }

    interface OAuth2Scope {

        String name();

        String description();

        AccessTier requiredAccessTier();

    }

    interface Server {

        String DEFAULT_OAS_SERVER_ENABLED = "false";

        String name();

        String url();

        default boolean enabled() {
            return Boolean.parseBoolean(DEFAULT_OAS_SERVER_ENABLED);
        }

    }

    interface CustomResponseHeaderGroup {

        String httpStatusCode();

        List<? extends ResponseHeader> headers();

    }

    interface ResponseHeader {

        String DEFAULT_OAS_RESPONSE_HEADER_REQUIRED = "false";
        String DEFAULT_OAS_RESPONSE_HEADER_SCHEMA = "string";

        String name();

        String description();

        default boolean required() {
            return Boolean.parseBoolean(DEFAULT_OAS_RESPONSE_HEADER_REQUIRED);
        }

        default String schema() {
            return DEFAULT_OAS_RESPONSE_HEADER_SCHEMA;
        }

        String example();

    }

}
