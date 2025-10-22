package pro.api4.jsonapi4j.oas.customizer;

import pro.api4.jsonapi4j.config.OasProperties;
import pro.api4.jsonapi4j.plugin.OperationPluginAware;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.http.HttpStatusCodes;
import pro.api4.jsonapi4j.oas.customizer.util.OasOperationInfoUtil;
import pro.api4.jsonapi4j.oas.customizer.util.OasSchemaNamesUtil;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.operation.plugin.OperationOasPlugin;
import pro.api4.jsonapi4j.request.CursorAwareRequest;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiMediaType;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static pro.api4.jsonapi4j.oas.OasOperationExtensionProperties.JSONAPI_AVAILABLE_RELATIONSHIPS;
import static pro.api4.jsonapi4j.oas.OasOperationExtensionProperties.URL_COMPATIBLE_UNIQUE_NAME;
import static pro.api4.jsonapi4j.oas.OasOperationExtensions.X_OPERATION_PROPERTIES;
import static pro.api4.jsonapi4j.oas.customizer.util.OasOperationInfoUtil.resolveRelationshipOperationPath;
import static pro.api4.jsonapi4j.oas.customizer.util.OasOperationInfoUtil.resolveResourceOperationPath;
import static pro.api4.jsonapi4j.oas.customizer.util.SchemaGeneratorUtil.getSchemaName;
import static org.apache.commons.collections4.MapUtils.emptyIfNull;


@Slf4j
@Data
public class JsonApiOperationsCustomizer {

    public static final String OAUTH2_CLIENT_CREDENTIALS = "Client_Credentials";
    public static final String OAUTH2_AUTHORIZATION_CODE_PKCE = "Authorization_Code_PKCE";

    private final String rootPath;
    private final OperationsRegistry operationsRegistry;
    private final Map<String, Map<String, OasProperties.ResponseHeader>> customResponseHeaders;

    public void customise(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            openApi.setPaths(new Paths());
        }
        createAndRegisterOperations(openApi.getPaths());
    }

    private void createAndRegisterOperations(Paths paths) {
        operationsRegistry.getResourceTypesWithAnyOperationConfigured()
                .stream()
                .sorted(Comparator.comparing(ResourceType::getType))
                .forEach(resourceType -> {
                    createAndRegisterResourceOperations(paths, resourceType);
                    operationsRegistry.getRelationshipNamesWithAnyOperationConfigured(resourceType)
                            .stream()
                            .sorted(Comparator.comparing(RelationshipName::getName))
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
        OperationPluginAware pluginAware = operationsRegistry.getResourceOperation(resourceType, operationType, false);
        return createOperation(
                resourceType,
                null,
                operationType,
                pluginAware
        );
    }

    private Operation createRelationshipOperation(ResourceType resourceType,
                                                  RelationshipName relationshipName,
                                                  OperationType operationType) {
        OperationPluginAware pluginAware = operationsRegistry.getRelationshipOperation(resourceType, relationshipName, operationType, false);
        return createOperation(
                resourceType,
                relationshipName,
                operationType,
                pluginAware
        );
    }

    private Operation createOperation(ResourceType resourceType,
                                      RelationshipName relationshipName,
                                      OperationType operationType,
                                      OperationPluginAware pluginAwareOperation) {
        if (pluginAwareOperation == null) {
            log.warn("Can't generate OAS info for {} operation. It wasn't registered for {} resource.", operationType.name(), resourceType.getType());
            return null;
        }

        OperationOasPlugin oasExtension = pluginAwareOperation.getPluginOrDefault(
                OperationOasPlugin.class,
                OperationOasPlugin.DEFAULT
        );

        OasOperationInfoUtil.Info operationOasInfo = OasOperationInfoUtil.resolveOperationOasInfo(
                resourceType,
                relationshipName,
                operationType,
                oasExtension.getResourceNameSingle(),
                oasExtension.getResourceNamePlural()
        );

        List<String> supportedIncludes = getSupportedIncludes(operationOasInfo);

        String payloadSchemaName = getSchemaName(
                oasExtension.getPayloadType()
        );
        String happyPathResponseDocSchemaName = OasSchemaNamesUtil.happyPathResponseDocSchemaName(
                resourceType,
                operationType
        );

        return createOasOperation(
                oasExtension,
                operationOasInfo,
                operationType.getHttpStatus(),
                payloadSchemaName,
                happyPathResponseDocSchemaName,
                operationOasInfo.getSupportedHttpErrorCodes(),
                supportedIncludes
        );
    }

    private List<String> getSupportedIncludes(OasOperationInfoUtil.Info operationOasInfo) {
        if (operationOasInfo.isIncludesSupported()) {
            if (operationOasInfo.getRelationshipName() != null) {
                return Collections.singletonList(operationOasInfo.getRelationshipName().getName());
            } else {
                return operationsRegistry.getRelationshipNamesWithReadOperationConfigured(operationOasInfo.getResourceType())
                        .stream()
                        .map(RelationshipName::getName)
                        .sorted()
                        .toList();
            }
        }
        return Collections.emptyList();
    }

    private Operation createOasOperation(OperationOasPlugin oasExtension,
                                         OasOperationInfoUtil.Info operationOasInfo,
                                         int httpStatus,
                                         String payloadSchemaName,
                                         String happyPathResponseDocSchemaName,
                                         Set<HttpStatusCodes> supportedHttpErrorCodes,
                                         List<String> supportedIncludes) {

        Operation oasOperation = new Operation();
        oasOperation.setSummary(operationOasInfo.getSummary());
        oasOperation.setDescription(operationOasInfo.getDescription());
        oasOperation.setTags(Collections.singletonList(operationOasInfo.getOperationTag()));

        // request body
        if (StringUtils.isNotBlank(payloadSchemaName)) {
            oasOperation.setRequestBody(new RequestBody().content(new Content().addMediaType(JsonApiMediaType.MEDIA_TYPE, new MediaType().schema(new Schema().$ref(payloadSchemaName)))));
        }

        // responses
        oasOperation.setResponses(
                generateResponses(
                        String.valueOf(httpStatus),
                        happyPathResponseDocSchemaName,
                        supportedHttpErrorCodes
                )
        );

        // parameters
        List<Parameter> parameters = new ArrayList<>();
        parameters.addAll(generateCustomParameters(oasExtension));
        parameters.addAll(generateJsonApiParameters(supportedIncludes, operationOasInfo.isPaginationSupported()));
        oasOperation.setParameters(parameters);

        // security requirements
        oasOperation.setSecurity(generateSecurityRequirements(oasExtension.getSecurityConfig()));

        // oas extensions
        addOperationExtensions(oasOperation, operationOasInfo.getUrlCompatibleUniqueName(), supportedIncludes);

        return oasOperation;
    }

    private ApiResponses generateResponses(String status,
                                           String happyPathResponseDocSchemaName,
                                           Set<HttpStatusCodes> supportedHttpErrorCodes) {
        ApiResponses responses = new ApiResponses();
        if (StringUtils.isNotBlank(happyPathResponseDocSchemaName)) {
            responses.addApiResponse(
                    status,
                    generateHappyPathResponse(
                            happyPathResponseDocSchemaName,
                            emptyIfNull(customResponseHeaders).get(status)
                    )
            );
        }
        responses.putAll(generateErrorResponses(supportedHttpErrorCodes));
        return responses;
    }

    private ApiResponse generateHappyPathResponse(String responseDocSchemaName,
                                                  Map<String, OasProperties.ResponseHeader> customResponseHeaders) {
        return generateResponse("Happy path scenario", responseDocSchemaName, null, customResponseHeaders);
    }

    private Map<String, ApiResponse> generateErrorResponses(Set<HttpStatusCodes> supportedHttpErrorCodes) {
        Map<String, ApiResponse> errorResponses = new HashMap<>();
        ErrorExamplesCustomizer.CODES_TO_EXAMPLE_NAME.forEach((code, name) -> {
            if (supportedHttpErrorCodes.contains(code)) {
                errorResponses.put(
                        String.valueOf(code.getCode()),
                        generateErrorResponse(
                                code.getDescription(),
                                name,
                                emptyIfNull(customResponseHeaders).get(String.valueOf(code.getCode()))
                        )
                );
            }
        });
        return errorResponses;
    }

    private ApiResponse generateErrorResponse(String description,
                                              String exampleName,
                                              Map<String, OasProperties.ResponseHeader> customResponseHeaders) {
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
                                         Map<String, OasProperties.ResponseHeader> customResponseHeaders) {
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

        if (MapUtils.isNotEmpty(customResponseHeaders)) {
            Map<String, Header> headers = response.getHeaders();
            if (response.getHeaders() == null) {
                headers = new LinkedHashMap<>();
                response.setHeaders(headers);
            }
            for (Map.Entry<String, OasProperties.ResponseHeader> responseHeader : customResponseHeaders.entrySet()) {
                OasProperties.ResponseHeader config = responseHeader.getValue();
                headers.put(responseHeader.getKey(), new Header()
                        .required(config.isRequired())
                        .description(config.getDescription())
                        .schema(resolveResponseHeaderSchema(config.getSchema()))
                        .example(config.getExample()));
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

    private List<SecurityRequirement> generateSecurityRequirements(OperationOasPlugin.SecurityConfig securityConfig) {
        if (securityConfig != null) {
            List<SecurityRequirement> securityRequirements = new ArrayList<>();
            if (securityConfig.isClientCredentialsSupported()) {
                securityRequirements.add(new SecurityRequirement().addList(OAUTH2_CLIENT_CREDENTIALS));
            }
            if (securityConfig.isPkceSupported()) {
                securityRequirements.add(new SecurityRequirement().addList(OAUTH2_AUTHORIZATION_CODE_PKCE, securityConfig.getRequiredScopes()));
            }
            return securityRequirements;
        }
        return null;
    }

    private List<Parameter> generateCustomParameters(OperationOasPlugin oasExtension) {
        if (oasExtension != null) {
            oasExtension.getParameters().stream().map(
                    parameterConfig -> {
                        Parameter parameter = new Parameter();
                        parameter.setName(parameterConfig.getName());
                        parameter.setDescription(parameterConfig.getDescription());
                        parameter.setRequired(parameterConfig.isRequired());
                        parameter.setIn(parameterConfig.getIn().getName());
                        if (parameterConfig.isArray()) {
                            parameter.setSchema(new ArraySchema().items(new Schema().type(parameterConfig.getType().getType()).example(parameterConfig.getExample())));
                        } else {
                            parameter.setExample(parameterConfig.getExample());
                            parameter.setSchema(new Schema().type(parameterConfig.getType().getType()));
                        }
                        return parameter;
                    }
            ).toList();
        }
        return Collections.emptyList();
    }

    private List<Parameter> generateJsonApiParameters(List<String> availableIncludes,
                                                      boolean isPaginationSupported) {
        List<Parameter> params = new ArrayList<>();
        if (availableIncludes != null && !availableIncludes.isEmpty()) {
            params.add(createIncludeParam(availableIncludes));
        }
        if (isPaginationSupported) {
            params.add(createCursorParam());
        }
        return params;
    }

    private Parameter createIncludeParam(List<String> availableRelationships) {
        Parameter includeQueryParam = new Parameter();
        includeQueryParam.setName(IncludeAwareRequest.INCLUDE_PARAM);
        includeQueryParam.setIn("query");
        includeQueryParam.setRequired(false);

        String example = availableRelationships.stream().findFirst().orElse(null);

        String description = "Allows clients to customize which related resources should be returned in compound docs" +
                ". Available relationships: " + String.join(", ", availableRelationships);

        includeQueryParam.setDescription(description);
        includeQueryParam.setSchema(
                new ArraySchema().items(new StringSchema().example(example)))
        ;
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

    private void addOperationExtensions(Operation operation,
                                        String urlCompatibleUniqueName,
                                        List<String> supportedIncludes) {
        if (CollectionUtils.isNotEmpty(supportedIncludes)) {
            if (operation.getExtensions() == null) {
                Map<String, Object> extensions = new LinkedHashMap<>();
                operation.setExtensions(extensions);
            }

            if (!operation.getExtensions().containsKey(X_OPERATION_PROPERTIES)) {
                Map<String, Object> operationExtensions = new LinkedHashMap<>();
                operation.getExtensions().put(X_OPERATION_PROPERTIES, operationExtensions);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> operationExtensions
                    = (Map<String, Object>) operation.getExtensions().get(X_OPERATION_PROPERTIES);

            if (StringUtils.isNotEmpty(urlCompatibleUniqueName)) {
                operationExtensions.put(URL_COMPATIBLE_UNIQUE_NAME, urlCompatibleUniqueName);
            }
            if (CollectionUtils.isNotEmpty(supportedIncludes)) {
                operationExtensions.put(JSONAPI_AVAILABLE_RELATIONSHIPS, supportedIncludes);
            }
        }
    }

}
