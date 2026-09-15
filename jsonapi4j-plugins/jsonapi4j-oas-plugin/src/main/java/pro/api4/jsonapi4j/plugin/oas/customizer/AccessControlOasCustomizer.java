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
import pro.api4.jsonapi4j.plugin.ac.model.outbound.OutboundAccessControlForCustomClass;
import pro.api4.jsonapi4j.plugin.ac.model.ScopesGroupModel;
import pro.api4.jsonapi4j.plugin.oas.config.DiagnosticsMode;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;
import pro.api4.jsonapi4j.plugin.oas.diagnostics.OasDiagnostics;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasResourceInfoUtil;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasResourceTypes;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasSecuritySchemes;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.SchemaGeneratorUtil;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasSchemaNamesUtil;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
        describeGuardedAttributes(openApi);
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

    /**
     * Says which attributes access control can withhold, and what it wants in exchange.
     * <p>
     * An anonymized attribute is simply absent from the response - which the response schema already allows, since it
     * requires nothing - but absence alone tells a client nothing about why. Naming the requirement on the attribute
     * turns "sometimes this field is missing" into something a caller can act on: ask for the scope.
     */
    private void describeGuardedAttributes(OpenAPI openApi) {
        if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
            return;
        }
        OasResourceTypes.registeredResourcesExcludingMeta(domainRegistry).forEach(registeredResource -> {
            ResourceType resourceType = registeredResource.getResourceType();
            Class<?> attributesType = OasResourceInfoUtil.attributesType(domainRegistry, resourceType);
            Schema<?> attributesSchema = openApi.getComponents().getSchemas()
                    .get(OasSchemaNamesUtil.attributesSchemaName(resourceType));
            if (attributesType == null || attributesSchema == null) {
                return;
            }
            OutboundAccessControlForCustomClass accessControl = OutboundAccessControlForCustomClass
                    .forClass(attributesType);
            if (accessControl == null) {
                return;
            }
            describeGuardedClass(attributesSchema, accessControl.getClassLevel());
            emptyIfNull(accessControl.getFieldLevel())
                    .forEach((fieldName, model) -> describeGuardedField(attributesSchema, fieldName, model));
        });
    }

    private void describeGuardedClass(Schema<?> attributesSchema,
                                      AccessControlModel classLevel) {
        requirementPhrase(classLevel).ifPresent(phrase -> SchemaGeneratorUtil.appendDescription(
                attributesSchema,
                String.format("Every attribute is withheld from a caller who does not qualify - requires %s.", phrase)
        ));
    }

    private void describeGuardedField(Schema<?> attributesSchema,
                                      String fieldName,
                                      AccessControlModel fieldLevel) {
        if (attributesSchema.getProperties() == null) {
            return;
        }
        Schema<?> property = (Schema<?>) attributesSchema.getProperties().get(fieldName);
        if (property == null) {
            return;
        }
        requirementPhrase(fieldLevel).ifPresent(phrase -> SchemaGeneratorUtil.appendDescription(
                property,
                String.format("Absent from the response unless the caller qualifies - requires %s.", phrase)
        ));
    }

    /**
     * What the requirement asks for, in the words the application chose where it supplied any. A requirement with no
     * description falls back to naming its scopes, which is the part a caller can actually do something about.
     */
    private Optional<String> requirementPhrase(AccessControlModel accessControl) {
        if (accessControl == null || accessControl.declaresNoRequirements()) {
            return Optional.empty();
        }
        List<String> reasons = new ArrayList<>();
        if (accessControl.getRequiredScopes() != null) {
            reasons.add(StringUtils.isNotBlank(accessControl.getRequiredScopes().getDescription())
                    ? accessControl.getRequiredScopes().getDescription()
                    : "the scopes " + String.join(", ", declaredScopeNames(accessControl.getRequiredScopes())));
        }
        if (accessControl.getRequiredEntitlements() != null) {
            reasons.add(StringUtils.isNotBlank(accessControl.getRequiredEntitlements().getDescription())
                    ? accessControl.getRequiredEntitlements().getDescription()
                    : "an entitlement");
        }
        if (accessControl.getRequiredPolicy() != null) {
            reasons.add(StringUtils.isNotBlank(accessControl.getRequiredPolicy().getDescription())
                    ? accessControl.getRequiredPolicy().getDescription()
                    : "an access policy");
        }
        if (accessControl.getRequiredOwnership() != null) {
            reasons.add("ownership of the resource");
        }
        if (accessControl.getAuthenticated() != null
                && accessControl.getAuthenticated().getAuthenticated() == Authenticated.AUTHENTICATED) {
            reasons.add("an authenticated caller");
        }
        return reasons.stream().filter(StringUtils::isNotBlank).distinct()
                .reduce((one, other) -> one + " and " + other);
    }

    private Set<String> declaredScopeNames(AccessControlScopesModel requiredScopes) {
        return CollectionUtils.emptyIfNull(requiredScopes.getGroups()).stream()
                .flatMap(group -> CollectionUtils.emptyIfNull(group.getScopes()).stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
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
            OasDiagnostics.report(
                    diagnostics(),
                    "A scope requirement on '%s' expands into more alternatives than a document should state, so it "
                            + "is not published as a security requirement. Restructure it into fewer clauses, or "
                            + "express it as an access policy.",
                    operation.getOperationId()
            );
            return Optional.empty();
        }
        alternatives.forEach(this::requireDeclaredScopes);

        List<String> schemeNames = schemeNamesFor(operation);
        if (schemeNames.isEmpty()) {
            OasDiagnostics.report(
                    diagnostics(),
                    "Access control requires scopes on '%s', but no OAuth2 grant flow is configured under "
                            + "'%s.oauth2' to hang them off, so the requirement is not published.",
                    operation.getOperationId(), OasProperties.OAS_PROPERTY
            );
            return Optional.empty();
        }
        Map<String, Set<String>> declaredScopesByScheme = OasSecuritySchemes.declaredScopesByScheme(oasProperties);
        List<SecurityRequirement> requirements = new ArrayList<>();
        for (String schemeName : schemeNames) {
            alternatives.stream()
                    .filter(alternative -> OasSecuritySchemes.carries(declaredScopesByScheme, schemeName, alternative))
                    .forEach(alternative -> requirements.add(
                            new SecurityRequirement().addList(schemeName, List.copyOf(alternative))));
        }
        reportUncarriedAlternatives(alternatives, schemeNames, declaredScopesByScheme, operation);
        return requirements.isEmpty() ? Optional.empty() : Optional.of(requirements);
    }

    /**
     * An alternative no configured flow can carry is left out rather than hung off a flow that would reject it. It is
     * reported because the requirement is real - access control enforces it - and the document has just gone quiet
     * about it, which is the one outcome worse than stating it awkwardly.
     */
    private void reportUncarriedAlternatives(List<Set<String>> alternatives,
                                             List<String> schemeNames,
                                             Map<String, Set<String>> declaredScopesByScheme,
                                             Operation operation) {
        alternatives.stream()
                .filter(alternative -> schemeNames.stream()
                        .noneMatch(schemeName -> OasSecuritySchemes.carries(declaredScopesByScheme, schemeName, alternative)))
                .forEach(alternative -> OasDiagnostics.report(
                        diagnostics(),
                        "Access control requires the scopes %s on '%s', but no grant flow the operation can use "
                                + "declares all of them (%s), so that alternative is not published. Declare them "
                                + "under the flow that grants them, or stop requiring them.",
                        String.join(", ", alternative),
                        operation.getOperationId(),
                        OasSecuritySchemes.describeMissingScopes(declaredScopesByScheme, schemeNames, alternative)
                ));
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
     * A scope no configured flow declares anywhere is a typo rather than a scope hung off the wrong flow, so it is
     * refused outright: nothing in the configuration could have been meant by it. A scope that is declared, but not
     * by the flow an operation would use, is the softer case {@link #reportUncarriedAlternatives} handles.
     * <p>
     * A configuration that enumerates no scopes at all has opted out of naming them, so there is nothing to check
     * against and no typo to find.
     */
    private void requireDeclaredScopes(Set<String> scopes) {
        Set<String> declared = OasSecuritySchemes.declaredScopes(oasProperties);
        if (declared.isEmpty()) {
            return;
        }
        scopes.stream()
                .filter(scope -> !declared.contains(scope))
                .findFirst()
                .ifPresent(scope -> {
                    throw OasDiagnostics.reject(
                            "Access control requires the OAuth2 scope '%s', which '%s.oauth2' does not declare. A "
                                    + "security requirement naming an undeclared scope is a dangling reference. Declare "
                                    + "it under the grant flow's scopes, or stop requiring it.",
                            scope, OasProperties.OAS_PROPERTY
                    );
                });
    }

    private DiagnosticsMode diagnostics() {
        return oasProperties == null ? DiagnosticsMode.WARN : oasProperties.diagnostics();
    }

    /**
     * The schemes an operation's scopes hang off: the ones it already names, or the document's own when it names
     * none. Access control supplies the scopes, the OpenAPI configuration supplies the flows.
     */
    private List<String> schemeNamesFor(Operation operation) {
        if (CollectionUtils.isNotEmpty(operation.getSecurity())) {
            return operation.getSecurity().stream().flatMap(requirement -> requirement.keySet().stream()).distinct().toList();
        }
        return OasSecuritySchemes.schemeNames(oasProperties);
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
