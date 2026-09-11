package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
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
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties.CustomResponseHeaderGroup;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties.ResponseHeader;
import pro.api4.jsonapi4j.operation.validation.ValidationProperties;
import pro.api4.jsonapi4j.plugin.PluginRegistry;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasIncludableTypesUtil;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasLinkageMetaUtil;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasOperationInfoUtil;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasResourceTypes;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasSchemaNamesUtil;
import pro.api4.jsonapi4j.plugin.oas.domain.model.OasResourceInfoModel;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo;
import pro.api4.jsonapi4j.plugin.oas.operation.model.NotApplicable;
import pro.api4.jsonapi4j.plugin.oas.operation.model.OasOperationInfoModel;
import pro.api4.jsonapi4j.plugin.oas.operation.model.PaginationStyle;
import pro.api4.jsonapi4j.request.CursorAwareRequest;
import pro.api4.jsonapi4j.request.LimitOffsetAwareRequest;
import pro.api4.jsonapi4j.request.SortAwareRequest;
import pro.api4.jsonapi4j.request.SparseFieldsetsAwareRequest;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
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

    /**
     * Name of the sparse fieldsets plugin. Matched as a string because plugins are peers — the OAS plugin describes
     * what another plugin contributes to the API without depending on it.
     */
    private static final String SPARSE_FIELDSETS_PLUGIN_NAME = "JsonApiSparseFieldsetsPlugin";

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
        // responses
        String happyPathResponseDocSchemaName = OasSchemaNamesUtil.happyPathResponseDocSchemaName(
                resourceType,
                operationType
        );
        oasOperation.setResponses(
                generateResponses(
                        String.valueOf(operationType.getHttpStatus()),
                        happyPathResponseDocSchemaName,
                        extraOasOperationInfo.getSupportedHttpErrorCodes()
                )
        );
        // security requirements
        if (oasOperationInfo != null) {
            List<SecurityRequirement> securityRequirements = generateSecurityRequirements(oasOperationInfo.getSecurityConfig());
            if (CollectionUtils.isNotEmpty(securityRequirements)) {
                oasOperation.setSecurity(securityRequirements);
            }
        }
        // oas extensions
        addOperationExtensions(oasOperation, supportedIncludes);

        return oasOperation;
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
        List<OasOperationInfoModel.Parameter> customParameters = oasOperationInfo != null
                ? oasOperationInfo.getParameters()
                : Collections.emptyList();
        Map<String, Parameter> parameters = new LinkedHashMap<>();
        // generate Json:Api default parameters first
        generateJsonApiParameters(supportedIncludes, extraOperationInfo, oasOperationInfo).forEach(p -> parameters.put(p.getName(), p));
        // custom parameters can override Json:Api default parameters
        generateCustomParameters(customParameters).forEach(p -> parameters.put(p.getName(), p));
        return List.copyOf(parameters.values());
    }

    private ApiResponses generateResponses(String status,
                                           String happyPathResponseDocSchemaName,
                                           Set<HttpStatusCodes> supportedHttpErrorCodes) {
        ApiResponses responses = new ApiResponses();
        responses.addApiResponse(
                status,
                generateHappyPathResponse(
                        status,
                        happyPathResponseDocSchemaName,
                        getCustomResponseHeadersFor(status)
                )
        );
        responses.putAll(generateErrorResponses(supportedHttpErrorCodes));
        return responses;
    }

    private List<? extends ResponseHeader> getCustomResponseHeadersFor(String httpCode) {
        List<? extends CustomResponseHeaderGroup> customResponseHeaders = oasProperties != null
                ? oasProperties.customResponseHeaders()
                : null;
        return emptyIfNull(customResponseHeaders).stream()
                .filter(hg -> httpCode.equalsIgnoreCase(hg.httpStatusCode()))
                .findFirst()
                .map(CustomResponseHeaderGroup::headers)
                .orElse(null);
    }

    /**
     * Builds the operation's success response. Operations that return no body — every write except create — still get
     * one: the status code is what the operation answers with, and an operation whose only documented outcomes are
     * failures is useless to a client generator and incomplete as a contract. Only the body is conditional.
     */
    private ApiResponse generateHappyPathResponse(String status,
                                                  String responseDocSchemaName,
                                                  List<? extends ResponseHeader> responseHeaders) {
        return generateResponse(
                describeHttpStatus(status),
                responseDocSchemaName,
                null,
                responseHeaders
        );
    }

    private String describeHttpStatus(String status) {
        return HttpStatusCodes.fromCode(Integer.parseInt(status))
                .map(HttpStatusCodes::getDescription)
                .orElse("Happy path scenario");
    }

    private Map<String, ApiResponse> generateErrorResponses(Set<HttpStatusCodes> supportedHttpErrorCodes) {
        Map<String, ApiResponse> errorResponses = new LinkedHashMap<>();
        ErrorExamplesCustomizer.CODES_TO_EXAMPLE_NAME.forEach((code, name) -> {
            if (supportedHttpErrorCodes.contains(code)) {
                errorResponses.put(
                        String.valueOf(code.getCode()),
                        generateErrorResponse(
                                code.getDescription(),
                                name,
                                getCustomResponseHeadersFor(String.valueOf(code.getCode()))
                        )
                );
            }
        });
        return errorResponses;
    }

    private ApiResponse generateErrorResponse(String description,
                                              String exampleName,
                                              List<? extends ResponseHeader> customResponseHeaders) {
        return generateResponse(
                description,
                OasSchemaNamesUtil.errorsDocSchemaName(),
                exampleName,
                customResponseHeaders
        );
    }

    private ApiResponse generateResponse(String description,
                                         String responseDocSchemaName,
                                         String exampleName,
                                         List<? extends ResponseHeader> customResponseHeaders) {
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

        if (CollectionUtils.isNotEmpty(customResponseHeaders)) {
            Map<String, Header> headers = response.getHeaders();
            if (response.getHeaders() == null) {
                headers = new LinkedHashMap<>();
                response.setHeaders(headers);
            }
            for (ResponseHeader responseHeader : customResponseHeaders) {
                headers.put(responseHeader.name(), new Header()
                        .required(responseHeader.required())
                        .description(responseHeader.description())
                        .schema(resolveResponseHeaderSchema(responseHeader.schema()))
                        .example(responseHeader.example()));
            }
        }

        return response;
    }

    private Schema resolveResponseHeaderSchema(String schema) {
        if ("integer".equalsIgnoreCase(schema)) {
            return new IntegerSchema();
        } else {
            return new StringSchema();
        }
    }

    private List<SecurityRequirement> generateSecurityRequirements(OasOperationInfoModel.SecurityConfig securityConfig) {
        if (securityConfig == null) {
            return null;
        }
        List<SecurityRequirement> securityRequirements = new ArrayList<>();
        if (securityConfig.isClientCredentialsSupported()) {
            addSecurityRequirement(
                    securityRequirements,
                    resolveSecuritySchemeName(OasProperties.OAuth2::clientCredentials),
                    securityConfig.getRequiredScopes()
            );
        }
        if (securityConfig.isPkceSupported()) {
            addSecurityRequirement(
                    securityRequirements,
                    resolveSecuritySchemeName(OasProperties.OAuth2::authorizationCodeWithPkce),
                    securityConfig.getRequiredScopes()
            );
        }
        return securityRequirements;
    }

    private void addSecurityRequirement(List<SecurityRequirement> securityRequirements,
                                        String schemeName,
                                        List<String> requiredScopes) {
        if (schemeName != null) {
            securityRequirements.add(new SecurityRequirement().addList(schemeName, requiredScopes));
        }
    }

    private String resolveSecuritySchemeName(Function<OasProperties.OAuth2, OasProperties.OAuth2GrantFlow> grantFlowAccessor) {
        if (oasProperties == null || oasProperties.oauth2() == null) {
            return null;
        }
        OasProperties.OAuth2GrantFlow grantFlow = grantFlowAccessor.apply(oasProperties.oauth2());
        if (grantFlow == null || StringUtils.isBlank(grantFlow.name())) {
            return null;
        }
        return grantFlow.name();
    }

    private List<Parameter> generateCustomParameters(List<OasOperationInfoModel.Parameter> customParameters) {
        return customParameters.stream().map(
                p -> {
                    Parameter parameter = new Parameter();
                    parameter.setName(p.getName());
                    parameter.setDescription(p.getDescription());
                    parameter.setRequired(p.isRequired());
                    parameter.setIn(p.getIn().getName());
                    if (p.isArray()) {
                        parameter.setSchema(new ArraySchema().items(new Schema().type(p.getType().getType()).example(p.getExample())));
                    } else {
                        parameter.setExample(p.getExample());
                        parameter.setSchema(new Schema().type(p.getType().getType()));
                    }
                    return parameter;
                }
        ).toList();
    }

    private List<Parameter> generateJsonApiParameters(List<String> availableIncludes,
                                                      OasOperationInfoUtil.Info extraOperationInfo,
                                                      OasOperationInfoModel oasOperationInfo) {
        List<Parameter> params = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(availableIncludes)) {
            params.add(createIncludeParam(availableIncludes));
        }
        params.addAll(createSparseFieldsetsParams(extraOperationInfo));
        if (extraOperationInfo.isPaginationSupported()) {
            for (PaginationStyle style : paginationStylesOf(oasOperationInfo)) {
                params.addAll(createPaginationParams(style));
            }
        }
        createSortParam(sortableFieldsOf(oasOperationInfo)).ifPresent(params::add);
        if (OperationType.getExistingResourceAwareOperations().contains(extraOperationInfo.getOperationType())) {
            params.add(createDefaultIdPathParam());
        }
        return params;
    }

    private List<String> sortableFieldsOf(OasOperationInfoModel oasOperationInfo) {
        return oasOperationInfo == null ? List.of() : emptyIfNull(oasOperationInfo.getSortableFields()).stream().toList();
    }

    private Set<PaginationStyle> paginationStylesOf(OasOperationInfoModel oasOperationInfo) {
        return oasOperationInfo == null || CollectionUtils.isEmpty(oasOperationInfo.getPaginationStyles())
                ? EnumSet.of(PaginationStyle.CURSOR)
                : oasOperationInfo.getPaginationStyles();
    }

    private List<Parameter> createSparseFieldsetsParams(OasOperationInfoUtil.Info extraOperationInfo) {
        if (!isSparseFieldsetsEnabled() || extraOperationInfo.getResponseType() == OasOperationInfoUtil.ResponseType.VOID) {
            return List.of();
        }
        return OasIncludableTypesUtil.sparseFieldsetsResourceTypes(domainRegistry, extraOperationInfo.getResourceType())
                .stream()
                .map(this::createSparseFieldsetParam)
                .toList();
    }

    private boolean isSparseFieldsetsEnabled() {
        return pluginRegistry != null && pluginRegistry.isActivePlugin(SPARSE_FIELDSETS_PLUGIN_NAME);
    }

    private Parameter createSparseFieldsetParam(ResourceType resourceType) {
        Parameter fieldsParam = new Parameter();
        fieldsParam.setName(SparseFieldsetsAwareRequest.getFieldsParam(resourceType.getType()));
        fieldsParam.setIn("query");
        fieldsParam.setRequired(false);
        fieldsParam.setDescription(String.format(
                "Limits the attributes returned for '%s' resources to the listed ones. Nested paths are allowed "
                        + "(address.city). See the %s schema for what can be asked for. An empty value returns no attributes.",
                resourceType.getType(),
                OasSchemaNamesUtil.attributesSchemaName(resourceType)
        ));
        fieldsParam.setSchema(new ArraySchema().items(new StringSchema()));
        return fieldsParam;
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

    private Parameter createDefaultIdPathParam() {
        Parameter cursorParam = new Parameter();
        cursorParam.setName("id");
        cursorParam.setIn("path");
        cursorParam.setDescription("Resource id. Required");
        StringSchema idSchema = new StringSchema();
        if (validationProperties != null) {
            idSchema.setMaxLength(validationProperties.resourceIdMaxLength());
        }
        cursorParam.setSchema(idSchema);
        return cursorParam;
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
