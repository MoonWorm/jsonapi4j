package pro.api4.jsonapi4j.oas.customizer;

import pro.api4.jsonapi4j.config.OasProperties;
import pro.api4.jsonapi4j.config.OasProperties.OAuth2GrantFlow;
import pro.api4.jsonapi4j.oas.customizer.util.OasOperationInfoUtil;
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

import static pro.api4.jsonapi4j.oas.OasSecuritySchemaExtensions.X_SCOPES_REQUIRED_ACCESS_TIER_EXTENSION;
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
        if (oasProperties.getInfo() != null) {
            if (openApi.getInfo() == null) {
                openApi.setInfo(new Info());
            }
            if (StringUtils.isNotBlank(oasProperties.getInfo().getTitle())) {
                openApi.getInfo().setTitle(oasProperties.getInfo().getTitle());
            }
            if (StringUtils.isNotBlank(oasProperties.getInfo().getVersion())) {
                openApi.getInfo().setVersion(oasProperties.getInfo().getVersion());
            }
            if (StringUtils.isNotBlank(oasProperties.getInfo().getDescription())) {
                openApi.getInfo().setDescription(oasProperties.getInfo().getDescription());
            }
            if (StringUtils.isNotBlank(oasProperties.getInfo().getTermsOfService())) {
                openApi.getInfo().setTermsOfService(oasProperties.getInfo().getTermsOfService());
            }
            if (MapUtils.isEmpty(oasProperties.getInfo().getExtensions())) {
                openApi.getInfo().setExtensions(oasProperties.getInfo().getExtensions());
            }

            if (oasProperties.getInfo().getContact() != null) {
                if (openApi.getInfo().getContact() == null) {
                    openApi.getInfo().setContact(new Contact());
                }
                if (StringUtils.isNotBlank(oasProperties.getInfo().getContact().getName())) {
                    openApi.getInfo().getContact().setName(oasProperties.getInfo().getContact().getName());
                }
                if (StringUtils.isNotBlank(oasProperties.getInfo().getContact().getUrl())) {
                    openApi.getInfo().getContact().setUrl(oasProperties.getInfo().getContact().getUrl());
                }
                if (StringUtils.isNotBlank(oasProperties.getInfo().getContact().getEmail())) {
                    openApi.getInfo().getContact().setEmail(oasProperties.getInfo().getContact().getEmail());
                }
            }

            if (oasProperties.getInfo().getLicense() != null) {
                if (openApi.getInfo().getLicense() == null) {
                    openApi.getInfo().setLicense(new License());
                }
                if (StringUtils.isNotBlank(oasProperties.getInfo().getLicense().getName())) {
                    openApi.getInfo().getLicense().setName(oasProperties.getInfo().getLicense().getName());
                }
                if (StringUtils.isNotBlank(oasProperties.getInfo().getLicense().getUrl())) {
                    openApi.getInfo().getLicense().setUrl(oasProperties.getInfo().getLicense().getUrl());
                }
                if (StringUtils.isNotBlank(oasProperties.getInfo().getLicense().getIdentifier())) {
                    openApi.getInfo().getLicense().setIdentifier(oasProperties.getInfo().getLicense().getIdentifier());
                }
            }
        }
    }

    private void enrichExternalDocs(OpenAPI openApi) {
        if (oasProperties.getExternalDocumentation() != null) {
            if (openApi.getExternalDocs() == null) {
                openApi.setExternalDocs(new ExternalDocumentation());
            }
            if (StringUtils.isNotBlank(oasProperties.getExternalDocumentation().getUrl())) {
                openApi.getExternalDocs().setUrl(oasProperties.getExternalDocumentation().getUrl());
            }
            if (StringUtils.isNotBlank(oasProperties.getExternalDocumentation().getDescription())) {
                openApi.getExternalDocs().setDescription(oasProperties.getExternalDocumentation().getDescription());
            }
        }
    }

    private void enrichServers(OpenAPI openApi) {
        if (MapUtils.isNotEmpty(oasProperties.getServers())) {
            if (openApi.getServers() == null) {
                openApi.setServers(new ArrayList<>());
            }
            List<Server> servers = oasProperties.getServers().values()
                    .stream()
                    .filter(OasProperties.Server::isEnabled)
                    .map(config -> new Server()
                            .description(config.getName())
                            .url(config.getUrl())
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
        OAuth2GrantFlow clientCredentialsFlow = oasProperties.getOauth2() != null
                ? oasProperties.getOauth2().getClientCredentials()
                : null;
        OAuth2GrantFlow authorizationCodeWithPkce = oasProperties.getOauth2() != null
                ? oasProperties.getOauth2().getAuthorizationCodeWithPkce()
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
            schemes.put(clientCredentialsFlow.getName(),
                    new SecurityScheme()
                            .type(SecurityScheme.Type.OAUTH2)
                            .description(clientCredentialsFlow.getDescription())
                            .flows(
                                    new OAuthFlows().clientCredentials(
                                            new OAuthFlow()
                                                    .tokenUrl(clientCredentialsFlow.getTokenUrl())
                                                    .scopes(getScopes(clientCredentialsFlow))
                                    )
                            )
            );
        }
        if (authorizationCodeWithPkceFlow != null) {
            schemes.put(authorizationCodeWithPkceFlow.getName(),
                    new SecurityScheme()
                            .type(SecurityScheme.Type.OAUTH2)
                            .description(authorizationCodeWithPkceFlow.getDescription())
                            .flows(
                                    new OAuthFlows().authorizationCode(
                                            new OAuthFlow()
                                                    .tokenUrl(authorizationCodeWithPkceFlow.getTokenUrl())
                                                    .authorizationUrl(authorizationCodeWithPkceFlow.getAuthorizationUrl())
                                                    .scopes(getScopes(authorizationCodeWithPkceFlow))
                                    )
                            ).extensions(getScopesExtensions(authorizationCodeWithPkceFlow))
            );
        }

        return schemes;
    }

    private Scopes getScopes(OAuth2GrantFlow oauth2GrantFlow) {
        Scopes scopes = new Scopes();
        if (oauth2GrantFlow.getScopes() != null && !oauth2GrantFlow.getScopes().isEmpty()) {
            oauth2GrantFlow.getScopes().values().forEach(scope -> scopes.addString(
                    scope.getName(),
                    scope.getDescription()
            ));
        }
        return scopes;
    }

    private Map<String, Object> getScopesExtensions(OAuth2GrantFlow oauth2GrantFlow) {
        if (oauth2GrantFlow.getScopes() != null && !oauth2GrantFlow.getScopes().isEmpty()) {
            Map<String, Object> extensions = new LinkedHashMap<>();
            extensions.put(
                    X_SCOPES_REQUIRED_ACCESS_TIER_EXTENSION,
                    oauth2GrantFlow.getScopes().values().stream()
                            .collect(
                                    toMap(
                                            OasProperties.OAuth2Scope::getName,
                                            OasProperties.OAuth2Scope::getRequiredAccessTier
                                    )
                            )
            );
            return extensions;
        }
        return Collections.emptyMap();
    }

}
