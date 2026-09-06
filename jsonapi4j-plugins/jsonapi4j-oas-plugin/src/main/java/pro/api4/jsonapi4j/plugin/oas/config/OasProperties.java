package pro.api4.jsonapi4j.plugin.oas.config;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.config.PluginProperties;
import pro.api4.jsonapi4j.config.PropertiesValidationResult;
import pro.api4.jsonapi4j.config.PropertiesValidationResult.PropertiesValidationResultBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface OasProperties extends PluginProperties {

    String OAS_PROPERTY = "oas";

    @Override
    default String section() {
        return OAS_PROPERTY;
    }

    String ENABLED_PROPERTY = "enabled";
    String OAS_ROOT_PATH_PROPERTY = "oasRootPath";
    String INFO_PROPERTY = "info";
    String EXTERNAL_DOCUMENTATION_PROPERTY = "externalDocumentation";
    String OAUTH2_PROPERTY = "oauth2";
    String SERVERS_PROPERTY = "servers";
    String CUSTOM_RESPONSE_HEADERS_PROPERTY = "customResponseHeaders";

    String DEFAULT_ENABLED = "true";
    String DEFAULT_OAS_ROOT_PATH = JsonApi4jProperties.DEFAULT_ROOT_PATH + "/oas";

    String EMAIL_PATTERN = "[^\\s@]+@[^\\s@]+\\.[^\\s@]+";
    String HTTP_STATUS_CODE_PATTERN = "\\d{3}";

    default boolean enabled() {
        return Boolean.parseBoolean(DEFAULT_ENABLED);
    }

    default String oasRootPath() {
        return DEFAULT_OAS_ROOT_PATH;
    }

    Info info();

    ExternalDocumentation externalDocumentation();

    OAuth2 oauth2();

    List<? extends Server> servers();

    List<? extends CustomResponseHeaderGroup> customResponseHeaders();

    @Override
    default PropertiesValidationResult validate() {
        if (!enabled()) {
            return PropertiesValidationResult.empty();
        }
        PropertiesValidationResultBuilder builder = PropertiesValidationResult.builder();
        validateOasRootPath(builder);
        validateInfo(builder);
        validateExternalDocumentation(builder);
        validateServers(builder);
        validateOAuth2(builder);
        validateCustomResponseHeaders(builder);
        return builder.build();
    }

    /**
     * The OAS servlet is mounted on {@code oasRootPath() + "/*"}, so it has to be a legal servlet mapping prefix.
     */
    private void validateOasRootPath(PropertiesValidationResultBuilder builder) {
        builder.requireServletPath(propertyPath(OAS_ROOT_PATH_PROPERTY), oasRootPath());
    }

    /**
     * The OpenAPI document has to live under the JSON:API root path, and the integrations do not agree on how they
     * get there: Spring Boot mounts the servlet on {@code jsonapi4j.rootPath + "/oas"} and never reads
     * {@code oasRootPath}, while Quarkus and the plain Servlet integration mount it on {@code oasRootPath} itself.
     * A value outside the root path therefore serves the document at a different URL depending on the host.
     */
    @Override
    default PropertiesValidationResult validateAgainst(JsonApi4jProperties rootProperties) {
        if (!enabled() || rootProperties == null) {
            return PropertiesValidationResult.empty();
        }
        String rootPath = StringUtils.trimToNull(rootProperties.rootPath());
        String oasRootPath = StringUtils.trimToNull(oasRootPath());
        if (rootPath == null || oasRootPath == null || "/".equals(rootPath) || !oasRootPath.startsWith("/")) {
            return PropertiesValidationResult.empty();
        }
        if (!oasRootPath.equals(rootPath) && !oasRootPath.startsWith(rootPath + "/")) {
            return PropertiesValidationResult.builder()
                    .addCrossPropertiesError(String.format(
                            "'%s' ('%s') must be under '%s' ('%s') - the Spring Boot integration mounts the OpenAPI " +
                                    "servlet under the JSON:API root path while Quarkus and the Servlet integration " +
                                    "mount it on '%s', so a path outside the root path serves the document at a " +
                                    "different URL depending on the host",
                            propertyPath(OAS_ROOT_PATH_PROPERTY), oasRootPath,
                            rootProperties.propertyPath(JsonApi4jProperties.ROOT_PATH_PROPERTY), rootPath,
                            propertyPath(OAS_ROOT_PATH_PROPERTY)
                    ))
                    .build();
        }
        return PropertiesValidationResult.empty();
    }

    private void validateInfo(PropertiesValidationResultBuilder builder) {
        Info info = info();
        if (info == null) {
            return;
        }
        builder.requireNotBlank(propertyPath(INFO_PROPERTY, Info.TITLE_PROPERTY), info.title())
                .requireNotBlank(propertyPath(INFO_PROPERTY, Info.VERSION_PROPERTY), info.version());
        if (StringUtils.isNotBlank(info.termsOfService())) {
            builder.requireHttpUrl(propertyPath(INFO_PROPERTY, Info.TERMS_OF_SERVICE_PROPERTY), info.termsOfService());
        }
        validateExtensions(builder, info.extensions());
        validateContact(builder, info.contact());
        validateLicense(builder, info.license());
    }

    /**
     * Extensions are emitted into the document verbatim, and the OpenAPI spec only allows extension keys prefixed
     * with {@code x-}.
     */
    private void validateExtensions(PropertiesValidationResultBuilder builder,
                                    Map<String, Object> extensions) {
        if (MapUtils.isEmpty(extensions)) {
            return;
        }
        extensions.keySet()
                .stream()
                .filter(name -> !StringUtils.startsWith(name, "x-"))
                .forEach(name -> builder.addPropertyError(
                        propertyPath(INFO_PROPERTY, Info.EXTENSIONS_PROPERTY, name),
                        "must be prefixed with 'x-' to be a valid OpenAPI extension"
                ));
    }

    private void validateContact(PropertiesValidationResultBuilder builder,
                                 Contact contact) {
        if (contact == null) {
            return;
        }
        if (StringUtils.isNotBlank(contact.url())) {
            builder.requireHttpUrl(propertyPath(INFO_PROPERTY, Info.CONTACT_PROPERTY, Contact.URL_PROPERTY), contact.url());
        }
        if (StringUtils.isNotBlank(contact.email()) && !contact.email().trim().matches(EMAIL_PATTERN)) {
            builder.addPropertyError(propertyPath(INFO_PROPERTY, Info.CONTACT_PROPERTY, Contact.EMAIL_PROPERTY), String.format(
                    "must be a valid email address, but was '%s'", contact.email()
            ));
        }
    }

    private void validateLicense(PropertiesValidationResultBuilder builder,
                                 License license) {
        if (license == null) {
            return;
        }
        builder.requireNotBlank(propertyPath(INFO_PROPERTY, Info.LICENSE_PROPERTY, License.NAME_PROPERTY), license.name());
        if (StringUtils.isNotBlank(license.url())) {
            builder.requireHttpUrl(propertyPath(INFO_PROPERTY, Info.LICENSE_PROPERTY, License.URL_PROPERTY), license.url());
        }
        if (StringUtils.isNotBlank(license.url()) && StringUtils.isNotBlank(license.identifier())) {
            builder.addCrossPropertiesError(String.format(
                    "'%s' and '%s' are mutually exclusive - declare either the SPDX identifier or the URL of the " +
                            "license, not both",
                    propertyPath(INFO_PROPERTY, Info.LICENSE_PROPERTY, License.URL_PROPERTY),
                    propertyPath(INFO_PROPERTY, Info.LICENSE_PROPERTY, License.IDENTIFIER_PROPERTY)
            ));
        }
    }

    /**
     * The URL is what makes an external documentation entry - the OpenAPI spec has it as the only required field.
     */
    private void validateExternalDocumentation(PropertiesValidationResultBuilder builder) {
        ExternalDocumentation externalDocumentation = externalDocumentation();
        if (externalDocumentation == null) {
            return;
        }
        builder.requireHttpUrl(propertyPath(EXTERNAL_DOCUMENTATION_PROPERTY, ExternalDocumentation.URL_PROPERTY), externalDocumentation.url());
    }

    /**
     * A disabled server never reaches the document, so it is left alone - keeping a placeholder entry around is a
     * legitimate way to park an environment.
     */
    private void validateServers(PropertiesValidationResultBuilder builder) {
        List<? extends Server> servers = servers();
        if (CollectionUtils.isEmpty(servers)) {
            return;
        }
        for (int i = 0; i < servers.size(); i++) {
            Server server = servers.get(i);
            if (server == null || !server.enabled()) {
                continue;
            }
            builder.requireNotBlank(propertyPath(indexed(SERVERS_PROPERTY, i), Server.URL_PROPERTY), server.url());
        }
    }

    private void validateOAuth2(PropertiesValidationResultBuilder builder) {
        OAuth2 oauth2 = oauth2();
        if (oauth2 == null) {
            return;
        }
        OAuth2GrantFlow clientCredentials = oauth2.clientCredentials();
        OAuth2GrantFlow authorizationCodeWithPkce = oauth2.authorizationCodeWithPkce();
        validateGrantFlow(builder, OAuth2.CLIENT_CREDENTIALS_PROPERTY, clientCredentials, false);
        validateGrantFlow(builder, OAuth2.AUTHORIZATION_CODE_WITH_PKCE_PROPERTY, authorizationCodeWithPkce, true);
        if (clientCredentials != null
                && authorizationCodeWithPkce != null
                && StringUtils.isNotBlank(clientCredentials.name())
                && StringUtils.equals(clientCredentials.name(), authorizationCodeWithPkce.name())) {
            builder.addCrossPropertiesError(String.format(
                    "'%s' and '%s' are both '%s' - each grant flow becomes a security scheme keyed by its name, so " +
                            "one would silently replace the other",
                    propertyPath(OAUTH2_PROPERTY, OAuth2.CLIENT_CREDENTIALS_PROPERTY, OAuth2GrantFlow.NAME_PROPERTY),
                    propertyPath(OAUTH2_PROPERTY, OAuth2.AUTHORIZATION_CODE_WITH_PKCE_PROPERTY, OAuth2GrantFlow.NAME_PROPERTY),
                    clientCredentials.name()
            ));
        }
    }

    private void validateGrantFlow(PropertiesValidationResultBuilder builder,
                                   String flow,
                                   OAuth2GrantFlow grantFlow,
                                   boolean authorizationUrlRequired) {
        if (grantFlow == null) {
            return;
        }
        builder.requireNotBlank(propertyPath(OAUTH2_PROPERTY, flow, OAuth2GrantFlow.NAME_PROPERTY), grantFlow.name())
                .requireHttpUrl(propertyPath(OAUTH2_PROPERTY, flow, OAuth2GrantFlow.TOKEN_URL_PROPERTY), grantFlow.tokenUrl());
        if (authorizationUrlRequired) {
            builder.requireHttpUrl(propertyPath(OAUTH2_PROPERTY, flow, OAuth2GrantFlow.AUTHORIZATION_URL_PROPERTY), grantFlow.authorizationUrl());
        }
        List<? extends OAuth2Scope> scopes = grantFlow.scopes();
        if (CollectionUtils.isEmpty(scopes)) {
            return;
        }
        for (int i = 0; i < scopes.size(); i++) {
            OAuth2Scope scope = scopes.get(i);
            builder.requireNotBlank(
                    propertyPath(OAUTH2_PROPERTY, flow, indexed(OAuth2GrantFlow.SCOPES_PROPERTY, i), OAuth2Scope.NAME_PROPERTY),
                    scope == null ? null : scope.name()
            );
        }
    }

    private void validateCustomResponseHeaders(PropertiesValidationResultBuilder builder) {
        List<? extends CustomResponseHeaderGroup> groups = customResponseHeaders();
        if (CollectionUtils.isEmpty(groups)) {
            return;
        }
        Set<String> declaredStatusCodes = new HashSet<>();
        for (int i = 0; i < groups.size(); i++) {
            CustomResponseHeaderGroup group = groups.get(i);
            if (group == null) {
                continue;
            }
            validateHttpStatusCode(builder, group, i, declaredStatusCodes);
            validateResponseHeaders(builder, group, i);
        }
    }

    /**
     * Groups are looked up by the exact status code an operation answers with, and only the first match is applied -
     * a wildcard or a duplicate would quietly contribute nothing.
     */
    private void validateHttpStatusCode(PropertiesValidationResultBuilder builder,
                                        CustomResponseHeaderGroup group,
                                        int index,
                                        Set<String> declaredStatusCodes) {
        String path = propertyPath(indexed(CUSTOM_RESPONSE_HEADERS_PROPERTY, index), CustomResponseHeaderGroup.HTTP_STATUS_CODE_PROPERTY);
        String httpStatusCode = StringUtils.trimToNull(group.httpStatusCode());
        if (httpStatusCode == null) {
            builder.requireNotBlank(path, group.httpStatusCode());
            return;
        }
        if (!httpStatusCode.matches(HTTP_STATUS_CODE_PATTERN)) {
            builder.addPropertyError(path, String.format(
                    "must be a 3-digit HTTP status code (e.g. '429'), but was '%s'", group.httpStatusCode()
            ));
            return;
        }
        if (!declaredStatusCodes.add(httpStatusCode)) {
            builder.addCrossPropertiesError(String.format(
                    "'%s' is declared more than once for the HTTP status code '%s' - only the first group is applied",
                    propertyPath(CUSTOM_RESPONSE_HEADERS_PROPERTY), httpStatusCode
            ));
        }
    }

    private void validateResponseHeaders(PropertiesValidationResultBuilder builder,
                                         CustomResponseHeaderGroup group,
                                         int index) {
        String groupPath = indexed(CUSTOM_RESPONSE_HEADERS_PROPERTY, index);
        List<? extends ResponseHeader> headers = group.headers();
        if (CollectionUtils.isEmpty(headers)) {
            builder.addPropertyError(propertyPath(groupPath, CustomResponseHeaderGroup.HEADERS_PROPERTY), "must declare at least one header");
            return;
        }
        for (int i = 0; i < headers.size(); i++) {
            ResponseHeader header = headers.get(i);
            String headerPath = indexed(CustomResponseHeaderGroup.HEADERS_PROPERTY, i);
            builder.requireNotBlank(
                            propertyPath(groupPath, headerPath, ResponseHeader.NAME_PROPERTY),
                            header == null ? null : header.name()
                    )
                    .requireOneOfIgnoringCase(
                            propertyPath(groupPath, headerPath, ResponseHeader.SCHEMA_PROPERTY),
                            header == null ? null : header.schema(),
                            ResponseHeader.SUPPORTED_SCHEMAS
                    );
        }
    }

    private static String indexed(String property, int index) {
        return String.format("%s[%d]", property, index);
    }

    interface Info {

        String TITLE_PROPERTY = "title";
        String DESCRIPTION_PROPERTY = "description";
        String VERSION_PROPERTY = "version";
        String TERMS_OF_SERVICE_PROPERTY = "termsOfService";
        String CONTACT_PROPERTY = "contact";
        String LICENSE_PROPERTY = "license";
        String EXTENSIONS_PROPERTY = "extensions";

        String DEFAULT_INFO_TITLE = "JsonApi4j API Sample Title";
        String DEFAULT_INFO_VERSION = "1.0.0";

        default String title() {
            return DEFAULT_INFO_TITLE;
        }

        String description();

        Contact contact();

        default String version() {
            return DEFAULT_INFO_VERSION;
        }

        String termsOfService();

        License license();

        Map<String, Object> extensions();

    }

    interface Contact {

        String NAME_PROPERTY = "name";
        String URL_PROPERTY = "url";
        String EMAIL_PROPERTY = "email";

        String name();

        String url();

        String email();

    }

    interface License {

        String NAME_PROPERTY = "name";
        String URL_PROPERTY = "url";
        String IDENTIFIER_PROPERTY = "identifier";

        String name();

        String url();

        String identifier();

    }

    interface ExternalDocumentation {

        String URL_PROPERTY = "url";
        String DESCRIPTION_PROPERTY = "description";

        String url();

        String description();

    }

    interface OAuth2 {

        String CLIENT_CREDENTIALS_PROPERTY = "clientCredentials";
        String AUTHORIZATION_CODE_WITH_PKCE_PROPERTY = "authorizationCodeWithPkce";

        OAuth2GrantFlow clientCredentials();

        OAuth2GrantFlow authorizationCodeWithPkce();

    }

    interface OAuth2GrantFlow {

        String NAME_PROPERTY = "name";
        String TOKEN_URL_PROPERTY = "tokenUrl";
        String AUTHORIZATION_URL_PROPERTY = "authorizationUrl";
        String SCOPES_PROPERTY = "scopes";

        String name();

        String description();

        String tokenUrl();

        // only required for Authorization Code grant
        String authorizationUrl();

        List<? extends OAuth2Scope> scopes();

    }

    interface OAuth2Scope {

        String NAME_PROPERTY = "name";

        String name();

        String description();

    }

    interface Server {

        String NAME_PROPERTY = "name";
        String URL_PROPERTY = "url";
        String ENABLED_PROPERTY = "enabled";

        String DEFAULT_SERVER_ENABLED = "false";

        String name();

        String url();

        default boolean enabled() {
            return Boolean.parseBoolean(DEFAULT_SERVER_ENABLED);
        }

    }

    interface CustomResponseHeaderGroup {

        String HTTP_STATUS_CODE_PROPERTY = "httpStatusCode";
        String HEADERS_PROPERTY = "headers";

        String httpStatusCode();

        List<? extends ResponseHeader> headers();

    }

    interface ResponseHeader {

        String NAME_PROPERTY = "name";
        String SCHEMA_PROPERTY = "schema";

        String DEFAULT_RESPONSE_HEADER_REQUIRED = "false";
        String DEFAULT_RESPONSE_HEADER_SCHEMA = "string";

        List<String> SUPPORTED_SCHEMAS = List.of("string", "integer");

        String name();

        String description();

        default boolean required() {
            return Boolean.parseBoolean(DEFAULT_RESPONSE_HEADER_REQUIRED);
        }

        default String schema() {
            return DEFAULT_RESPONSE_HEADER_SCHEMA;
        }

        String example();

    }

}
