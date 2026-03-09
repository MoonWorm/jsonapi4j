package pro.api4.jsonapi4j.plugin.oas.customizer;

import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties.OAuth2GrantFlow;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasOperationInfoUtil;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static pro.api4.jsonapi4j.plugin.oas.OasSecuritySchemaExtensions.X_SCOPES_REQUIRED_ACCESS_TIER_EXTENSION;
import static java.util.stream.Collectors.toMap;

@Data
public class CommonOpenApiCustomizer {

    private final OasProperties oasProperties;
    private final OperationsRegistry operationsRegistry;

    public void customise(OpenAPI openApi) {
        enrichOpenApiInfo(openApi);
        enrichExternalDocs(openApi);
        enrichServers(openApi);
        enrichTags(openApi);
        enrichSecuritySchemas(openApi);
    }

    private void enrichOpenApiInfo(OpenAPI openApi) {
        if (oasProperties != null) {
            if (oasProperties.info() != null) {
                if (openApi.getInfo() == null) {
                    openApi.setInfo(new Info());
                }
                if (StringUtils.isNotBlank(oasProperties.info().title())) {
                    openApi.getInfo().setTitle(oasProperties.info().title());
                }
                if (StringUtils.isNotBlank(oasProperties.info().version())) {
                    openApi.getInfo().setVersion(oasProperties.info().version());
                }
                if (StringUtils.isNotBlank(oasProperties.info().description())) {
                    openApi.getInfo().setDescription(oasProperties.info().description());
                }
                if (StringUtils.isNotBlank(oasProperties.info().termsOfService())) {
                    openApi.getInfo().setTermsOfService(oasProperties.info().termsOfService());
                }
                if (MapUtils.isEmpty(oasProperties.info().extensions())) {
                    openApi.getInfo().setExtensions(oasProperties.info().extensions());
                }

                if (oasProperties.info().contact() != null) {
                    if (openApi.getInfo().getContact() == null) {
                        openApi.getInfo().setContact(new Contact());
                    }
                    if (StringUtils.isNotBlank(oasProperties.info().contact().name())) {
                        openApi.getInfo().getContact().setName(oasProperties.info().contact().name());
                    }
                    if (StringUtils.isNotBlank(oasProperties.info().contact().url())) {
                        openApi.getInfo().getContact().setUrl(oasProperties.info().contact().url());
                    }
                    if (StringUtils.isNotBlank(oasProperties.info().contact().email())) {
                        openApi.getInfo().getContact().setEmail(oasProperties.info().contact().email());
                    }
                }

                if (oasProperties.info().license() != null) {
                    if (openApi.getInfo().getLicense() == null) {
                        openApi.getInfo().setLicense(new License());
                    }
                    if (StringUtils.isNotBlank(oasProperties.info().license().name())) {
                        openApi.getInfo().getLicense().setName(oasProperties.info().license().name());
                    }
                    if (StringUtils.isNotBlank(oasProperties.info().license().url())) {
                        openApi.getInfo().getLicense().setUrl(oasProperties.info().license().url());
                    }
                    if (StringUtils.isNotBlank(oasProperties.info().license().identifier())) {
                        openApi.getInfo().getLicense().setIdentifier(oasProperties.info().license().identifier());
                    }
                }
            }
        }
    }

    private void enrichExternalDocs(OpenAPI openApi) {
        if (oasProperties.externalDocumentation() != null) {
            if (openApi.getExternalDocs() == null) {
                openApi.setExternalDocs(new ExternalDocumentation());
            }
            if (StringUtils.isNotBlank(oasProperties.externalDocumentation().url())) {
                openApi.getExternalDocs().setUrl(oasProperties.externalDocumentation().url());
            }
            if (StringUtils.isNotBlank(oasProperties.externalDocumentation().description())) {
                openApi.getExternalDocs().setDescription(oasProperties.externalDocumentation().description());
            }
        }
    }

    private void enrichServers(OpenAPI openApi) {
        if (CollectionUtils.isNotEmpty(oasProperties.servers())) {
            if (openApi.getServers() == null) {
                openApi.setServers(new ArrayList<>());
            }
            List<Server> servers = oasProperties.servers()
                    .stream()
                    .filter(OasProperties.Server::enabled)
                    .map(config -> new Server()
                            .description(config.name())
                            .url(config.url())
                    ).toList();
            openApi.getServers().addAll(servers);
        }
    }

    private void enrichTags(OpenAPI openApi) {
        List<Tag> jsonApiTags = operationsRegistry.getResourceTypesWithAnyOperationConfigured()
                .stream()
                .map(OasOperationInfoUtil::resolveOperationTag)
                .sorted()
                .map(t -> new Tag().name(t))
                .toList();
        if (CollectionUtils.isNotEmpty(jsonApiTags)) {
            if (CollectionUtils.isEmpty(openApi.getTags())) {
                openApi.setTags(new ArrayList<>());
            }
            openApi.getTags().addAll(jsonApiTags);
        }
    }

    private void enrichSecuritySchemas(OpenAPI openApi) {
        OAuth2GrantFlow clientCredentialsFlow = oasProperties.oauth2() != null
                ? oasProperties.oauth2().clientCredentials()
                : null;
        OAuth2GrantFlow authorizationCodeWithPkce = oasProperties.oauth2() != null
                ? oasProperties.oauth2().authorizationCodeWithPkce()
                : null;
        if (clientCredentialsFlow != null || authorizationCodeWithPkce != null) {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            if (openApi.getComponents().getSchemas() == null) {
                openApi.getComponents().setSecuritySchemes(new HashMap<>());
            }
            openApi.getComponents().getSecuritySchemes().putAll(
                    createSecuritySchemesMap(
                            clientCredentialsFlow,
                            authorizationCodeWithPkce
                    )
            );
        }
    }

    private Map<String, SecurityScheme> createSecuritySchemesMap(OAuth2GrantFlow clientCredentialsFlow,
                                                                 OAuth2GrantFlow authorizationCodeWithPkceFlow) {
        Map<String, SecurityScheme> schemes = new HashMap<>();
        if (clientCredentialsFlow != null) {
            schemes.put(clientCredentialsFlow.name(),
                    new SecurityScheme()
                            .type(SecurityScheme.Type.OAUTH2)
                            .description(clientCredentialsFlow.description())
                            .flows(
                                    new OAuthFlows().clientCredentials(
                                            new OAuthFlow()
                                                    .tokenUrl(clientCredentialsFlow.tokenUrl())
                                                    .scopes(getScopes(clientCredentialsFlow))
                                    )
                            )
            );
        }
        if (authorizationCodeWithPkceFlow != null) {
            schemes.put(authorizationCodeWithPkceFlow.name(),
                    new SecurityScheme()
                            .type(SecurityScheme.Type.OAUTH2)
                            .description(authorizationCodeWithPkceFlow.description())
                            .flows(
                                    new OAuthFlows().authorizationCode(
                                            new OAuthFlow()
                                                    .tokenUrl(authorizationCodeWithPkceFlow.tokenUrl())
                                                    .authorizationUrl(authorizationCodeWithPkceFlow.authorizationUrl())
                                                    .scopes(getScopes(authorizationCodeWithPkceFlow))
                                    )
                            ).extensions(getScopesExtensions(authorizationCodeWithPkceFlow))
            );
        }

        return schemes;
    }

    private Scopes getScopes(OAuth2GrantFlow oauth2GrantFlow) {
        Scopes scopes = new Scopes();
        if (oauth2GrantFlow.scopes() != null && !oauth2GrantFlow.scopes().isEmpty()) {
            oauth2GrantFlow.scopes().forEach(scope -> scopes.addString(
                    scope.name(),
                    scope.description()
            ));
        }
        return scopes;
    }

    private Map<String, Object> getScopesExtensions(OAuth2GrantFlow oauth2GrantFlow) {
        if (oauth2GrantFlow.scopes() != null && !oauth2GrantFlow.scopes().isEmpty()) {
            Map<String, Object> extensions = new LinkedHashMap<>();
            extensions.put(
                    X_SCOPES_REQUIRED_ACCESS_TIER_EXTENSION,
                    oauth2GrantFlow.scopes().stream()
                            .collect(
                                    toMap(
                                            OasProperties.OAuth2Scope::name,
                                            OasProperties.OAuth2Scope::requiredAccessTier
                                    )
                            )
            );
            return extensions;
        }
        return Collections.emptyMap();
    }

}
