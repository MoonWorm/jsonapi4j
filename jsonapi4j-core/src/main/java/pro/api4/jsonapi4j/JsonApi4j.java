package pro.api4.jsonapi4j;

import lombok.*;
import pro.api4.jsonapi4j.plugin.ac.impl.AccessControlEvaluator;
import pro.api4.jsonapi4j.plugin.utils.ReflectionUtils;
import pro.api4.jsonapi4j.plugin.ac.impl.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.impl.model.AccessControlModel;
import pro.api4.jsonapi4j.plugin.ac.impl.model.outbound.OutboundAccessControlForJsonApiResource;
import pro.api4.jsonapi4j.plugin.ac.impl.model.outbound.OutboundAccessControlForJsonApiResourceIdentifier;
import pro.api4.jsonapi4j.plugin.ac.JsonApiAccessControlPlugin;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.Relationship;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.domain.ToManyRelationship;
import pro.api4.jsonapi4j.domain.ToOneRelationship;
import pro.api4.jsonapi4j.model.document.data.MultipleResourcesDoc;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import pro.api4.jsonapi4j.operation.*;
import pro.api4.jsonapi4j.operation.exception.OperationNotFoundException;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.processor.IdAndType;
import pro.api4.jsonapi4j.processor.IdSupplier;
import pro.api4.jsonapi4j.processor.ResourceProcessorContext;
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
import static org.apache.commons.collections4.MapUtils.emptyIfNull;

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
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class JsonApi4j {

    private final DomainRegistry domainRegistry;
    private final OperationsRegistry operationsRegistry;
    @With
    @Builder.Default
    private AccessControlEvaluator accessControlEvaluator = AccessControlEvaluator.createDefault();
    @With
    @Builder.Default
    private Executor executor = ResourceProcessorContext.DEFAULT_EXECUTOR;

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

    private <RELATIONSHIP_DTO> ToOneRelationshipDoc resolveToOneRelationshipDocCommon(
            ResourceType resourceType,
            RelationshipName relationshipName,
            JsonApiRequest relationshipRequest,
            SingleDataItemSupplier<JsonApiRequest, ?> executable,
            AccessControlModel inboundAccessControlSettings
    ) {

        @SuppressWarnings("unchecked")
        ToOneRelationship<?, RELATIONSHIP_DTO> relationshipConfig
                = (ToOneRelationship<?, RELATIONSHIP_DTO>) domainRegistry.getToOneRelationshipStrict(resourceType, relationshipName);

        @SuppressWarnings("unchecked")
        SingleDataItemSupplier<JsonApiRequest, RELATIONSHIP_DTO> executableCasted
                = (SingleDataItemSupplier<JsonApiRequest, RELATIONSHIP_DTO>) executable;

        OutboundAccessControlForJsonApiResourceIdentifier outboundAccessControlSettings
                = getOutboundAccessControlSettingsForRelationship(relationshipConfig);

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
            AccessControlModel inboundAccessControlSettings
    ) {

        @SuppressWarnings("unchecked")
        ToManyRelationship<?, RELATIONSHIP_DTO> relationshipConfig
                = (ToManyRelationship<?, RELATIONSHIP_DTO>) domainRegistry.getToManyRelationshipStrict(resourceType, relationshipName);

        @SuppressWarnings("unchecked")
        MultipleDataItemsSupplier<JsonApiRequest, RELATIONSHIP_DTO> executableCasted
                = (MultipleDataItemsSupplier<JsonApiRequest, RELATIONSHIP_DTO>) executable;

        OutboundAccessControlForJsonApiResourceIdentifier outboundAccessControlSettings
                = getOutboundAccessControlSettingsForRelationship(relationshipConfig);

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

    private AccessControlModel getInboundAccessControlSettings(RegisteredOperation<?> registeredOperation) {
        Object accessControlModel = emptyIfNull(registeredOperation.getPluginInfo()).get(JsonApiAccessControlPlugin.NAME);
        if (accessControlModel instanceof AccessControlModel acm) {
            return acm;
        }
        return null;
    }

    private OutboundAccessControlForJsonApiResourceIdentifier getOutboundAccessControlSettingsForRelationship(
            Relationship<?, ?> relationshipConfig
    ) {
        AccessControlModel resourceIdentifierClassLevel = AccessControlModel.fromClassAnnotation(
                relationshipConfig.getClass()
        );

        AccessControlModel resourceIdentifierMetaFieldLevel = AccessControlModel.fromAnnotation(
                ReflectionUtils.fetchAnnotationForMethod(
                        relationshipConfig.getClass(),
                        "resolveResourceIdentifierMeta",
                        AccessControl.class
                )
        );

        return OutboundAccessControlForJsonApiResourceIdentifier.builder()
                .resourceIdentifierClassLevel(resourceIdentifierClassLevel)
                .resourceIdentifierMetaFieldLevel(resourceIdentifierMetaFieldLevel)
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
            Resource<RESOURCE_DTO> resourceConfig) {
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
            RegisteredOperation<ReadResourceByIdOperation<?>> registeredOperation
                    = operationsRegistry.getRegisteredReadResourceByIdOperation(resourceType, true);

            @SuppressWarnings("unchecked")
            ReadResourceByIdOperation<RESOURCE_DTO> executable
                    = (ReadResourceByIdOperation<RESOURCE_DTO>) registeredOperation.getOperation();
            executable.validate(request);

            return processSingleResource(
                    request,
                    executable::readById,
                    getInboundAccessControlSettings(registeredOperation)
            );
        }

        public <RESOURCE_DTO> SingleResourceDoc<?> createResource(JsonApiRequest request) {
            RegisteredOperation<CreateResourceOperation<?>> registeredOperation
                    = operationsRegistry.getRegisteredCreateResourceOperation(resourceType, true);

            @SuppressWarnings("unchecked")
            CreateResourceOperation<RESOURCE_DTO> executable
                    = (CreateResourceOperation<RESOURCE_DTO>) registeredOperation.getOperation();

            executable.validate(request);

            return processSingleResource(
                    request,
                    executable::create,
                    getInboundAccessControlSettings(registeredOperation)
            );
        }

        public void updateResource(JsonApiRequest request) {
            RegisteredOperation<UpdateResourceOperation> registeredOperation
                    = operationsRegistry.getRegisteredUpdateResourceOperation(resourceType, true);

            UpdateResourceOperation executable = registeredOperation.getOperation();
            executable.validate(request);
            processSingleResourceNoResponse(
                    request,
                    executable::update,
                    getInboundAccessControlSettings(registeredOperation)
            );
        }

        public void deleteResource(JsonApiRequest request) {
            RegisteredOperation<DeleteResourceOperation> registeredOperation
                    = operationsRegistry.getRegisteredDeleteResourceOperation(resourceType, true);

            DeleteResourceOperation executable = registeredOperation.getOperation();

            executable.validate(request);

            processSingleResourceNoResponse(
                    request,
                    executable::delete,
                    getInboundAccessControlSettings(registeredOperation)
            );
        }

        private <RESOURCE_DTO> SingleResourceDoc<?> processSingleResource(
                JsonApiRequest request,
                SingleDataItemSupplier<JsonApiRequest, RESOURCE_DTO> dataSupplier,
                AccessControlModel inboundAccessControlSettings
        ) {
            @SuppressWarnings("unchecked")
            Resource<RESOURCE_DTO> resourceConfig
                    = (Resource<RESOURCE_DTO>) domainRegistry.getResource(resourceType);

            return new SingleResourceProcessor()
                    .forRequest(request)
                    .concurrentRelationshipResolution(executor)
                    .accessControlEvaluator(accessControlEvaluator)
                    .inboundAccessControlSettings(inboundAccessControlSettings)
                    .outboundAccessControlSettings(getOutboundRequirementsForResourceOperation(resourceConfig))
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
                AccessControlModel inboundAccessControlSettings
        ) {
            processSingleResource(request,
                    req -> {
                        executable.accept(req);
                        return null;
                    },
                    inboundAccessControlSettings
            );
        }

        public <RESOURCE_DTO> MultipleResourcesDoc<?> readMultipleResources(JsonApiRequest request) {
            RegisteredOperation<ReadMultipleResourcesOperation<?>> registeredReadMultipleOperation
                    = operationsRegistry.getRegisteredReadMultipleResourcesOperation(resourceType, false);
            if (registeredReadMultipleOperation != null) {
                @SuppressWarnings("unchecked")
                ReadMultipleResourcesOperation<RESOURCE_DTO> readAllExecutable
                        = (ReadMultipleResourcesOperation<RESOURCE_DTO>) registeredReadMultipleOperation.getOperation();
                readAllExecutable.validate(request);
                return processMultipleResources(
                        request,
                        readAllExecutable::readPage,
                        getInboundAccessControlSettings(registeredReadMultipleOperation)
                );
            } else if (request.getFilters().size() == 1
                    && request.getFilters().containsKey(ReadMultipleResourcesOperation.ID_FILTER_NAME)) {
                RegisteredOperation<ReadResourceByIdOperation<?>> registeredReadByIdOperation
                        = operationsRegistry.getRegisteredReadResourceByIdOperation(resourceType, false);

                @SuppressWarnings("unchecked")
                ReadResourceByIdOperation<RESOURCE_DTO> readByIdExecutable
                        = (ReadResourceByIdOperation<RESOURCE_DTO>) registeredReadByIdOperation.getOperation();
                if (readByIdExecutable != null) {
                    ReadMultipleResourcesOperation<RESOURCE_DTO> mimickedReadAllExecutable = mimicReadMultipleResourcesOperationViaSequentialReadByIds(readByIdExecutable);
                    mimickedReadAllExecutable.validate(request);
                    return processMultipleResources(
                            request,
                            mimickedReadAllExecutable::readPage,
                            getInboundAccessControlSettings(registeredReadByIdOperation)
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

        private <DATA_SOURCE_DTO> MultipleResourcesDoc<?> processMultipleResources(
                JsonApiRequest request,
                MultipleDataItemsSupplier<JsonApiRequest, DATA_SOURCE_DTO> dataSupplier,
                AccessControlModel inboundAccessControlSettings
        ) {
            @SuppressWarnings("unchecked")
            Resource<DATA_SOURCE_DTO> resourceConfig
                    = (Resource<DATA_SOURCE_DTO>) domainRegistry.getResource(resourceType);
            return new MultipleResourcesProcessor()
                    .forRequest(request)
                    .concurrentRelationshipResolution(executor)
                    .accessControlEvaluator(accessControlEvaluator)
                    .inboundAccessControlSettings(inboundAccessControlSettings)
                    .outboundAccessControlSettings(getOutboundRequirementsForResourceOperation(resourceConfig))
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

        private OutboundAccessControlForJsonApiResource getOutboundRequirementsForResourceOperation(
                Resource<?> resourceConfig
        ) {
            AccessControlModel resourceClassLevel = AccessControlModel.fromClassAnnotation(resourceConfig.getClass());

            AccessControlModel resourceAttributesFieldLevel = AccessControlModel.fromAnnotation(
                    ReflectionUtils.fetchAnnotationForMethod(
                            resourceConfig.getClass(),
                            "resolveAttributes",
                            AccessControl.class
                    )
            );

            AccessControlModel resourceLinksFieldLevel = AccessControlModel.fromAnnotation(
                    ReflectionUtils.fetchAnnotationForMethod(
                            resourceConfig.getClass(),
                            "resolveResourceLinks",
                            AccessControl.class
                    )
            );

            AccessControlModel resourceMetaFieldLevel = AccessControlModel.fromAnnotation(
                    ReflectionUtils.fetchAnnotationForMethod(
                            resourceConfig.getClass(),
                            "resolveResourceMeta",
                            AccessControl.class
                    )
            );

            return OutboundAccessControlForJsonApiResource.builder()
                    .resourceClassLevel(resourceClassLevel)
                    .resourceAttributesFieldLevel(resourceAttributesFieldLevel)
                    .resourceLinksFieldLevel(resourceLinksFieldLevel)
                    .resourceMetaFieldLevel(resourceMetaFieldLevel)
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

            RegisteredOperation<ReadToManyRelationshipOperation<?, ?>> registeredOperation
                    = operationsRegistry.getRegisteredReadToManyRelationshipOperation(resourceType, relationshipName, true);

            @SuppressWarnings("unchecked")
            ReadToManyRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO> executable
                    = (ReadToManyRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO>) registeredOperation.getOperation();

            JsonApiRequest relationshipRequest
                    = getRelationshipRequestSupplier(relationshipConfigCasted, resourceIdSupplier, executable::validate).create(parentRequest, dataSourceDto);

            MultipleDataItemsSupplier<JsonApiRequest, RELATIONSHIP_DTO> dataSupplier
                    = relRequest -> executable.readManyForResource(relRequest, dataSourceDto);

            return resolveToManyRelationshipsDocCommon(
                    resourceType,
                    relationshipName,
                    relationshipRequest,
                    dataSupplier,
                    getInboundAccessControlSettings(registeredOperation)
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

            RegisteredOperation<ReadToManyRelationshipOperation<?, ?>> registeredOperation
                    = operationsRegistry.getRegisteredReadToManyRelationshipOperation(resourceType, relationshipConfig.relationshipName(), true);

            @SuppressWarnings("unchecked")
            BatchReadToManyRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO> executable
                    = (BatchReadToManyRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO>) registeredOperation.getOperation();

            AccessControlModel inboundAccessControlSettings
                    = getInboundAccessControlSettings(registeredOperation);

            OutboundAccessControlForJsonApiResourceIdentifier outboundAccessControlSettings
                    = getOutboundAccessControlSettingsForRelationship(relationshipConfig);

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
                    = operationsRegistry.getReadToManyRelationshipOperation(resourceType, relationshipConfig.relationshipName(), false);
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

            RegisteredOperation<ReadToOneRelationshipOperation<?, ?>> registeredOperation
                    = operationsRegistry.getRegisteredReadToOneRelationshipOperation(resourceType, relationshipName, true);

            @SuppressWarnings("unchecked")
            ReadToOneRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO> executable
                    = (ReadToOneRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO>) registeredOperation.getOperation();

            JsonApiRequest relationshipRequest
                    = getRelationshipRequestSupplier(relationshipConfig, resourceIdSupplier, executable::validate).create(parentRequest, dataSourceDto);

            SingleDataItemSupplier<JsonApiRequest, RELATIONSHIP_DTO> dataSupplier
                    = relRequest -> executable.readOneForResource(relRequest, dataSourceDto);

            return resolveToOneRelationshipDocCommon(
                    resourceType,
                    relationshipName,
                    relationshipRequest,
                    dataSupplier,
                    getInboundAccessControlSettings(registeredOperation)
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

            RegisteredOperation<ReadToOneRelationshipOperation<?, ?>> registeredOperation
                    = operationsRegistry.getRegisteredReadToOneRelationshipOperation(resourceType, relationshipConfig.relationshipName(), true);

            @SuppressWarnings("unchecked")
            BatchReadToOneRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO> executable
                    = (BatchReadToOneRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO>) registeredOperation.getOperation();

            AccessControlModel inboundAccessControlSettings
                    = getInboundAccessControlSettings(registeredOperation);

            OutboundAccessControlForJsonApiResourceIdentifier outboundAccessControlSettings
                    = getOutboundAccessControlSettingsForRelationship(relationshipConfig);

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
            RegisteredOperation<ReadToManyRelationshipOperation<?, ?>> registeredOperation
                    = operationsRegistry.getRegisteredReadToManyRelationshipOperation(resourceType, relationshipName, true);

            @SuppressWarnings("unchecked") ReadToManyRelationshipOperation<?, RELATIONSHIP_DTO> executable =
                    (ReadToManyRelationshipOperation<?, RELATIONSHIP_DTO>) registeredOperation.getOperation();

            executable.validate(relationshipRequest);
            return resolveToManyRelationshipsDocCommon(
                    resourceType,
                    relationshipName,
                    relationshipRequest,
                    (MultipleDataItemsSupplier<JsonApiRequest, RELATIONSHIP_DTO>) executable::readMany,
                    getInboundAccessControlSettings(registeredOperation)
            );
        }

        public void updateToManyRelationship(JsonApiRequest relationshipRequest) {
            RegisteredOperation<UpdateToManyRelationshipOperation> registeredOperation
                    = operationsRegistry.getRegisteredUpdateToManyRelationshipOperation(resourceType, relationshipName, true);

            UpdateToManyRelationshipOperation executable
                    = registeredOperation.getOperation();

            executable.validate(relationshipRequest);

            executeCastedNoResponse(
                    relationshipRequest,
                    executable::update,
                    getInboundAccessControlSettings(registeredOperation)
            );
        }

        private void executeCastedNoResponse(
                JsonApiRequest relationshipRequest,
                Consumer<JsonApiRequest> executable,
                AccessControlModel inboundAccessControlSettings
        ) {
            resolveToManyRelationshipsDocCommon(
                    resourceType,
                    relationshipName,
                    relationshipRequest,
                    req -> {
                        executable.accept(req);
                        return null;
                    },
                    inboundAccessControlSettings
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
            RegisteredOperation<ReadToOneRelationshipOperation<?, ?>> registeredOperation
                    = operationsRegistry.getRegisteredReadToOneRelationshipOperation(resourceType, relationshipName, true);

            @SuppressWarnings("unchecked")
            ReadToOneRelationshipOperation<?, RELATIONSHIP_DTO> executable
                    = (ReadToOneRelationshipOperation<?, RELATIONSHIP_DTO>) registeredOperation.getOperation();

            executable.validate(relationshipRequest);

            return resolveToOneRelationshipDocCommon(
                    resourceType,
                    relationshipName,
                    relationshipRequest,
                    (SingleDataItemSupplier<JsonApiRequest, RELATIONSHIP_DTO>) executable::readOne,
                    getInboundAccessControlSettings(registeredOperation)
            );
        }

        public void updateToOneRelationship(JsonApiRequest relationshipRequest) {
            RegisteredOperation<UpdateToOneRelationshipOperation> registeredOperation
                    = operationsRegistry.getRegisteredUpdateToOneRelationshipOperation(resourceType, relationshipName, true);

            UpdateToOneRelationshipOperation executable
                    = registeredOperation.getOperation();

            executable.validate(relationshipRequest);

            executeCastedNoResponse(
                    relationshipRequest,
                    executable::update,
                    getInboundAccessControlSettings(registeredOperation)
            );
        }

        private void executeCastedNoResponse(
                JsonApiRequest relationshipRequest,
                Consumer<JsonApiRequest> executable,
                AccessControlModel inboundAccessControlSettings
        ) {
            resolveToOneRelationshipDocCommon(
                    resourceType,
                    relationshipName,
                    relationshipRequest,
                    req -> {
                        executable.accept(req);
                        return null;
                    },
                    inboundAccessControlSettings
            );
        }
    }
}
