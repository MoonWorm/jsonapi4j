package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.http.HttpStatusCodes;
import pro.api4.jsonapi4j.operation.OperationMeta;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.operation.RegisteredOperation;
import pro.api4.jsonapi4j.plugin.PluginRegistry;
import pro.api4.jsonapi4j.plugin.ac.annotation.Authenticated;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlScopesModel;
import pro.api4.jsonapi4j.plugin.ac.model.ScopesGroupModel;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasResourceTypes;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasSchemaNamesUtil;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.apache.commons.collections4.MapUtils.emptyIfNull;
import static pro.api4.jsonapi4j.plugin.oas.customizer.util.OasOperationInfoUtil.resolveRelationshipOperationPath;
import static pro.api4.jsonapi4j.plugin.oas.customizer.util.OasOperationInfoUtil.resolveResourceOperationPath;

/**
 * Describes what the access control plugin enforces: which scopes an operation needs, which ones are open to
 * anyone, and where a {@code 403} is reachable.
 * <p>
 * It lives here rather than in that plugin because OpenAPI is this module's concern - access control should not
 * have to know the document exists. The dependency is optional and this customizer is only constructed when the
 * plugin is registered and enabled, so an application without it never loads these classes.
 * <p>
 * Requirements declared through {@code @AccessControl} win over
 * {@link pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo.SecurityConfig#requiredScopes()}:
 * the document should state what is enforced, and access control is what enforces it.
 */
public class AccessControlOasCustomizer implements OasCustomizer {

    public static final String ACCESS_CONTROL_PLUGIN_NAME = "JsonApiAccessControlPlugin";

    /**
     * How many alternatives a scope requirement may expand into before it is left to prose. Two levels of
     * annotation cannot realistically reach this, but a document is easier to read wrong than to read long.
     */
    private static final int MAX_SECURITY_ALTERNATIVES = 16;

    private static final String FORBIDDEN = String.valueOf(HttpStatusCodes.SC_403_FORBIDDEN.getCode());

    private final String rootPath;
    private final DomainRegistry domainRegistry;
    private final OperationsRegistry operationsRegistry;
    private final OasProperties oasProperties;

    public AccessControlOasCustomizer(JsonApi4j jsonApi4j) {
        this.rootPath = jsonApi4j.getProperties().rootPath();
        this.domainRegistry = jsonApi4j.getDomainRegistry();
        this.operationsRegistry = jsonApi4j.getOperationsRegistry();
        this.oasProperties = jsonApi4j.getPluginRegistry().configOf(OasProperties.class).orElse(null);
    }

    public static boolean isEnabledFor(PluginRegistry pluginRegistry) {
        return pluginRegistry != null && pluginRegistry.isActivePlugin(ACCESS_CONTROL_PLUGIN_NAME);
    }

    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            return;
        }
        OasResourceTypes.resourceTypesWithOperationsExcludingMeta(domainRegistry, operationsRegistry)
                .forEach(resourceType -> {
                    describeResourceOperations(openApi, resourceType);
                    operationsRegistry.getRelationshipNamesWithAnyOperationConfigured(resourceType)
                            .forEach(relationshipName -> describeRelationshipOperations(openApi, resourceType, relationshipName));
                });
    }

    private void describeResourceOperations(OpenAPI openApi,
                                            ResourceType resourceType) {
        OperationType.getResourceOperationTypes().stream()
                .filter(operationType -> operationsRegistry.isResourceOperationConfigured(resourceType, operationType))
                .forEach(operationType -> describe(
                        openApi,
                        resolveResourceOperationPath(rootPath, operationType, resourceType),
                        operationType,
                        operationsRegistry.getRegisteredResourceOperation(resourceType, operationType, false)
                ));
    }

    private void describeRelationshipOperations(OpenAPI openApi,
                                                ResourceType resourceType,
                                                RelationshipName relationshipName) {
        OperationType.getAllRelationshipOperationTypes().stream()
                .filter(operationType -> operationsRegistry.isRelationshipOperationConfigured(resourceType, relationshipName, operationType))
                .forEach(operationType -> describe(
                        openApi,
                        resolveRelationshipOperationPath(rootPath, operationType, resourceType, relationshipName),
                        operationType,
                        operationsRegistry.getRegisteredRelationshipOperation(resourceType, relationshipName, operationType, false)
                ));
    }

    private void describe(OpenAPI openApi,
                          String path,
                          OperationType operationType,
                          RegisteredOperation<?> registeredOperation) {
        AccessControlModel accessControl = accessControlOf(registeredOperation);
        if (accessControl == null) {
            return;
        }
        Operation operation = operationAt(openApi, path, operationType);
        if (operation == null) {
            return;
        }
        if (isAnonymous(accessControl)) {
            operation.setSecurity(List.of());
        } else {
            securityRequirements(accessControl.getRequiredScopes(), operation)
                    .ifPresent(operation::setSecurity);
            if (operationType.getMethod() != OperationType.Method.GET) {
                addForbidden(operation);
            }
        }
        describeRequirements(operation, accessControl);
    }

    private AccessControlModel accessControlOf(RegisteredOperation<?> registeredOperation) {
        if (registeredOperation == null) {
            return null;
        }
        OperationMeta operationMeta = registeredOperation.getOperationMeta();
        Object pluginInfo = emptyIfNull(operationMeta.getPluginInfo()).get(ACCESS_CONTROL_PLUGIN_NAME);
        return pluginInfo instanceof AccessControlModel accessControl ? accessControl : null;
    }

    private Operation operationAt(OpenAPI openApi,
                                  String path,
                                  OperationType operationType) {
        PathItem pathItem = openApi.getPaths().get(path);
        return pathItem == null
                ? null
                : pathItem.readOperationsMap().get(PathItem.HttpMethod.valueOf(operationType.getMethod().name()));
    }

    private boolean isAnonymous(AccessControlModel accessControl) {
        return accessControl.getAuthenticated() != null
                && accessControl.getAuthenticated().getAuthenticated() == Authenticated.ANONYMOUS;
    }

    /**
     * Turns a scope requirement into OpenAPI's security array, which is disjunctive normal form: entries are
     * alternatives, and the scopes within one entry are all required. A two-level {@code ALL_OF}/{@code ANY_OF}
     * requirement is a monotone formula, so it always has such a form - {@code (A or B) and (C or D)} expands to
     * four alternatives. Only sheer size stops it, and then the requirement is left to prose rather than stated
     * wrongly.
     */
    private Optional<List<SecurityRequirement>> securityRequirements(AccessControlScopesModel requiredScopes,
                                                                     Operation operation) {
        if (requiredScopes == null || CollectionUtils.isEmpty(requiredScopes.getGroups())) {
            return Optional.empty();
        }
        List<Set<String>> alternatives = alternatives(requiredScopes);
        if (alternatives.isEmpty() || alternatives.size() > MAX_SECURITY_ALTERNATIVES) {
            return Optional.empty();
        }
        alternatives.forEach(this::requireDeclaredScopes);

        List<String> schemeNames = schemeNamesFor(operation);
        if (schemeNames.isEmpty()) {
            return Optional.empty();
        }
        List<SecurityRequirement> requirements = new ArrayList<>();
        for (String schemeName : schemeNames) {
            for (Set<String> alternative : alternatives) {
                requirements.add(new SecurityRequirement().addList(schemeName, List.copyOf(alternative)));
            }
        }
        return Optional.of(requirements);
    }

    private List<Set<String>> alternatives(AccessControlScopesModel requiredScopes) {
        List<List<Set<String>>> perGroup = requiredScopes.getGroups().stream()
                .map(this::alternativesOf)
                .toList();
        return requiredScopes.getMode() == AccessControlScopesModel.Mode.ANY_OF
                ? perGroup.stream().flatMap(List::stream).toList()
                : crossProduct(perGroup);
    }

    private List<Set<String>> alternativesOf(ScopesGroupModel group) {
        return group.getMode() == ScopesGroupModel.Mode.ALL_OF
                ? List.of(new LinkedHashSet<>(group.getScopes()))
                : group.getScopes().stream().map(scope -> (Set<String>) new LinkedHashSet<>(Set.of(scope))).toList();
    }

    private List<Set<String>> crossProduct(List<List<Set<String>>> perGroup) {
        List<Set<String>> combined = List.of(new LinkedHashSet<>());
        for (List<Set<String>> group : perGroup) {
            List<Set<String>> next = new ArrayList<>();
            for (Set<String> soFar : combined) {
                for (Set<String> alternative : group) {
                    Set<String> merged = new LinkedHashSet<>(soFar);
                    merged.addAll(alternative);
                    next.add(merged);
                    if (next.size() > MAX_SECURITY_ALTERNATIVES) {
                        return List.of();
                    }
                }
            }
            combined = next;
        }
        return combined;
    }

    /**
     * A scope named by access control but absent from {@code jsonapi4j.oas.oauth2.*.scopes} would be published as a
     * dangling reference, which is what breaks the Swagger UI authorize button. Two lists that must agree are worth
     * checking rather than hoping about.
     */
    private void requireDeclaredScopes(Set<String> scopes) {
        Set<String> declared = declaredScopes();
        if (declared.isEmpty()) {
            return;
        }
        scopes.stream()
                .filter(scope -> !declared.contains(scope))
                .findFirst()
                .ifPresent(scope -> {
                    throw new IllegalStateException(String.format(
                            "Access control requires the OAuth2 scope '%s', which '%s.oauth2' does not declare. A "
                                    + "security requirement naming an undeclared scope is a dangling reference. Declare "
                                    + "it under the grant flow's scopes, or stop requiring it.",
                            scope, OasProperties.OAS_PROPERTY
                    ));
                });
    }

    private Set<String> declaredScopes() {
        Set<String> declared = new LinkedHashSet<>();
        declaredScopesOf(OasProperties.OAuth2::clientCredentials, declared);
        declaredScopesOf(OasProperties.OAuth2::authorizationCodeWithPkce, declared);
        return declared;
    }

    private void declaredScopesOf(Function<OasProperties.OAuth2, OasProperties.OAuth2GrantFlow> grantFlowAccessor,
                                  Set<String> into) {
        grantFlow(grantFlowAccessor).ifPresent(grantFlow -> CollectionUtils.emptyIfNull(grantFlow.scopes())
                .forEach(scope -> into.add(scope.name())));
    }

    /**
     * The schemes an operation's scopes hang off: the ones it already names, or the document's own when it names
     * none. Access control supplies the scopes, the OpenAPI configuration supplies the flows.
     */
    private List<String> schemeNamesFor(Operation operation) {
        if (CollectionUtils.isNotEmpty(operation.getSecurity())) {
            return operation.getSecurity().stream().flatMap(requirement -> requirement.keySet().stream()).distinct().toList();
        }
        return List.of(
                        grantFlow(OasProperties.OAuth2::clientCredentials),
                        grantFlow(OasProperties.OAuth2::authorizationCodeWithPkce)
                ).stream()
                .flatMap(Optional::stream)
                .map(OasProperties.OAuth2GrantFlow::name)
                .toList();
    }

    private Optional<OasProperties.OAuth2GrantFlow> grantFlow(
            Function<OasProperties.OAuth2, OasProperties.OAuth2GrantFlow> grantFlowAccessor) {
        if (oasProperties == null || oasProperties.oauth2() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(grantFlowAccessor.apply(oasProperties.oauth2()))
                .filter(grantFlow -> StringUtils.isNotBlank(grantFlow.name()));
    }

    private void addForbidden(Operation operation) {
        if (operation.getResponses() == null || operation.getResponses().containsKey(FORBIDDEN)) {
            return;
        }
        String exampleName = ErrorExamplesCustomizer.CODES_TO_EXAMPLE_NAME.get(HttpStatusCodes.SC_403_FORBIDDEN);
        operation.getResponses().addApiResponse(FORBIDDEN, new ApiResponse()
                .description(HttpStatusCodes.SC_403_FORBIDDEN.getDescription())
                .content(new Content().addMediaType(
                        JsonApiMediaType.MEDIA_TYPE,
                        new MediaType()
                                .schema(new Schema<>().$ref(OasSchemaNamesUtil.errorsDocSchemaName()))
                                .examples(exampleName == null
                                        ? null
                                        : Map.of(exampleName, new Example().$ref("#/components/examples/" + exampleName)))
                )));
    }

    /**
     * Every access control requirement carries a description, written to explain a denial to whoever hits one. It
     * is the same audience that reads the operation, so it is appended there - including for requirements OpenAPI
     * has no field for, such as entitlements and policies.
     */
    private void describeRequirements(Operation operation,
                                      AccessControlModel accessControl) {
        List<String> reasons = new ArrayList<>();
        if (accessControl.getRequiredScopes() != null) {
            reasons.add(accessControl.getRequiredScopes().getDescription());
        }
        if (accessControl.getRequiredEntitlements() != null) {
            reasons.add(accessControl.getRequiredEntitlements().getDescription());
        }
        if (accessControl.getRequiredPolicy() != null) {
            reasons.add(accessControl.getRequiredPolicy().getDescription());
        }
        String note = reasons.stream().filter(StringUtils::isNotBlank).distinct()
                .reduce((one, other) -> one + "; " + other)
                .orElse(null);
        if (note == null) {
            return;
        }
        operation.setDescription(StringUtils.isBlank(operation.getDescription())
                ? "Access requires: " + note + "."
                : operation.getDescription() + " Access requires: " + note + ".");
    }

}
