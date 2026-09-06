package pro.api4.jsonapi4j.plugin.oas.config;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.config.PluginProperties;
import pro.api4.jsonapi4j.config.PluginPropertiesValidationResult;
import pro.api4.jsonapi4j.config.PluginPropertiesValidationResult.PluginPropertiesValidationResultBuilder;

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
    default PluginPropertiesValidationResult validate() {
        if (!enabled()) {
            return PluginPropertiesValidationResult.empty();
        }
        PluginPropertiesValidationResultBuilder builder = PluginPropertiesValidationResult.builder();
        validateOasRootPath(builder);
        validateInfo(builder);
        validateExternalDocumentation(builder);
        validateServers(builder);
        validateOAuth2(builder);
        validateCustomResponseHeaders(builder);
        return builder.build();
    }

    /**
     * The OAS servlet is mounted on {@code oasRootPath() + "/*"}, which only yields a legal servlet mapping for an
     * absolute path without a trailing slash.
     */
    private void validateOasRootPath(PluginPropertiesValidationResultBuilder builder) {
        String oasRootPath = oasRootPath();
        String path = propertyPath("oasRootPath");
        if (StringUtils.isBlank(oasRootPath)) {
            builder.requireNotBlank(path, oasRootPath);
            return;
        }
        if (!oasRootPath.startsWith("/")) {
            builder.addPropertyError(path, String.format(
                    "must start with '/', but was '%s'", oasRootPath
            ));
        }
        if (oasRootPath.length() > 1 && oasRootPath.endsWith("/")) {
            builder.addPropertyError(path, String.format(
                    "must not end with '/', but was '%s'", oasRootPath
            ));
        }
    }

    private void validateInfo(PluginPropertiesValidationResultBuilder builder) {
        Info info = info();
        if (info == null) {
            return;
        }
        builder.requireNotBlank(propertyPath("info", "title"), info.title())
                .requireNotBlank(propertyPath("info", "version"), info.version());
        if (StringUtils.isNotBlank(info.termsOfService())) {
            builder.requireHttpUrl(propertyPath("info", "termsOfService"), info.termsOfService());
        }
        validateExtensions(builder, info.extensions());
        validateContact(builder, info.contact());
        validateLicense(builder, info.license());
    }

    /**
     * Extensions are emitted into the document verbatim, and the OpenAPI spec only allows extension keys prefixed
     * with {@code x-}.
     */
    private void validateExtensions(PluginPropertiesValidationResultBuilder builder,
                                    Map<String, Object> extensions) {
        if (MapUtils.isEmpty(extensions)) {
            return;
        }
        extensions.keySet()
                .stream()
                .filter(name -> !StringUtils.startsWith(name, "x-"))
                .forEach(name -> builder.addPropertyError(
                        propertyPath("info", "extensions", name),
                        "must be prefixed with 'x-' to be a valid OpenAPI extension"
                ));
    }

    private void validateContact(PluginPropertiesValidationResultBuilder builder,
                                 Contact contact) {
        if (contact == null) {
            return;
        }
        if (StringUtils.isNotBlank(contact.url())) {
            builder.requireHttpUrl(propertyPath("info", "contact", "url"), contact.url());
        }
        if (StringUtils.isNotBlank(contact.email()) && !contact.email().trim().matches(EMAIL_PATTERN)) {
            builder.addPropertyError(propertyPath("info", "contact", "email"), String.format(
                    "must be a valid email address, but was '%s'", contact.email()
            ));
        }
    }

    private void validateLicense(PluginPropertiesValidationResultBuilder builder,
                                 License license) {
        if (license == null) {
            return;
        }
        builder.requireNotBlank(propertyPath("info", "license", "name"), license.name());
        if (StringUtils.isNotBlank(license.url())) {
            builder.requireHttpUrl(propertyPath("info", "license", "url"), license.url());
        }
        if (StringUtils.isNotBlank(license.url()) && StringUtils.isNotBlank(license.identifier())) {
            builder.addCrossPropertiesError(String.format(
                    "'%s' and '%s' are mutually exclusive - declare either the SPDX identifier or the URL of the " +
                            "license, not both",
                    propertyPath("info", "license", "url"),
                    propertyPath("info", "license", "identifier")
            ));
        }
    }

    /**
     * The URL is what makes an external documentation entry - the OpenAPI spec has it as the only required field.
     */
    private void validateExternalDocumentation(PluginPropertiesValidationResultBuilder builder) {
        ExternalDocumentation externalDocumentation = externalDocumentation();
        if (externalDocumentation == null) {
            return;
        }
        builder.requireHttpUrl(propertyPath("externalDocumentation", "url"), externalDocumentation.url());
    }

    /**
     * A disabled server never reaches the document, so it is left alone - keeping a placeholder entry around is a
     * legitimate way to park an environment.
     */
    private void validateServers(PluginPropertiesValidationResultBuilder builder) {
        List<? extends Server> servers = servers();
        if (CollectionUtils.isEmpty(servers)) {
            return;
        }
        for (int i = 0; i < servers.size(); i++) {
            Server server = servers.get(i);
            if (server == null || !server.enabled()) {
                continue;
            }
            builder.requireNotBlank(propertyPath(indexed("servers", i), "url"), server.url());
        }
    }

    private void validateOAuth2(PluginPropertiesValidationResultBuilder builder) {
        OAuth2 oauth2 = oauth2();
        if (oauth2 == null) {
            return;
        }
        OAuth2GrantFlow clientCredentials = oauth2.clientCredentials();
        OAuth2GrantFlow authorizationCodeWithPkce = oauth2.authorizationCodeWithPkce();
        validateGrantFlow(builder, "clientCredentials", clientCredentials, false);
        validateGrantFlow(builder, "authorizationCodeWithPkce", authorizationCodeWithPkce, true);
        if (clientCredentials != null
                && authorizationCodeWithPkce != null
                && StringUtils.isNotBlank(clientCredentials.name())
                && StringUtils.equals(clientCredentials.name(), authorizationCodeWithPkce.name())) {
            builder.addCrossPropertiesError(String.format(
                    "'%s' and '%s' are both '%s' - each grant flow becomes a security scheme keyed by its name, so " +
                            "one would silently replace the other",
                    propertyPath("oauth2", "clientCredentials", "name"),
                    propertyPath("oauth2", "authorizationCodeWithPkce", "name"),
                    clientCredentials.name()
            ));
        }
    }

    private void validateGrantFlow(PluginPropertiesValidationResultBuilder builder,
                                   String flow,
                                   OAuth2GrantFlow grantFlow,
                                   boolean authorizationUrlRequired) {
        if (grantFlow == null) {
            return;
        }
        builder.requireNotBlank(propertyPath("oauth2", flow, "name"), grantFlow.name())
                .requireHttpUrl(propertyPath("oauth2", flow, "tokenUrl"), grantFlow.tokenUrl());
        if (authorizationUrlRequired) {
            builder.requireHttpUrl(propertyPath("oauth2", flow, "authorizationUrl"), grantFlow.authorizationUrl());
        }
        List<? extends OAuth2Scope> scopes = grantFlow.scopes();
        if (CollectionUtils.isEmpty(scopes)) {
            return;
        }
        for (int i = 0; i < scopes.size(); i++) {
            OAuth2Scope scope = scopes.get(i);
            builder.requireNotBlank(
                    propertyPath("oauth2", flow, indexed("scopes", i), "name"),
                    scope == null ? null : scope.name()
            );
        }
    }

    private void validateCustomResponseHeaders(PluginPropertiesValidationResultBuilder builder) {
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
    private void validateHttpStatusCode(PluginPropertiesValidationResultBuilder builder,
                                        CustomResponseHeaderGroup group,
                                        int index,
                                        Set<String> declaredStatusCodes) {
        String path = propertyPath(indexed("customResponseHeaders", index), "httpStatusCode");
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
                    propertyPath("customResponseHeaders"), httpStatusCode
            ));
        }
    }

    private void validateResponseHeaders(PluginPropertiesValidationResultBuilder builder,
                                         CustomResponseHeaderGroup group,
                                         int index) {
        String groupPath = indexed("customResponseHeaders", index);
        List<? extends ResponseHeader> headers = group.headers();
        if (CollectionUtils.isEmpty(headers)) {
            builder.addPropertyError(propertyPath(groupPath, "headers"), "must declare at least one header");
            return;
        }
        for (int i = 0; i < headers.size(); i++) {
            ResponseHeader header = headers.get(i);
            String headerPath = indexed("headers", i);
            builder.requireNotBlank(
                            propertyPath(groupPath, headerPath, "name"),
                            header == null ? null : header.name()
                    )
                    .requireOneOfIgnoringCase(
                            propertyPath(groupPath, headerPath, "schema"),
                            header == null ? null : header.schema(),
                            ResponseHeader.SUPPORTED_SCHEMAS
                    );
        }
    }

    private static String indexed(String property, int index) {
        return String.format("%s[%d]", property, index);
    }

    interface Info {

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

    }

    interface Server {

        String DEFAULT_SERVER_ENABLED = "false";

        String name();

        String url();

        default boolean enabled() {
            return Boolean.parseBoolean(DEFAULT_SERVER_ENABLED);
        }

    }

    interface CustomResponseHeaderGroup {

        String httpStatusCode();

        List<? extends ResponseHeader> headers();

    }

    interface ResponseHeader {

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
