package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import lombok.Getter;
import pro.api4.jsonapi4j.JsonApi4j;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.RegisteredResource;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.http.HttpStatusCodes;
import pro.api4.jsonapi4j.operation.*;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.plugin.oas.config.DiagnosticsMode;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;
import pro.api4.jsonapi4j.plugin.oas.diagnostics.OasDiagnostics;
import pro.api4.jsonapi4j.operation.validation.ValidationProperties;
import pro.api4.jsonapi4j.plugin.PluginRegistry;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasLinkageMetaUtil;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasOperationInfoUtil;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasResourceInfoUtil;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasResourceTypes;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasSecuritySchemes;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasSchemaNamesUtil;
import pro.api4.jsonapi4j.plugin.oas.domain.model.OasResourceInfoModel;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo;
import pro.api4.jsonapi4j.plugin.oas.operation.model.NotApplicable;
import pro.api4.jsonapi4j.plugin.oas.operation.model.OasOperationInfoModel;
import pro.api4.jsonapi4j.plugin.oas.operation.model.PaginationStyle;
import pro.api4.jsonapi4j.request.CursorAwareRequest;
import pro.api4.jsonapi4j.request.FiltersAwareRequest;
import pro.api4.jsonapi4j.request.LimitOffsetAwareRequest;
import pro.api4.jsonapi4j.request.SortAwareRequest;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static pro.api4.jsonapi4j.plugin.oas.OasOperationExtensionProperties.JSONAPI_AVAILABLE_RELATIONSHIPS;
import static pro.api4.jsonapi4j.plugin.oas.OasOperationExtensions.X_OPERATION_PROPERTIES;
import static pro.api4.jsonapi4j.plugin.oas.customizer.util.OasOperationInfoUtil.resolveRelationshipOperationPath;
import static pro.api4.jsonapi4j.plugin.oas.customizer.util.OasOperationInfoUtil.resolveResourceOperationPath;
import static pro.api4.jsonapi4j.plugin.oas.customizer.util.SchemaGeneratorUtil.getSchemaName;


@Slf4j
@Getter
public class JsonApiOperationsCustomizer implements OasCustomizer {

    private final String rootPath;
    private final DomainRegistry domainRegistry;
    private final OperationsRegistry operationsRegistry;
    private final OasProperties oasProperties;
    private final ValidationProperties validationProperties;
    private final PluginRegistry pluginRegistry;

    public JsonApiOperationsCustomizer(JsonApi4j jsonApi4j) {
        this.rootPath = jsonApi4j.getProperties().rootPath();
        this.domainRegistry = jsonApi4j.getDomainRegistry();
        this.operationsRegistry = jsonApi4j.getOperationsRegistry();
        this.oasProperties = jsonApi4j.getPluginRegistry().configOf(OasProperties.class).orElse(null);
        this.validationProperties = jsonApi4j.getProperties().validation();
        this.pluginRegistry = jsonApi4j.getPluginRegistry();
    }

    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            openApi.setPaths(new Paths());
        }
        createAndRegisterOperations(openApi.getPaths());
    }

    private void createAndRegisterOperations(Paths paths) {
        OasResourceTypes.resourceTypesWithOperationsExcludingMeta(domainRegistry, operationsRegistry)
                .sorted()
                .forEach(resourceType -> {
                    createAndRegisterResourceOperations(paths, resourceType);
                    operationsRegistry.getRelationshipNamesWithAnyOperationConfigured(resourceType)
                            .stream()
                            .sorted()
                            .forEach(relationshipName -> {
                                createAndRegisterRelationshipOperations(paths, resourceType, relationshipName);
                            });
                });
    }

    private void createAndRegisterResourceOperations(Paths paths,
                                                     ResourceType resourceType) {
        OperationType.getResourceOperationTypes().stream()
                .filter(operationType -> operationsRegistry.isResourceOperationConfigured(resourceType, operationType))
                .forEach(operationType -> {
                    Operation operation = createResourceOperation(
                            resourceType,
                            operationType
                    );
                    String operationPath = resolveResourceOperationPath(
                            rootPath,
                            operationType,
                            resourceType
                    );
                    registerOperation(
                            paths,
                            operationPath,
                            operationType.getMethod(),
                            operation
                    );
                });
    }

    private void createAndRegisterRelationshipOperations(Paths paths,
                                                         ResourceType resourceType,
                                                         RelationshipName relationshipName) {
        OperationType.getAllRelationshipOperationTypes()
                .stream()
                .filter(operationType -> operationsRegistry.isRelationshipOperationConfigured(resourceType, relationshipName, operationType))
                .forEach(operationType -> {
                    Operation operation = createRelationshipOperation(
                            resourceType,
                            relationshipName,
                            operationType
                    );
                    String operationPath = resolveRelationshipOperationPath(
                            rootPath,
                            operationType,
                            resourceType,
                            relationshipName
                    );
                    registerOperation(
                            paths,
                            operationPath,
                            operationType.getMethod(),
                            operation
                    );
                });
    }

    private void registerOperation(Paths paths,
                                   String operationPath,
                                   OperationType.Method method,
                                   Operation operation) {
        PathItem pathItem = paths.get(operationPath);
        if (pathItem == null) {
            pathItem = new PathItem();
            paths.addPathItem(operationPath, pathItem);
        }
        if (OperationType.Method.GET == method) {
            pathItem.setGet(operation);
        } else if (OperationType.Method.POST == method) {
            pathItem.setPost(operation);
        } else if (OperationType.Method.PATCH == method) {
            pathItem.setPatch(operation);
        } else if (OperationType.Method.DELETE == method) {
            pathItem.setDelete(operation);
        } else {
            throw new IllegalStateException("Unsupported JSON:API method: " + method.name());
        }
    }

    private Operation createResourceOperation(ResourceType resourceType,
                                              OperationType operationType) {
        RegisteredOperation<?> registeredOperation = operationsRegistry.getRegisteredResourceOperation(resourceType, operationType, false);
        return createOperation(registeredOperation);
    }

    private Operation createRelationshipOperation(ResourceType resourceType,
                                                  RelationshipName relationshipName,
                                                  OperationType operationType) {
        RegisteredOperation<?> registeredOperation = operationsRegistry.getRegisteredRelationshipOperation(resourceType, relationshipName, operationType, false);
        return createOperation(registeredOperation);
    }

    private Operation createOperation(RegisteredOperation<?> registeredOperation) {
        if (registeredOperation == null) {
            return null;
        }
        return createOperation(registeredOperation.getOperationMeta());
    }

    private Operation createOperation(OperationMeta operationMeta) {
        Object oasOperationInfoObject = MapUtils.emptyIfNull(operationMeta.getPluginInfo()).get(JsonApiOasPlugin.NAME);
        OasOperationInfoModel oasOperationInfo = null;
        if (oasOperationInfoObject instanceof OasOperationInfoModel ooim) {
            oasOperationInfo = ooim;
        }

        ResourceType resourceType = operationMeta.getResourceType();
        OperationType operationType = operationMeta.getOperationType();

        RegisteredResource<?> registeredResource = domainRegistry.getResource(resourceType);
        Object oasResourceInfoObject = MapUtils.emptyIfNull(registeredResource.getPluginInfo()).get(JsonApiOasPlugin.NAME);
        OasOperationInfoUtil.Info extraOasOperationInfo = OasOperationInfoUtil.resolveOperationOasInfo(
                operationMeta,
                getResourceCustomNameSingle(oasResourceInfoObject)
        );
        List<String> supportedIncludes = getSupportedIncludes(extraOasOperationInfo);

        Operation oasOperation = new Operation();
        // operationId — what client generators name the method after, so every operation gets one
        oasOperation.setOperationId(extraOasOperationInfo.getUrlCompatibleUniqueName());
        // summary
        oasOperation.setSummary(
                overrideIfNotBlank(
                        oasOperationInfo != null ? oasOperationInfo.getSummary() : null,
                        extraOasOperationInfo.getSummary()
                )
        );
        // description
        oasOperation.setDescription(
                overrideIfNotBlank(
                        oasOperationInfo != null ? oasOperationInfo.getDescription() : null,
                        extraOasOperationInfo.getDescription()
                )
        );
        // tags
        oasOperation.setTags(Collections.singletonList(extraOasOperationInfo.getOperationTag()));
        // deprecation
        if (isDeprecated(oasOperationInfo, resourceType)) {
            oasOperation.setDeprecated(true);
        }
        // parameters
        oasOperation.setParameters(
                generateParameters(
                        oasOperationInfo,
                        supportedIncludes,
                        extraOasOperationInfo
                )
        );
        // request body
        String payloadSchemaName = resolveRequestBodySchemaName(oasOperationInfo, operationMeta);
        if (StringUtils.isNotBlank(payloadSchemaName)) {
            oasOperation.setRequestBody(
                    new RequestBody()
                            .required(true)
                            .content(new Content().addMediaType(
                                    JsonApiMediaType.MEDIA_TYPE,
                                    new MediaType().schema(new Schema().$ref(payloadSchemaName))
                            ))
            );
        }
        // security requirements — resolved before the responses, which document 401 only where there is a scheme to fail
        List<SecurityRequirement> securityRequirements = oasOperationInfo != null
                ? generateSecurityRequirements(oasOperationInfo.getSecurityConfig(), oasOperation.getOperationId())
                : null;
        if (CollectionUtils.isNotEmpty(securityRequirements)) {
            oasOperation.setSecurity(securityRequirements);
        }
        // an operation with no security of its own still inherits the document's, so 401 belongs on it either way
        boolean secured = CollectionUtils.isNotEmpty(securityRequirements) || isDocumentSecured();
        // responses
        String happyPathResponseDocSchemaName = OasSchemaNamesUtil.happyPathResponseDocSchemaName(
                resourceType,
                operationType
        );
        oasOperation.setResponses(
                generateResponses(
                        String.valueOf(operationType.getHttpStatus()),
                        happyPathResponseDocSchemaName,
                        resolveSupportedHttpErrorCodes(extraOasOperationInfo, secured)
                )
        );
        // oas extensions
        addOperationExtensions(oasOperation, supportedIncludes);

        return oasOperation;
    }

    /**
     * Deprecation only widens: a resource on its way out takes every one of its operations with it, and an operation
     * may retire on its own. Nothing opts back in, which is both the honest reading - an operation cannot outlive the
     * resource it acts on - and what keeps a {@code boolean} enough, since it never has to mean "not stated".
     */
    private boolean isDeprecated(OasOperationInfoModel oasOperationInfo,
                                 ResourceType resourceType) {
        boolean deprecatedResource = OasResourceInfoUtil.resourceInfo(domainRegistry, resourceType)
                .map(OasResourceInfoModel::isDeprecated)
                .orElse(false);
        return deprecatedResource || (oasOperationInfo != null && oasOperationInfo.isDeprecated());
    }

    private String overrideIfNotBlank(String override,
                                      String generated) {
        return StringUtils.isNotBlank(override) ? override : generated;
    }

    /**
     * Resolves the schema documenting the operation's request body: the type declared via
     * {@link OasOperationInfo#payloadType()} when there is one, and otherwise the body JSON:API prescribes for this
     * operation. Returns {@code null} for operations that carry no body.
     */
    private String resolveRequestBodySchemaName(OasOperationInfoModel oasOperationInfo,
                                                OperationMeta operationMeta) {
        if (oasOperationInfo != null
                && oasOperationInfo.getPayloadType() != null
                && oasOperationInfo.getPayloadType() != NotApplicable.class) {
            return getSchemaName(oasOperationInfo.getPayloadType());
        }
        ResourceType resourceType = operationMeta.getResourceType();
        RelationshipName relationshipName = operationMeta.getRelationshipName();
        return OasSchemaNamesUtil.requestBodyDocSchemaName(
                resourceType,
                relationshipName,
                operationMeta.getOperationType(),
                OasLinkageMetaUtil.resolveLinkageMetaType(domainRegistry, resourceType, relationshipName) != null
        );
    }

    private String getResourceCustomNameSingle(Object oasResourceInfoObject) {
        if (oasResourceInfoObject instanceof OasResourceInfoModel oasResourceInfo) {
            if (StringUtils.isNotBlank(oasResourceInfo.getResourceNameSingle())) {
                return oasResourceInfo.getResourceNameSingle();
            }
        }
        return null;
    }

    private List<String> getSupportedIncludes(OasOperationInfoUtil.Info operationOasInfo) {
        if (operationOasInfo.isIncludesSupported()) {
            if (operationOasInfo.getRelationshipName() != null) {
                return Collections.singletonList(operationOasInfo.getRelationshipName().getName());
            } else {
                return operationsRegistry.getRelationshipNamesWithReadOperationConfigured(operationOasInfo.getResourceType())
                        .stream()
                        .sorted()
                        .map(RelationshipName::getName)
                        .toList();
            }
        }
        return Collections.emptyList();
    }

    private List<Parameter> generateParameters(OasOperationInfoModel oasOperationInfo,
                                               List<String> supportedIncludes,
                                               OasOperationInfoUtil.Info extraOperationInfo) {
        Map<String, Parameter> parameters = new LinkedHashMap<>();
        generateJsonApiParameters(supportedIncludes, extraOperationInfo, oasOperationInfo)
                .forEach(parameter -> parameters.put(parameter.getName(), parameter));
        customParametersOf(oasOperationInfo).forEach(custom -> {
            Parameter generated = parameters.get(custom.getName());
            if (generated == null) {
                parameters.put(custom.getName(), createCustomParam(custom));
            } else {
                describe(generated, custom.getDescription(), custom.getExample());
            }
        });
        return List.copyOf(parameters.values());
    }

    private List<OasOperationInfoModel.Parameter> customParametersOf(OasOperationInfoModel oasOperationInfo) {
        return oasOperationInfo == null ? List.of() : emptyIfNull(oasOperationInfo.getParameters()).stream().toList();
    }

    /**
     * A declaration naming a parameter the framework already generated contributes its prose and nothing else. The
     * generated schema carries constraints read from the running configuration - {@code maxLength} on a resource id,
     * {@code maxItems} on {@code include} - and replacing it wholesale drops them without saying so.
     */
    private void describe(Parameter parameter,
                          String description,
                          String example) {
        if (StringUtils.isNotBlank(description)) {
            parameter.setDescription(description);
        }
        if (StringUtils.isNotBlank(example)) {
            if (parameter.getSchema() instanceof ArraySchema arraySchema && arraySchema.getItems() != null) {
                arraySchema.getItems().setExample(example);
            } else {
                parameter.setExample(example);
            }
        }
    }

    private Set<HttpStatusCodes> resolveSupportedHttpErrorCodes(OasOperationInfoUtil.Info extraOperationInfo,
                                                                boolean secured) {
        EnumSet<HttpStatusCodes> codes = EnumSet.copyOf(extraOperationInfo.getSupportedHttpErrorCodes());
        if (secured) {
            codes.add(HttpStatusCodes.SC_401_UNAUTHORIZED);
        }
        return codes;
    }

    private boolean isDocumentSecured() {
        return resolveSecuritySchemeName(OasProperties.OAuth2::clientCredentials) != null
                || resolveSecuritySchemeName(OasProperties.OAuth2::authorizationCodeWithPkce) != null;
    }

    private ApiResponses generateResponses(String status,
                                           String happyPathResponseDocSchemaName,
                                           Set<HttpStatusCodes> supportedHttpErrorCodes) {
        ApiResponses responses = new ApiResponses();
        responses.addApiResponse(status, generateHappyPathResponse(status, happyPathResponseDocSchemaName));
        responses.putAll(generateErrorResponses(supportedHttpErrorCodes));
        return responses;
    }

    private ApiResponse generateHappyPathResponse(String status,
                                                  String responseDocSchemaName) {
        return generateResponse(describeHttpStatus(status), responseDocSchemaName, null);
    }

    private String describeHttpStatus(String status) {
        return HttpStatusCodes.fromCode(Integer.parseInt(status))
                .map(HttpStatusCodes::getDescription)
                .orElse("Happy path scenario");
    }

    private Map<String, ApiResponse> generateErrorResponses(Set<HttpStatusCodes> supportedHttpErrorCodes) {
        Map<String, ApiResponse> errorResponses = new LinkedHashMap<>();
        supportedHttpErrorCodes.forEach(code -> errorResponses.put(
                String.valueOf(code.getCode()),
                generateResponse(
                        code.getDescription(),
                        OasSchemaNamesUtil.errorsDocSchemaName(),
                        ErrorExamplesCustomizer.CODES_TO_EXAMPLE_NAME.get(code)
                )
        ));
        return errorResponses;
    }

    private ApiResponse generateResponse(String description,
                                         String responseDocSchemaName,
                                         String exampleName) {
        ApiResponse response = new ApiResponse();
        if (StringUtils.isNotBlank(description)) {
            response.setDescription(description);
        }
        if (StringUtils.isNotBlank(responseDocSchemaName)) {
            response.setContent(
                    new Content().addMediaType(
                            JsonApiMediaType.MEDIA_TYPE,
                            new MediaType()
                                    .schema(new Schema().$ref(responseDocSchemaName))
                                    .examples(exampleName == null ? null : Map.of(exampleName, new Example().$ref("#/components/examples/" + exampleName)))
                    )
            );
        }
        return response;
    }

    private List<SecurityRequirement> generateSecurityRequirements(OasOperationInfoModel.SecurityConfig securityConfig,
                                                                   String operationId) {
        if (securityConfig == null) {
            return null;
        }
        List<String> schemeNames = new ArrayList<>();
        if (securityConfig.isClientCredentialsSupported()) {
            schemeNames.add(resolveSecuritySchemeName(OasProperties.OAuth2::clientCredentials));
        }
        if (securityConfig.isPkceSupported()) {
            schemeNames.add(resolveSecuritySchemeName(OasProperties.OAuth2::authorizationCodeWithPkce));
        }
        schemeNames.removeIf(Objects::isNull);
        return securityRequirementsFor(schemeNames, emptyIfNull(securityConfig.getRequiredScopes()).stream().toList(), operationId);
    }

    /**
     * Hangs the required scopes off each supported flow that declares them. A flow is left out rather than made to
     * name a scope its own scheme does not declare - OpenAPI reads an operation's scopes against the scheme it names,
     * so such an entry is a reference to nothing, and Swagger UI's authorize dialog cannot offer it.
     */
    private List<SecurityRequirement> securityRequirementsFor(List<String> schemeNames,
                                                              List<String> requiredScopes,
                                                              String operationId) {
        Map<String, Set<String>> declaredScopesByScheme = OasSecuritySchemes.declaredScopesByScheme(oasProperties);
        List<String> carriers = schemeNames.stream()
                .filter(schemeName -> OasSecuritySchemes.carries(declaredScopesByScheme, schemeName, requiredScopes))
                .toList();
        if (carriers.isEmpty() && !schemeNames.isEmpty()) {
            OasDiagnostics.report(
                    diagnostics(),
                    "'%s' declares the required scopes %s, but none of the grant flows it supports declares them "
                            + "(%s), so the requirement is not published. Declare them under the flow that grants "
                            + "them, or drop them from the operation.",
                    operationId,
                    String.join(", ", requiredScopes),
                    OasSecuritySchemes.describeMissingScopes(declaredScopesByScheme, schemeNames, requiredScopes)
            );
        }
        return carriers.stream()
                .map(schemeName -> new SecurityRequirement().addList(schemeName, requiredScopes))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private DiagnosticsMode diagnostics() {
        return oasProperties == null ? DiagnosticsMode.WARN : oasProperties.diagnostics();
    }

    private String resolveSecuritySchemeName(Function<OasProperties.OAuth2, OasProperties.OAuth2GrantFlow> grantFlowAccessor) {
        return OasSecuritySchemes.schemeName(oasProperties, grantFlowAccessor);
    }

    /**
     * JSON:API passes a multi-valued parameter as one comma-separated value, so the document has to say so: left
     * unsaid, OpenAPI's default for a query parameter is a repeated key ({@code ?include=a&include=b}), and that is
     * what a generated client would send. The request layer accepts either form, so this corrects what the document
     * promises rather than what the server takes.
     */
    private void commaSeparated(Parameter parameter) {
        parameter.setStyle(Parameter.StyleEnum.FORM);
        parameter.setExplode(false);
    }

    private Parameter createCustomParam(OasOperationInfoModel.Parameter custom) {
        Parameter parameter = new Parameter();
        parameter.setName(custom.getName());
        parameter.setDescription(custom.getDescription());
        parameter.setRequired(custom.isRequired());
        parameter.setIn(custom.getIn().getName());
        if (custom.isArray()) {
            parameter.setSchema(new ArraySchema().items(new Schema<>().type(custom.getType().getType()).example(custom.getExample())));
            commaSeparated(parameter);
        } else {
            parameter.setExample(custom.getExample());
            parameter.setSchema(new Schema<>().type(custom.getType().getType()));
        }
        return parameter;
    }

    private List<Parameter> generateJsonApiParameters(List<String> availableIncludes,
                                                      OasOperationInfoUtil.Info extraOperationInfo,
                                                      OasOperationInfoModel oasOperationInfo) {
        List<Parameter> params = new ArrayList<>();
        // the path parameter first: it is part of the address, while everything after it only tunes the request
        if (OperationType.getExistingResourceAwareOperations().contains(extraOperationInfo.getOperationType())) {
            params.add(createDefaultIdPathParam(extraOperationInfo.getResourceType()));
        }
        if (CollectionUtils.isNotEmpty(availableIncludes)) {
            params.add(createIncludeParam(availableIncludes));
        }
        if (extraOperationInfo.isPaginationSupported()) {
            for (PaginationStyle style : paginationStylesOf(oasOperationInfo)) {
                params.addAll(createPaginationParams(style));
            }
        }
        createSortParam(sortableFieldsOf(oasOperationInfo)).ifPresent(params::add);
        params.addAll(createFilterParams(filtersOf(oasOperationInfo)));
        return params;
    }

    /**
     * Publishes {@code filter[...]} only for the dimensions the operation declared, for the same reason {@code sort}
     * is published from {@link OasOperationInfo#sortableFields()}: the framework parses every {@code filter[...]} it
     * is given, but acting on one is the operation's business.
     */
    private List<Parameter> createFilterParams(List<OasOperationInfoModel.Filter> filters) {
        return filters.stream().map(this::createFilterParam).toList();
    }

    private Parameter createFilterParam(OasOperationInfoModel.Filter filter) {
        Parameter filterParam = new Parameter();
        filterParam.setName(FiltersAwareRequest.getFilterParam(filter.getName()));
        filterParam.setIn("query");
        filterParam.setRequired(false);
        filterParam.setDescription(StringUtils.isNotBlank(filter.getDescription())
                ? filter.getDescription()
                : String.format("Filters by '%s'. Optional", filter.getName()));

        Schema<?> itemSchema = new Schema<>().type(filter.getType().getType());
        if (StringUtils.isNotBlank(filter.getExample())) {
            itemSchema.setExample(filter.getExample());
        }
        ArraySchema schema = new ArraySchema().items(itemSchema);
        if (validationProperties != null) {
            schema.setMaxItems(validationProperties.maxElementsInFilterParam());
        }
        filterParam.setSchema(schema);
        commaSeparated(filterParam);
        return filterParam;
    }

    private List<OasOperationInfoModel.Filter> filtersOf(OasOperationInfoModel oasOperationInfo) {
        return oasOperationInfo == null ? List.of() : emptyIfNull(oasOperationInfo.getFilters()).stream().toList();
    }

    private List<String> sortableFieldsOf(OasOperationInfoModel oasOperationInfo) {
        return oasOperationInfo == null ? List.of() : emptyIfNull(oasOperationInfo.getSortableFields()).stream().toList();
    }

    private Set<PaginationStyle> paginationStylesOf(OasOperationInfoModel oasOperationInfo) {
        return oasOperationInfo == null || CollectionUtils.isEmpty(oasOperationInfo.getPaginationStyles())
                ? EnumSet.of(PaginationStyle.CURSOR)
                : oasOperationInfo.getPaginationStyles();
    }

    private List<Parameter> createPaginationParams(PaginationStyle style) {
        return style == PaginationStyle.CURSOR
                ? List.of(createCursorParam())
                : List.of(createLimitParam(), createOffsetParam());
    }

    private Parameter createLimitParam() {
        Parameter limitParam = new Parameter();
        limitParam.setName(LimitOffsetAwareRequest.LIMIT_PARAM);
        limitParam.setIn("query");
        limitParam.setRequired(false);
        limitParam.setDescription("Maximum number of items to return in a single page. Optional");
        IntegerSchema schema = new IntegerSchema();
        schema.setFormat("int64");
        schema.setMinimum(BigDecimal.ONE);
        schema.setDefault(LimitOffsetAwareRequest.DEFAULT_LIMIT);
        if (validationProperties != null) {
            schema.setMaximum(BigDecimal.valueOf(validationProperties.limitMaxValue()));
        }
        limitParam.setSchema(schema);
        return limitParam;
    }

    private Parameter createOffsetParam() {
        Parameter offsetParam = new Parameter();
        offsetParam.setName(LimitOffsetAwareRequest.OFFSET_PARAM);
        offsetParam.setIn("query");
        offsetParam.setRequired(false);
        offsetParam.setDescription("Number of items to skip before collecting the page. Optional");
        IntegerSchema schema = new IntegerSchema();
        schema.setFormat("int64");
        schema.setMinimum(BigDecimal.ZERO);
        schema.setDefault(LimitOffsetAwareRequest.DEFAULT_OFFSET);
        offsetParam.setSchema(schema);
        return offsetParam;
    }

    /**
     * Publishes {@code sort} only when the operation declared what it can sort by — the framework parses the
     * parameter for every request, but acting on it is the operation's business.
     */
    private Optional<Parameter> createSortParam(List<String> sortableFields) {
        if (CollectionUtils.isEmpty(sortableFields)) {
            return Optional.empty();
        }
        List<String> allowedValues = sortableFields.stream()
                .flatMap(field -> Stream.of(field, "-" + field))
                .toList();

        Parameter sortParam = new Parameter();
        sortParam.setName(SortAwareRequest.SORT_PARAM);
        sortParam.setIn("query");
        sortParam.setRequired(false);
        sortParam.setDescription("Sort order. Prefix a field with '-' for descending, e.g. '-"
                + sortableFields.get(0) + "'. Optional");

        StringSchema itemSchema = new StringSchema();
        allowedValues.forEach(itemSchema::addEnumItem);
        ArraySchema schema = new ArraySchema().items(itemSchema);
        if (validationProperties != null) {
            schema.setMaxItems(validationProperties.maxElementsInSortByParam());
        }
        sortParam.setSchema(schema);
        sortParam.setExample(allowedValues.get(0));
        commaSeparated(sortParam);
        return Optional.of(sortParam);
    }

    private Parameter createIncludeParam(List<String> availableRelationships) {
        Parameter includeQueryParam = new Parameter();
        includeQueryParam.setName(IncludeAwareRequest.INCLUDE_PARAM);
        includeQueryParam.setIn("query");
        includeQueryParam.setRequired(false);

        String example = availableRelationships.stream().findFirst().orElse(null);

        String description = "Allows clients to customize which related resources should be returned in compound docs"
                + ". Available relationships: " + String.join(", ", availableRelationships)
                + ". Dotted paths reach further levels (e.g. '" + example + ".<relationship>'); this document describes"
                + " the first level only";

        includeQueryParam.setDescription(description);
        ArraySchema includeSchema = new ArraySchema().items(new StringSchema().example(example));
        if (validationProperties != null) {
            includeSchema.setMaxItems(validationProperties.maxElementsInIncludeParam());
        }
        includeQueryParam.setSchema(includeSchema);
        includeQueryParam.setExample(example);
        commaSeparated(includeQueryParam);

        return includeQueryParam;
    }

    private Parameter createCursorParam() {
        Parameter cursorParam = new Parameter();
        cursorParam.setName(CursorAwareRequest.CURSOR_PARAM);
        cursorParam.setIn("query");
        cursorParam.setRequired(false);
        cursorParam.setDescription("Server-generated cursor value pointing to a certain page of items. Optional, targets first page if not specified");
        cursorParam.setSchema(new StringSchema());
        return cursorParam;
    }

    /**
     * The identifier's prose comes from the resource, not from the operation: every operation acting on one instance
     * of a resource asks for the same id, so declaring it per operation only gave them room to disagree.
     */
    private Parameter createDefaultIdPathParam(ResourceType resourceType) {
        Optional<OasResourceInfoModel> resourceInfo = OasResourceInfoUtil.resourceInfo(domainRegistry, resourceType);

        Parameter idParam = new Parameter();
        idParam.setName("id");
        idParam.setIn("path");
        idParam.setDescription(OasResourceInfoUtil.resourceIdDescription(resourceInfo) + ". Required");
        idParam.setExample(OasResourceInfoUtil.resourceIdExample(resourceInfo));
        StringSchema idSchema = new StringSchema();
        if (validationProperties != null) {
            idSchema.setMaxLength(validationProperties.resourceIdMaxLength());
        }
        idParam.setSchema(idSchema);
        return idParam;
    }

    private void addOperationExtensions(Operation operation,
                                        List<String> supportedIncludes) {
        if (CollectionUtils.isEmpty(supportedIncludes)) {
            return;
        }
        if (operation.getExtensions() == null) {
            operation.setExtensions(new LinkedHashMap<>());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> operationExtensions = (Map<String, Object>) operation.getExtensions()
                .computeIfAbsent(X_OPERATION_PROPERTIES, key -> new LinkedHashMap<String, Object>());
        operationExtensions.put(JSONAPI_AVAILABLE_RELATIONSHIPS, supportedIncludes);
    }

}
