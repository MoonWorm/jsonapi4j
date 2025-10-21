package pro.api4.jsonapi4j;

import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.Relationship;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.domain.ToManyRelationship;
import pro.api4.jsonapi4j.domain.ToOneRelationship;
import pro.api4.jsonapi4j.domain.plugin.ac.RelationshipsOutboundAccessControlPlugin;
import pro.api4.jsonapi4j.domain.plugin.ac.ResourceOutboundAccessControlPlugin;
import pro.api4.jsonapi4j.model.document.data.MultipleResourcesDoc;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import pro.api4.jsonapi4j.operation.BatchReadToManyRelationshipOperation;
import pro.api4.jsonapi4j.operation.BatchReadToOneRelationshipOperation;
import pro.api4.jsonapi4j.operation.CreateResourceOperation;
import pro.api4.jsonapi4j.operation.DeleteResourceOperation;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.operation.ReadMultipleResourcesOperation;
import pro.api4.jsonapi4j.operation.ReadResourceByIdOperation;
import pro.api4.jsonapi4j.operation.ReadToManyRelationshipOperation;
import pro.api4.jsonapi4j.operation.ReadToOneRelationshipOperation;
import pro.api4.jsonapi4j.operation.UpdateResourceOperation;
import pro.api4.jsonapi4j.operation.UpdateToManyRelationshipOperation;
import pro.api4.jsonapi4j.operation.UpdateToOneRelationshipOperation;
import pro.api4.jsonapi4j.operation.exception.OperationNotFoundException;
import pro.api4.jsonapi4j.operation.plugin.OperationInboundAccessControlPlugin;
import pro.api4.jsonapi4j.plugin.ac.AccessControlEvaluator;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlRequirements;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlRequirementsForObject;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.processor.IdAndType;
import pro.api4.jsonapi4j.processor.IdSupplier;
import pro.api4.jsonapi4j.processor.ResourceProcessorContext;
import pro.api4.jsonapi4j.processor.ac.InboundAccessControlSettings;
import pro.api4.jsonapi4j.processor.ac.OutboundAccessControlSettingsForRelationship;
import pro.api4.jsonapi4j.processor.ac.OutboundAccessControlSettingsForResource;
import pro.api4.jsonapi4j.processor.multi.MultipleDataItemsSupplier;
import pro.api4.jsonapi4j.processor.multi.relationship.ToManyRelationshipsProcessor;
import pro.api4.jsonapi4j.processor.multi.resource.MultipleResourcesProcessor;
import pro.api4.jsonapi4j.processor.resolvers.BatchToManyRelationshipResolver;
import pro.api4.jsonapi4j.processor.resolvers.BatchToOneRelationshipResolver;
import pro.api4.jsonapi4j.processor.resolvers.DefaultRelationshipResolver;
import pro.api4.jsonapi4j.processor.resolvers.ResourceTypeAndIdResolver;
import pro.api4.jsonapi4j.processor.resolvers.ToManyRelationshipResolver;
import pro.api4.jsonapi4j.processor.resolvers.ToOneRelationshipResolver;
import pro.api4.jsonapi4j.processor.resolvers.relationships.DefaultRelationshipResolvers;
import pro.api4.jsonapi4j.processor.single.SingleDataItemSupplier;
import pro.api4.jsonapi4j.processor.single.relationship.ToOneRelationshipProcessor;
import pro.api4.jsonapi4j.processor.single.resource.SingleResourceProcessor;
import pro.api4.jsonapi4j.request.JsonApiRequest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toMap;

/**
 * <p>
 * Main entry point for JSON:API request processing flow. Builds, configures, and executes different types of
 * JSON:API processors based on the request's operation type.
 * </p>
 * <p>
 * The usual flow is the next:
 * <ol>
 *     <li>Accepts {@link JsonApiRequest}</li>
 *     <li>Determines the {@link OperationType} and triggers the corresponding flow e.g. 'read-all', 'update-resource', etc</li>
 *     <li>Validates the {@link JsonApiRequest}</li>
 *     <li>Constructs the corresponding Processor with all needed settings and executes it</li>
 *     <li>Returns the generated JSON:API response, for example, {@link SingleResourceDoc}, {@link ToManyRelationshipsDoc}, etc</li>
 * </ol>
 * </p>
 */
public class JsonApi4j {

    private final DomainRegistry domainRegistry;
    private final OperationsRegistry operationsRegistry;
    private AccessControlEvaluator accessControlEvaluator
            = ResourceProcessorContext.DEFAULT_ACCESS_CONTROL_EVALUATOR;
    private Executor executor
            = ResourceProcessorContext.DEFAULT_EXECUTOR;

    public JsonApi4j(DomainRegistry domainRegistry,
                     OperationsRegistry operationsRegistry) {
        this.domainRegistry = domainRegistry;
        this.operationsRegistry = operationsRegistry;
    }

    public Object execute(JsonApiRequest request) {
        if (request.getTargetRelationshipName() == null) {
            return executeResourceOperation(request);
        } else {
            return executeRelationshipOperations(request);
        }
    }

    public ResourceTypeStepSelected forResourceType(ResourceType resourceType) {
        return new ResourceTypeStepSelected(resourceType);
    }

    private Object executeRelationshipOperations(JsonApiRequest request) {
        switch (request.getOperationType()) {
            case READ_TO_ONE_RELATIONSHIP -> {
                return forResourceType(request.getTargetResourceType())
                        .forToOneRelationship(request.getTargetRelationshipName())
                        .readToOneRelationship(request);
            }
            case READ_TO_MANY_RELATIONSHIP -> {
                return forResourceType(request.getTargetResourceType())
                        .forToManyRelationship(request.getTargetRelationshipName())
                        .readToManyRelationship(request);
            }
            case UPDATE_TO_ONE_RELATIONSHIP -> {
                forResourceType(request.getTargetResourceType())
                        .forToOneRelationship(request.getTargetRelationshipName())
                        .updateToOneRelationship(request);
                return null;
            }
            case UPDATE_TO_MANY_RELATIONSHIP -> {
                forResourceType(request.getTargetResourceType())
                        .forToManyRelationship(request.getTargetRelationshipName())
                        .updateToManyRelationship(request);
                return null;
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported JSON:API request, unknown operation type: " + request.getOperationType()
            );
        }
    }

    private Object executeResourceOperation(JsonApiRequest request) {
        switch (request.getOperationType()) {
            case READ_RESOURCE_BY_ID -> {
                return forResourceType(request.getTargetResourceType())
                        .readResourceById(request);
            }
            case READ_MULTIPLE_RESOURCES -> {
                return forResourceType(request.getTargetResourceType())
                        .readMultipleResources(request);
            }
            case CREATE_RESOURCE -> {
                return forResourceType(request.getTargetResourceType())
                        .createResource(request);
            }
            case UPDATE_RESOURCE -> {
                forResourceType(request.getTargetResourceType())
                        .updateResource(request);
                return null;
            }
            case DELETE_RESOURCE -> {
                forResourceType(request.getTargetResourceType())
                        .deleteResource(request);
                return null;
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported JSON:API request, unknown operation type: " + request.getOperationType()
            );
        }
    }

    private <RELATIONSHIP_DTO> ToOneRelationshipDoc resolveToOneRelationshipDocCommon(
            ResourceType resourceType,
            RelationshipName relationshipName,
            JsonApiRequest relationshipRequest,
            SingleDataItemSupplier<JsonApiRequest, ?> executable,
            OperationInboundAccessControlPlugin operationAcPlugin
    ) {

        @SuppressWarnings("unchecked")
        ToOneRelationship<?, RELATIONSHIP_DTO> relationshipConfig
                = (ToOneRelationship<?, RELATIONSHIP_DTO>) domainRegistry.getToOneRelationshipStrict(resourceType, relationshipName);

        @SuppressWarnings("unchecked")
        SingleDataItemSupplier<JsonApiRequest, RELATIONSHIP_DTO> executableCasted
                = (SingleDataItemSupplier<JsonApiRequest, RELATIONSHIP_DTO>) executable;

        InboundAccessControlSettings inboundAccessControlSettings
                = getInboundAccessControlSettings(operationAcPlugin);

        OutboundAccessControlSettingsForRelationship outboundAccessControlSettings
                = getOutboundAccessControlSettingsForRelationship(relationshipConfig.getPlugin(RelationshipsOutboundAccessControlPlugin.class));

        ResourceTypeAndIdResolver<RELATIONSHIP_DTO> typeAndIdResolver
                = getResourceIdentifierTypeAndIdResolver(relationshipConfig);

        return new ToOneRelationshipProcessor()
                .forRequest(relationshipRequest)
                .accessControlEvaluator(accessControlEvaluator)
                .inboundAccessControlSettings(inboundAccessControlSettings)
                .outboundAccessControlSettings(outboundAccessControlSettings)
                .dataSupplier(executableCasted)
                .topLevelLinksResolver(relationshipConfig::resolveRelationshipLinks)
                .topLevelMetaResolver(relationshipConfig::resolveRelationshipMeta)
                .resourceIdentifierMetaResolver(relationshipConfig::resolveResourceIdentifierMeta)
                .resourceTypeAndIdSupplier(typeAndIdResolver)
                .toToOneRelationshipDoc();
    }

    private <RELATIONSHIP_DTO> ToManyRelationshipsDoc resolveToManyRelationshipsDocCommon(
            ResourceType resourceType,
            RelationshipName relationshipName,
            JsonApiRequest relationshipRequest,
            MultipleDataItemsSupplier<JsonApiRequest, ?> executable,
            OperationInboundAccessControlPlugin operationAcPlugin
    ) {

        @SuppressWarnings("unchecked")
        ToManyRelationship<?, RELATIONSHIP_DTO> relationshipConfig
                = (ToManyRelationship<?, RELATIONSHIP_DTO>) domainRegistry.getToManyRelationshipStrict(resourceType, relationshipName);

        @SuppressWarnings("unchecked")
        MultipleDataItemsSupplier<JsonApiRequest, RELATIONSHIP_DTO> executableCasted
                = (MultipleDataItemsSupplier<JsonApiRequest, RELATIONSHIP_DTO>) executable;

        InboundAccessControlSettings inboundAccessControlSettings
                = getInboundAccessControlSettings(operationAcPlugin);

        OutboundAccessControlSettingsForRelationship outboundAccessControlSettings
                = getOutboundAccessControlSettingsForRelationship(relationshipConfig.getPlugin(RelationshipsOutboundAccessControlPlugin.class));

        ResourceTypeAndIdResolver<RELATIONSHIP_DTO> typeAndIdResolver
                = getResourceIdentifierTypeAndIdResolver(relationshipConfig);

        return new ToManyRelationshipsProcessor()
                .forRequest(relationshipRequest)
                .accessControlEvaluator(accessControlEvaluator)
                .inboundAccessControlSettings(inboundAccessControlSettings)
                .outboundAccessControlSettings(outboundAccessControlSettings)
                .dataSupplier(executableCasted)
                .resourceIdentifierMetaResolver(relationshipConfig::resolveResourceIdentifierMeta)
                .topLevelLinksResolver(relationshipConfig::resolveRelationshipLinks)
                .topLevelMetaResolver(relationshipConfig::resolveRelationshipMeta)
                .resourceTypeAndIdSupplier(typeAndIdResolver)
                .toToManyRelationshipsDoc();
    }

    private InboundAccessControlSettings getInboundAccessControlSettings(
            OperationInboundAccessControlPlugin operationAcPlugin
    ) {
        return InboundAccessControlSettings
                .builder()
                .forRequest(
                        operationAcPlugin == null ?
                                AccessControlRequirements.DEFAULT :
                                operationAcPlugin.getRequestAccessControl()
                )
                .build();
    }

    private OutboundAccessControlSettingsForRelationship getOutboundAccessControlSettingsForRelationship(
            RelationshipsOutboundAccessControlPlugin relationshipAcPlugin
    ) {
        return OutboundAccessControlSettingsForRelationship
                .builder()
                .forResourceIdentifier(
                        relationshipAcPlugin == null ?
                                AccessControlRequirementsForObject.DEFAULT :
                                relationshipAcPlugin.getResourceIdentifierAccessControl()
                )
                .build();
    }

    private <RELATIONSHIP_DTO, RESOURCE_DTO> ResourceTypeAndIdResolver<RELATIONSHIP_DTO> getResourceIdentifierTypeAndIdResolver(
            Relationship<RESOURCE_DTO, RELATIONSHIP_DTO> relationshipConfig) {
        return dto -> new IdAndType(
                relationshipConfig.resolveResourceIdentifierId(dto),
                relationshipConfig.resolveResourceIdentifierType(dto)
        );
    }

    private <RESOURCE_DTO> ResourceTypeAndIdResolver<RESOURCE_DTO> getResourceTypeAndIdResolver(
            Resource<?, RESOURCE_DTO> resourceConfig) {
        return dto -> new IdAndType(
                resourceConfig.resolveResourceId(dto),
                resourceConfig.resourceType()
        );
    }

    public class ResourceTypeStepSelected {

        private final ResourceType resourceType;

        public ResourceTypeStepSelected(ResourceType resourceType) {
            this.resourceType = resourceType;
        }

        public ToManyRelationshipsOperationStepSelected forToManyRelationship(RelationshipName relationshipName) {
            return new ToManyRelationshipsOperationStepSelected(resourceType, relationshipName);
        }

        public ToOneRelationshipOperationStepSelected forToOneRelationship(RelationshipName relationshipName) {
            return new ToOneRelationshipOperationStepSelected(resourceType, relationshipName);
        }

        public <RESOURCE_DTO> SingleResourceDoc<?> readResourceById(JsonApiRequest request) {
            @SuppressWarnings("unchecked")
            ReadResourceByIdOperation<RESOURCE_DTO> executable
                    = (ReadResourceByIdOperation<RESOURCE_DTO>) operationsRegistry.getReadResourceByIdOperation(resourceType, true);
            executable.validate(request);
            return processSingleResource(
                    request,
                    executable::readById,
                    executable.getPlugin(OperationInboundAccessControlPlugin.class)
            );
        }

        public <RESOURCE_DTO> SingleResourceDoc<?> createResource(JsonApiRequest request) {
            @SuppressWarnings("unchecked")
            CreateResourceOperation<RESOURCE_DTO> executable
                    = (CreateResourceOperation<RESOURCE_DTO>) operationsRegistry.getCreateResourceOperation(resourceType, true);
            executable.validate(request);
            return processSingleResource(
                    request,
                    executable::create,
                    executable.getPlugin(OperationInboundAccessControlPlugin.class)
            );
        }

        public void updateResource(JsonApiRequest request) {
            UpdateResourceOperation executable
                    = operationsRegistry.getUpdateResourceOperation(resourceType, true);
            executable.validate(request);
            processSingleResourceNoResponse(
                    request,
                    executable::update,
                    executable.getPlugin(OperationInboundAccessControlPlugin.class)
            );
        }

        public void deleteResource(JsonApiRequest request) {
            DeleteResourceOperation executable
                    = operationsRegistry.getDeleteResourceOperation(resourceType, true);
            executable.validate(request);
            processSingleResourceNoResponse(
                    request,
                    executable::delete,
                    executable.getPlugin(OperationInboundAccessControlPlugin.class)
            );
        }

        private <ATTRIBUTES, RESOURCE_DTO> SingleResourceDoc<?> processSingleResource(
                JsonApiRequest request,
                SingleDataItemSupplier<JsonApiRequest, RESOURCE_DTO> dataSupplier,
                OperationInboundAccessControlPlugin operationAcPlugin
        ) {
            @SuppressWarnings("unchecked")
            Resource<ATTRIBUTES, RESOURCE_DTO> resourceConfig
                    = (Resource<ATTRIBUTES, RESOURCE_DTO>) domainRegistry.getResource(resourceType);

            return new SingleResourceProcessor()
                    .forRequest(request)
                    .concurrentRelationshipResolution(executor)
                    .accessControlEvaluator(accessControlEvaluator)
                    .inboundAccessControlSettings(getInboundAccessControlSettings(operationAcPlugin))
                    .outboundAccessControlSettings(getOutboundAccessControlSettingsForResource(resourceConfig))
                    .dataSupplier(dataSupplier)
                    .defaultRelationships(getDefaultRelationshipResolvers(resourceConfig::resolveResourceId))
                    .toManyRelationshipResolvers(getToManyRelationshipsResolvers(resourceConfig::resolveResourceId))
                    .batchToManyRelationshipResolvers(getBatchToManyRelationshipResolvers(resourceConfig::resolveResourceId))
                    .toOneRelationshipResolvers(getToOneRelationshipResolvers(resourceConfig::resolveResourceId))
                    .batchToOneRelationshipResolvers(getBatchToOneRelationshipResolvers(resourceConfig::resolveResourceId))
                    .topLevelLinksResolver(resourceConfig::resolveTopLevelLinksForSingleResourceDoc)
                    .topLevelMetaResolver(resourceConfig::resolveTopLevelMetaForSingleResourceDoc)
                    .resourceLinksResolver(resourceConfig::resolveResourceLinks)
                    .resourceMetaResolver(resourceConfig::resolveResourceMeta)
                    .attributesResolver(resourceConfig::resolveAttributes)
                    .resourceTypeAndIdResolver(getResourceTypeAndIdResolver(resourceConfig))
                    .toSingleResourceDoc();
        }

        private void processSingleResourceNoResponse(
                JsonApiRequest request,
                Consumer<JsonApiRequest> executable,
                OperationInboundAccessControlPlugin operationAcPlugin
        ) {
            processSingleResource(request, req -> {
                        executable.accept(req);
                        return null;
                    },
                    operationAcPlugin);
        }

        public <RESOURCE_DTO> MultipleResourcesDoc<?> readMultipleResources(JsonApiRequest request) {

            @SuppressWarnings("unchecked")
            ReadMultipleResourcesOperation<RESOURCE_DTO> readAllExecutable
                    = (ReadMultipleResourcesOperation<RESOURCE_DTO>) operationsRegistry.getReadMultipleResourcesOperation(resourceType, false);
            if (readAllExecutable != null) {
                readAllExecutable.validate(request);
                return processMultipleResources(
                        request,
                        readAllExecutable::readPage,
                        readAllExecutable.getPlugin(OperationInboundAccessControlPlugin.class)
                );
            } else if (request.getFilters().size() == 1
                    && request.getFilters().containsKey(ReadMultipleResourcesOperation.ID_FILTER_NAME)) {
                @SuppressWarnings("unchecked")
                ReadResourceByIdOperation<RESOURCE_DTO> readByIdExecutable
                        = (ReadResourceByIdOperation<RESOURCE_DTO>) operationsRegistry.getReadResourceByIdOperation(resourceType, false);
                if (readByIdExecutable != null) {
                    ReadMultipleResourcesOperation<RESOURCE_DTO> mimickedReadAllExecutable = mimicReadMultipleResourcesOperationViaSequentialReadByIds(readByIdExecutable);
                    mimickedReadAllExecutable.validate(request);
                    return processMultipleResources(
                            request,
                            mimickedReadAllExecutable::readPage,
                            readByIdExecutable.getPlugin(OperationInboundAccessControlPlugin.class)
                    );
                }
            }
            throw new OperationNotFoundException(OperationType.READ_MULTIPLE_RESOURCES, resourceType);
        }

        private <RESOURCE_DTO> ReadMultipleResourcesOperation<RESOURCE_DTO> mimicReadMultipleResourcesOperationViaSequentialReadByIds(
                ReadResourceByIdOperation<RESOURCE_DTO> readByIdExecutable
        ) {
            return new ReadMultipleResourcesOperation<>() {

                @Override
                public CursorPageableResponse<RESOURCE_DTO> readPage(JsonApiRequest request) {
                    List<RESOURCE_DTO> result = request.getFilters().get(ReadMultipleResourcesOperation.ID_FILTER_NAME).stream().map(id -> {
                        JsonApiRequest readByIdRequest = JsonApiRequest.composeReadByIdRequest(
                                id,
                                resourceType
                        );
                        return readByIdExecutable.readById(readByIdRequest);
                    }).toList();
                    return CursorPageableResponse.fromItemsNotPageable(result);
                }

                @Override
                public ResourceType resourceType() {
                    return resourceType;
                }

            };
        }

        private <ATTRIBUTES, DATA_SOURCE_DTO> MultipleResourcesDoc<?> processMultipleResources(
                JsonApiRequest request,
                MultipleDataItemsSupplier<JsonApiRequest, DATA_SOURCE_DTO> dataSupplier,
                OperationInboundAccessControlPlugin operationAcPlugin
        ) {
            @SuppressWarnings("unchecked")
            Resource<ATTRIBUTES, DATA_SOURCE_DTO> resourceConfig
                    = (Resource<ATTRIBUTES, DATA_SOURCE_DTO>) domainRegistry.getResource(resourceType);

            return new MultipleResourcesProcessor()
                    .forRequest(request)
                    .concurrentRelationshipResolution(executor)
                    .accessControlEvaluator(accessControlEvaluator)
                    .inboundAccessControlSettings(getInboundAccessControlSettings(operationAcPlugin))
                    .outboundAccessControlSettings(getOutboundAccessControlSettingsForResource(resourceConfig))
                    .dataSupplier(dataSupplier)
                    .defaultRelationships(getDefaultRelationshipResolvers(resourceConfig::resolveResourceId))
                    .toManyRelationshipResolvers(getToManyRelationshipsResolvers(resourceConfig::resolveResourceId))
                    .batchToManyRelationshipResolvers(getBatchToManyRelationshipResolvers(resourceConfig::resolveResourceId))
                    .toOneRelationshipResolvers(getToOneRelationshipResolvers(resourceConfig::resolveResourceId))
                    .batchToOneRelationshipResolvers(getBatchToOneRelationshipResolvers(resourceConfig::resolveResourceId))
                    .topLevelLinksResolver(resourceConfig::resolveTopLevelLinksForMultiResourcesDoc)
                    .topLevelMetaResolver(resourceConfig::resolveTopLevelMetaForMultiResourcesDoc)
                    .resourceLinksResolver(resourceConfig::resolveResourceLinks)
                    .resourceMetaResolver(resourceConfig::resolveResourceMeta)
                    .attributesResolver(resourceConfig::resolveAttributes)
                    .resourceTypeAndIdResolver(getResourceTypeAndIdResolver(resourceConfig))
                    .toMultipleResourcesDoc();
        }

        private OutboundAccessControlSettingsForResource getOutboundAccessControlSettingsForResource(
                Resource<?, ?> resourceConfig
        ) {
            return resourceConfig != null
                    ? getOutboundAccessControlSettingsForResource(
                            resourceConfig.getPlugin(ResourceOutboundAccessControlPlugin.class)
                    )
                    : OutboundAccessControlSettingsForResource.DEFAULT;
        }

        private OutboundAccessControlSettingsForResource getOutboundAccessControlSettingsForResource(
                ResourceOutboundAccessControlPlugin resourceAcPlugin
        ) {
            return OutboundAccessControlSettingsForResource.builder()
                    .forResource(resourceAcPlugin == null || resourceAcPlugin.getResourceAccessControl() == null
                            ? AccessControlRequirementsForObject.DEFAULT
                            : resourceAcPlugin.getResourceAccessControl())
                    .forAttributes(resourceAcPlugin == null || resourceAcPlugin.getAttributesAccessControl() == null
                            ? AccessControlRequirementsForObject.DEFAULT
                            : resourceAcPlugin.getAttributesAccessControl())
                    .build();
        }

        private <REQUEST, DATA_SOURCE_DTO> Map<RelationshipName, DefaultRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> getDefaultRelationshipResolvers(
                IdSupplier<DATA_SOURCE_DTO> resourceIdSupplier
        ) {
            return domainRegistry.getAvailableRelationshipNames(resourceType).stream()
                    .collect(
                            toMap(
                                    r -> r,
                                    r -> DefaultRelationshipResolvers.defaultRelationshipResolver(
                                            resourceType,
                                            resourceIdSupplier
                                    )
                            )
                    );
        }

        @SuppressWarnings("unchecked")
        private <RESOURCE_DTO> Map<RelationshipName, ToManyRelationshipResolver<JsonApiRequest, RESOURCE_DTO>> getToManyRelationshipsResolvers(
                IdSupplier<RESOURCE_DTO> resourceIdSupplier
        ) {
            return domainRegistry.getToManyRelationships(resourceType)
                    .stream()
                    .filter(r -> !hasReadToManyRelationshipsBatchImplementation(r))
                    .collect(
                            toMap(
                                    ToManyRelationship::relationshipName,
                                    relationshipConfig ->
                                            (req, dataSourceDto) ->
                                                    resolveToManyRelationships(
                                                            (ToManyRelationship<RESOURCE_DTO, ?>) relationshipConfig,
                                                            dataSourceDto,
                                                            resourceIdSupplier,
                                                            req
                                                    )
                            )
                    );
        }

        private <RESOURCE_DTO, RELATIONSHIP_DTO> ToManyRelationshipsDoc resolveToManyRelationships(
                ToManyRelationship<RESOURCE_DTO, ?> relationshipConfig,
                RESOURCE_DTO dataSourceDto,
                IdSupplier<RESOURCE_DTO> resourceIdSupplier,
                JsonApiRequest parentRequest
        ) {

            RelationshipName relationshipName = relationshipConfig.relationshipName();

            @SuppressWarnings("unchecked")
            ToManyRelationship<RESOURCE_DTO, RELATIONSHIP_DTO> relationshipConfigCasted
                    = (ToManyRelationship<RESOURCE_DTO, RELATIONSHIP_DTO>) relationshipConfig;

            @SuppressWarnings("unchecked")
            ReadToManyRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO> executable
                    = (ReadToManyRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO>) operationsRegistry.getReadToManyDataRelationshipOperation(resourceType, relationshipName, true);

            JsonApiRequest relationshipRequest
                    = getRelationshipRequestSupplier(relationshipConfigCasted, resourceIdSupplier, executable::validate).create(parentRequest, dataSourceDto);

            MultipleDataItemsSupplier<JsonApiRequest, RELATIONSHIP_DTO> dataSupplier
                    = relRequest -> executable.readForResource(relRequest, dataSourceDto);

            return resolveToManyRelationshipsDocCommon(
                    resourceType,
                    relationshipName,
                    relationshipRequest,
                    dataSupplier,
                    executable.getPlugin(OperationInboundAccessControlPlugin.class)
            );
        }

        @SuppressWarnings("unchecked")
        private <RESOURCE_DTO> Map<RelationshipName, BatchToManyRelationshipResolver<JsonApiRequest, RESOURCE_DTO>> getBatchToManyRelationshipResolvers(
                IdSupplier<RESOURCE_DTO> resourceIdSupplier
        ) {
            return domainRegistry.getToManyRelationships(resourceType)
                    .stream()
                    .filter(this::hasReadToManyRelationshipsBatchImplementation)
                    .collect(
                            toMap(
                                    ToManyRelationship::relationshipName,
                                    relationshipConfig ->
                                            (req, dataSourceDtos) ->
                                                    resolveToManyRelationshipsInBatch(
                                                            (ToManyRelationship<RESOURCE_DTO, ?>) relationshipConfig,
                                                            dataSourceDtos,
                                                            resourceIdSupplier,
                                                            req
                                                    )
                            )
                    );
        }

        private <RESOURCE_DTO, RELATIONSHIP_DTO> Map<RESOURCE_DTO, ToManyRelationshipsDoc> resolveToManyRelationshipsInBatch(
                ToManyRelationship<RESOURCE_DTO, ?> relationshipConfig,
                List<RESOURCE_DTO> dataSourceDtos,
                IdSupplier<RESOURCE_DTO> resourceIdSupplier,
                JsonApiRequest parentRequest
        ) {

            @SuppressWarnings("unchecked")
            ToManyRelationship<RESOURCE_DTO, RELATIONSHIP_DTO> relationshipConfigCasted
                    = (ToManyRelationship<RESOURCE_DTO, RELATIONSHIP_DTO>) relationshipConfig;

            @SuppressWarnings("unchecked")
            BatchReadToManyRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO> executable
                    = (BatchReadToManyRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO>) operationsRegistry.getReadToManyDataRelationshipOperation(resourceType, relationshipConfig.relationshipName(), true);

            InboundAccessControlSettings inboundAccessControlSettings
                    = getInboundAccessControlSettings(executable.getPlugin(OperationInboundAccessControlPlugin.class));

            OutboundAccessControlSettingsForRelationship outboundAccessControlSettings
                    = getOutboundAccessControlSettingsForRelationship(relationshipConfig.getPlugin(RelationshipsOutboundAccessControlPlugin.class));

            RelationshipRequestSupplier<JsonApiRequest, RESOURCE_DTO> relationshipRequestSupplier
                    = getRelationshipRequestSupplier(relationshipConfigCasted, resourceIdSupplier, executable::validate);

            ResourceTypeAndIdResolver<RELATIONSHIP_DTO> resourceIdentifierTypeAndIdResolver
                    = getResourceIdentifierTypeAndIdResolver(relationshipConfigCasted);

            return new BatchToManyRelationshipsProcessor()
                    .accessControlEvaluator(accessControlEvaluator)
                    .inboundAccessControlSettings(inboundAccessControlSettings)
                    .outboundAccessControlSettings(outboundAccessControlSettings)
                    .dataSupplier(executable::readBatches)
                    .resourceIdentifierMetaResolver(relationshipConfigCasted::resolveResourceIdentifierMeta)
                    .topLevelLinksResolver(relationshipConfigCasted::resolveRelationshipLinks)
                    .topLevelMetaResolver(relationshipConfigCasted::resolveRelationshipMeta)
                    .resourceIdentifierTypeAndIdResolver(resourceIdentifierTypeAndIdResolver)
                    .toManyRelationshipsDocBatch(parentRequest, dataSourceDtos, relationshipRequestSupplier);
        }

        private boolean hasReadToManyRelationshipsBatchImplementation(ToManyRelationship<?, ?> relationshipConfig) {
            ReadToManyRelationshipOperation<?, ?> operation
                    = operationsRegistry.getReadToManyDataRelationshipOperation(resourceType, relationshipConfig.relationshipName(), false);
            return operation instanceof BatchReadToManyRelationshipOperation;
        }

        @SuppressWarnings("unchecked")
        private <RESOURCE_DTO> Map<RelationshipName, ToOneRelationshipResolver<JsonApiRequest, RESOURCE_DTO>> getToOneRelationshipResolvers(
                IdSupplier<RESOURCE_DTO> resourceIdSupplier
        ) {
            return domainRegistry.getToOneRelationships(resourceType)
                    .stream()
                    .filter(r -> !hasReadToOneRelationshipBatchImplementation(r))
                    .collect(
                            toMap(
                                    ToOneRelationship::relationshipName,
                                    relationshipConfig ->
                                            (req, dataSourceDto) ->
                                                    resolveSingleDataRelationshipDoc(
                                                            (ToOneRelationship<RESOURCE_DTO, ?>) relationshipConfig,
                                                            dataSourceDto,
                                                            resourceIdSupplier,
                                                            req
                                                    )
                            )
                    );
        }

        private <RESOURCE_DTO, RELATIONSHIP_DTO> ToOneRelationshipDoc resolveSingleDataRelationshipDoc(
                ToOneRelationship<RESOURCE_DTO, ?> relationshipConfig,
                RESOURCE_DTO dataSourceDto,
                IdSupplier<RESOURCE_DTO> resourceIdSupplier,
                JsonApiRequest parentRequest
        ) {
            RelationshipName relationshipName
                    = relationshipConfig.relationshipName();

            @SuppressWarnings("unchecked")
            ReadToOneRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO> executable
                    = (ReadToOneRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO>) operationsRegistry.getReadToOneRelationshipOperation(resourceType, relationshipName, true);

            JsonApiRequest relationshipRequest
                    = getRelationshipRequestSupplier(relationshipConfig, resourceIdSupplier, executable::validate).create(parentRequest, dataSourceDto);

            SingleDataItemSupplier<JsonApiRequest, RELATIONSHIP_DTO> dataSupplier
                    = relRequest -> executable.readForResource(relRequest, dataSourceDto);

            return resolveToOneRelationshipDocCommon(
                    resourceType,
                    relationshipName,
                    relationshipRequest,
                    dataSupplier,
                    executable.getPlugin(OperationInboundAccessControlPlugin.class)
            );
        }

        @SuppressWarnings("unchecked")
        private <RESOURCE_DTO> Map<RelationshipName, BatchToOneRelationshipResolver<JsonApiRequest, RESOURCE_DTO>> getBatchToOneRelationshipResolvers(
                IdSupplier<RESOURCE_DTO> resourceIdSupplier
        ) {
            return domainRegistry.getToOneRelationships(resourceType)
                    .stream()
                    .filter(this::hasReadToOneRelationshipBatchImplementation)
                    .collect(
                            toMap(
                                    ToOneRelationship::relationshipName,
                                    relationshipConfig ->
                                            (req, dataSourceDtos) ->
                                                    resolveToOneRelationshipInBatch(
                                                            (ToOneRelationship<RESOURCE_DTO, ?>) relationshipConfig,
                                                            dataSourceDtos,
                                                            resourceIdSupplier,
                                                            req
                                                    )
                            )
                    );
        }

        private <RESOURCE_DTO, RELATIONSHIP_DTO> Map<RESOURCE_DTO, ToOneRelationshipDoc> resolveToOneRelationshipInBatch(
                ToOneRelationship<RESOURCE_DTO, ?> relationshipConfig,
                List<RESOURCE_DTO> dataSourceDtos,
                IdSupplier<RESOURCE_DTO> resourceIdSupplier,
                JsonApiRequest request
        ) {

            @SuppressWarnings("unchecked")
            ToOneRelationship<RESOURCE_DTO, RELATIONSHIP_DTO> relationshipConfigCasted
                    = (ToOneRelationship<RESOURCE_DTO, RELATIONSHIP_DTO>) relationshipConfig;

            @SuppressWarnings("unchecked")
            BatchReadToOneRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO> executable
                    = (BatchReadToOneRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO>) operationsRegistry.getReadToOneRelationshipOperation(resourceType, relationshipConfig.relationshipName(), true);

            InboundAccessControlSettings inboundAccessControlSettings
                    = getInboundAccessControlSettings(executable.getPlugin(OperationInboundAccessControlPlugin.class));

            OutboundAccessControlSettingsForRelationship outboundAccessControlSettings
                    = getOutboundAccessControlSettingsForRelationship(relationshipConfig.getPlugin(RelationshipsOutboundAccessControlPlugin.class));

            RelationshipRequestSupplier<JsonApiRequest, RESOURCE_DTO> relationshipRequestSupplier
                    = getRelationshipRequestSupplier(relationshipConfigCasted, resourceIdSupplier, executable::validate);

            ResourceTypeAndIdResolver<RELATIONSHIP_DTO> resourceIdentifierTypeAndIdResolver
                    = getResourceIdentifierTypeAndIdResolver(relationshipConfigCasted);

            return new BatchToOneRelationshipProcessor()
                    .accessControlEvaluator(accessControlEvaluator)
                    .inboundAccessControlSettings(inboundAccessControlSettings)
                    .outboundAccessControlSettings(outboundAccessControlSettings)
                    .dataSupplier(executable::readBatches)
                    .resourceIdentifierMetaResolver(relationshipConfigCasted::resolveResourceIdentifierMeta)
                    .topLevelLinksResolver(relationshipConfigCasted::resolveRelationshipLinks)
                    .topLevelMetaResolver(relationshipConfigCasted::resolveRelationshipMeta)
                    .resourceIdentifierTypeAndIdResolver(resourceIdentifierTypeAndIdResolver)
                    .toOneRelationshipDocBatch(request, dataSourceDtos, relationshipRequestSupplier);
        }

        private boolean hasReadToOneRelationshipBatchImplementation(ToOneRelationship<?, ?> relationshipConfig) {
            ReadToOneRelationshipOperation<?, ?> operation
                    = operationsRegistry.getReadToOneRelationshipOperation(resourceType, relationshipConfig.relationshipName(), false);
            return operation instanceof BatchReadToOneRelationshipOperation;
        }

        private <RESOURCE_DTO> RelationshipRequestSupplier<JsonApiRequest, RESOURCE_DTO> getRelationshipRequestSupplier(
                Relationship<RESOURCE_DTO, ?> relationshipConfig,
                IdSupplier<RESOURCE_DTO> resourceIdSupplier,
                Consumer<JsonApiRequest> validator
        ) {
            return (originalRequest, dataSourceDto) -> {
                JsonApiRequest relationshipRequest
                        = relationshipConfig.constructRelationshipRequest(originalRequest, dataSourceDto);
                if (relationshipRequest == null) {
                    relationshipRequest = JsonApiRequest.composeRelationshipRequest(
                            resourceIdSupplier.getId(dataSourceDto),
                            relationshipConfig
                    );
                }

                validator.accept(relationshipRequest);

                return relationshipRequest;
            };
        }

    }

    public class ToManyRelationshipsOperationStepSelected {

        private final ResourceType resourceType;
        private final RelationshipName relationshipName;

        public ToManyRelationshipsOperationStepSelected(ResourceType resourceType,
                                                        RelationshipName relationshipName) {
            this.resourceType = resourceType;
            this.relationshipName = relationshipName;
        }

        public <RELATIONSHIP_DTO> ToManyRelationshipsDoc readToManyRelationship(JsonApiRequest relationshipRequest) {
            ToManyRelationship<?, ?> relationshipConfig
                    = domainRegistry.getToManyRelationshipStrict(resourceType, relationshipName);

            @SuppressWarnings("unchecked") ReadToManyRelationshipOperation<?, RELATIONSHIP_DTO> executable =
                    (ReadToManyRelationshipOperation<?, RELATIONSHIP_DTO>) operationsRegistry.getReadToManyDataRelationshipOperation(
                            resourceType,
                            relationshipConfig.relationshipName(),
                            true
                    );

            executable.validate(relationshipRequest);
            return resolveToManyRelationshipsDocCommon(
                    resourceType,
                    relationshipName,
                    relationshipRequest,
                    (MultipleDataItemsSupplier<JsonApiRequest, RELATIONSHIP_DTO>) executable::read,
                    executable.getPlugin(OperationInboundAccessControlPlugin.class)
            );
        }

        public void updateToManyRelationship(JsonApiRequest relationshipRequest) {
            ToManyRelationship<?, ?> relationshipConfig
                    = domainRegistry.getToManyRelationshipStrict(resourceType, relationshipName);

            UpdateToManyRelationshipOperation executable
                    = operationsRegistry.getUpdateToManyRelationshipOperation(resourceType, relationshipConfig.relationshipName(), true);

            executable.validate(relationshipRequest);

            executeCastedNoResponse(
                    relationshipRequest,
                    executable::update,
                    executable.getPlugin(OperationInboundAccessControlPlugin.class)
            );
        }

        private void executeCastedNoResponse(
                JsonApiRequest relationshipRequest,
                Consumer<JsonApiRequest> executable,
                OperationInboundAccessControlPlugin operationAcPlugin
        ) {
            resolveToManyRelationshipsDocCommon(
                    resourceType,
                    relationshipName,
                    relationshipRequest,
                    req -> {
                        executable.accept(req);
                        return null;
                    },
                    operationAcPlugin
            );
        }
    }

    public class ToOneRelationshipOperationStepSelected {

        private final ResourceType resourceType;
        private final RelationshipName relationshipName;

        public ToOneRelationshipOperationStepSelected(ResourceType resourceType,
                                                      RelationshipName relationshipName) {
            this.resourceType = resourceType;
            this.relationshipName = relationshipName;
        }

        public <RELATIONSHIP_DTO> ToOneRelationshipDoc readToOneRelationship(JsonApiRequest relationshipRequest) {
            ToOneRelationship<?, ?> relationshipConfig
                    = domainRegistry.getToOneRelationshipStrict(resourceType, relationshipName);

            @SuppressWarnings("unchecked")
            ReadToOneRelationshipOperation<?, RELATIONSHIP_DTO> executable
                    = (ReadToOneRelationshipOperation<?, RELATIONSHIP_DTO>) operationsRegistry.getReadToOneRelationshipOperation(resourceType, relationshipConfig.relationshipName(), true);

            executable.validate(relationshipRequest);

            return resolveToOneRelationshipDocCommon(
                    resourceType,
                    relationshipName,
                    relationshipRequest,
                    (SingleDataItemSupplier<JsonApiRequest, RELATIONSHIP_DTO>) executable::read,
                    executable.getPlugin(OperationInboundAccessControlPlugin.class)
            );
        }

        public void updateToOneRelationship(JsonApiRequest relationshipRequest) {

            ToOneRelationship<?, ?> relationshipConfig
                    = domainRegistry.getToOneRelationshipStrict(resourceType, relationshipName);

            UpdateToOneRelationshipOperation executable
                    = operationsRegistry.getUpdateToOneRelationshipOperation(resourceType, relationshipConfig.relationshipName(), true);

            executable.validate(relationshipRequest);

            executeCastedNoResponse(
                    relationshipRequest,
                    executable::update,
                    executable.getPlugin(OperationInboundAccessControlPlugin.class)
            );
        }

        private void executeCastedNoResponse(
                JsonApiRequest relationshipRequest,
                Consumer<JsonApiRequest> executable,
                OperationInboundAccessControlPlugin operationAcPlugin
        ) {
            resolveToOneRelationshipDocCommon(
                    resourceType,
                    relationshipName,
                    relationshipRequest,
                    req -> {
                        executable.accept(req);
                        return null;
                    },
                    operationAcPlugin
            );
        }
    }

    public void setAccessControlEvaluator(AccessControlEvaluator accessControlEvaluator) {
        this.accessControlEvaluator = accessControlEvaluator;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }
}
