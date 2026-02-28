package pro.api4.jsonapi4j;

import lombok.*;
import pro.api4.jsonapi4j.domain.*;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.MultipleResourcesDoc;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import pro.api4.jsonapi4j.operation.*;
import pro.api4.jsonapi4j.operation.exception.OperationNotFoundException;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.plugin.JsonApiPluginInfo;
import pro.api4.jsonapi4j.plugin.PluginSettings;
import pro.api4.jsonapi4j.processor.*;
import pro.api4.jsonapi4j.processor.multi.MultipleDataItemsSupplier;
import pro.api4.jsonapi4j.processor.multi.relationship.ToManyRelationshipsProcessor;
import pro.api4.jsonapi4j.processor.multi.resource.MultipleResourcesProcessor;
import pro.api4.jsonapi4j.processor.resolvers.*;
import pro.api4.jsonapi4j.processor.resolvers.links.resource.ResourceLinksDefaultResolvers;
import pro.api4.jsonapi4j.processor.resolvers.links.toplevel.MultiResourcesDocLinksDefaultResolvers;
import pro.api4.jsonapi4j.processor.resolvers.links.toplevel.SingleResourceDocLinksDefaultResolvers;
import pro.api4.jsonapi4j.processor.resolvers.links.toplevel.ToManyRelationshipLinksDefaultResolvers;
import pro.api4.jsonapi4j.processor.resolvers.links.toplevel.ToOneRelationshipLinksDefaultResolvers;
import pro.api4.jsonapi4j.processor.resolvers.relationships.DefaultRelationshipResolvers;
import pro.api4.jsonapi4j.processor.single.SingleDataItemSupplier;
import pro.api4.jsonapi4j.processor.single.relationship.ToOneRelationshipProcessor;
import pro.api4.jsonapi4j.processor.single.resource.SingleResourceProcessor;
import pro.api4.jsonapi4j.request.DefaultJsonApiRequest;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.response.CursorPageableResponse;

import java.util.*;
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
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class JsonApi4j {
    @Builder.Default
    private DomainRegistry domainRegistry = DomainRegistry.empty();
    @Builder.Default
    private OperationsRegistry operationsRegistry = OperationsRegistry.empty();
    @Builder.Default
    private List<JsonApi4jPlugin> plugins = Collections.emptyList();
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
            List<PluginSettings> pluginSettings
    ) {
        RegisteredRelationship<ToOneRelationship<?>> registeredRelationship
                = domainRegistry.getToOneRelationshipStrict(resourceType, relationshipName);

        @SuppressWarnings("unchecked")
        ToOneRelationship<RELATIONSHIP_DTO> toOneRelationshipCasted
                = (ToOneRelationship<RELATIONSHIP_DTO>) registeredRelationship.getRelationship();

        @SuppressWarnings("unchecked")
        SingleDataItemSupplier<JsonApiRequest, RELATIONSHIP_DTO> executableCasted
                = (SingleDataItemSupplier<JsonApiRequest, RELATIONSHIP_DTO>) executable;

        ResourceTypeAndIdResolver<RELATIONSHIP_DTO> typeAndIdResolver
                = getResourceIdentifierTypeAndIdResolver(toOneRelationshipCasted);

        return new ToOneRelationshipProcessor()
                .forRequest(relationshipRequest)
                .plugins(pluginSettings)
                .dataSupplier(executableCasted)
                .topLevelLinksResolver(getSingleDataTopLevelLinksResolver(toOneRelationshipCasted, resourceType, relationshipName))
                .topLevelMetaResolver(toOneRelationshipCasted::resolveRelationshipMeta)
                .resourceIdentifierMetaResolver(toOneRelationshipCasted::resolveResourceIdentifierMeta)
                .resourceTypeAndIdSupplier(typeAndIdResolver)
                .toToOneRelationshipDoc();
    }

    private <RELATIONSHIP_DTO> SingleDataItemDocLinksResolver<JsonApiRequest, RELATIONSHIP_DTO> getSingleDataTopLevelLinksResolver(
            ToOneRelationship<RELATIONSHIP_DTO> toOneRelationshipCasted,
            ResourceType resourceType,
            RelationshipName relationshipName
    ) {
        return (req, dto) -> {
            LinksObject linksObject = toOneRelationshipCasted.resolveRelationshipLinks(req, dto);
            if (linksObject == Relationship.NOT_IMPLEMENTED_LINKS_STUB) {
                return ToOneRelationshipLinksDefaultResolvers.defaultLinksResolver(
                        resourceType,
                        req.getResourceId(),
                        relationshipName,
                        resourceTypeStr -> new ResourceType(toOneRelationshipCasted.resolveResourceIdentifierType(resourceTypeStr)),
                        toOneRelationshipCasted::resolveResourceIdentifierId
                ).resolve(req, dto);
            }
            return linksObject;
        };
    }

    private <RELATIONSHIP_DTO> ToManyRelationshipsDoc resolveToManyRelationshipsDocCommon(
            ResourceType resourceType,
            RelationshipName relationshipName,
            JsonApiRequest relationshipRequest,
            MultipleDataItemsSupplier<JsonApiRequest, ?> executable,
            List<PluginSettings> pluginSettings
    ) {
        RegisteredRelationship<ToManyRelationship<?>> registeredRelationship =
                domainRegistry.getToManyRelationshipStrict(resourceType, relationshipName);

        @SuppressWarnings("unchecked")
        ToManyRelationship<RELATIONSHIP_DTO> toManyRelationshipCasted
                = (ToManyRelationship<RELATIONSHIP_DTO>) registeredRelationship.getRelationship();

        @SuppressWarnings("unchecked")
        MultipleDataItemsSupplier<JsonApiRequest, RELATIONSHIP_DTO> executableCasted
                = (MultipleDataItemsSupplier<JsonApiRequest, RELATIONSHIP_DTO>) executable;

        ResourceTypeAndIdResolver<RELATIONSHIP_DTO> typeAndIdResolver
                = getResourceIdentifierTypeAndIdResolver(toManyRelationshipCasted);

        return new ToManyRelationshipsProcessor()
                .forRequest(relationshipRequest)
                .plugins(pluginSettings)
                .dataSupplier(executableCasted)
                .resourceIdentifierMetaResolver(toManyRelationshipCasted::resolveResourceIdentifierMeta)
                .topLevelLinksResolver(getMultipleDataItemsTopLevelLinksResolver(toManyRelationshipCasted, resourceType, relationshipName))
                .topLevelMetaResolver(toManyRelationshipCasted::resolveRelationshipMeta)
                .resourceTypeAndIdSupplier(typeAndIdResolver)
                .toToManyRelationshipsDoc();
    }

    private <RELATIONSHIP_DTO> MultipleDataItemsDocLinksResolver<JsonApiRequest, RELATIONSHIP_DTO> getMultipleDataItemsTopLevelLinksResolver(
            ToManyRelationship<RELATIONSHIP_DTO> toManyRelationshipCasted,
            ResourceType resourceType,
            RelationshipName relationshipName
    ) {
        return (req, dtos, nextCursor) -> {
            LinksObject linksObject = toManyRelationshipCasted.resolveRelationshipLinks(req, dtos, nextCursor);
            if (linksObject == Relationship.NOT_IMPLEMENTED_LINKS_STUB) {
                return ToManyRelationshipLinksDefaultResolvers.defaultLinksResolver(
                        resourceType,
                        req.getResourceId(),
                        relationshipName,
                        resourceTypeStr -> new ResourceType(toManyRelationshipCasted.resolveResourceIdentifierType(resourceTypeStr)),
                        toManyRelationshipCasted::resolveResourceIdentifierId
                ).resolve(req, dtos, nextCursor);
            }
            return linksObject;
        };
    }

    private <RELATIONSHIP_DTO> ResourceTypeAndIdResolver<RELATIONSHIP_DTO> getResourceIdentifierTypeAndIdResolver(
            Relationship<RELATIONSHIP_DTO> relationship) {
        return dto -> new IdAndType(
                relationship.resolveResourceIdentifierId(dto),
                new ResourceType(relationship.resolveResourceIdentifierType(dto))
        );
    }

    private List<PluginSettings> getPluginSettings(RegisteredOperation<?> registeredOperation,
                                                   RegisteredResource<?> registeredResource) {
        return getPluginSettings(registeredOperation, registeredResource, null);
    }


    private List<PluginSettings> getPluginSettings(
            RegisteredOperation<?> registeredOperation,
            RegisteredResource<?> registeredResource,
            RegisteredRelationship<?> registeredRelationship) {
        List<PluginSettings> result = new ArrayList<>();
        for (JsonApi4jPlugin plugin : plugins) {
            JsonApiPluginInfo info = new JsonApiPluginInfo(
                    registeredOperation.getPluginInfo().get(plugin.pluginName()),
                    registeredResource.getPluginInfo().get(plugin.pluginName()),
                    registeredRelationship != null ? registeredRelationship.getPluginInfo().get(plugin.pluginName()) : null
            );
            result.add(PluginSettings.builder().plugin(plugin).info(info).build());
        }
        return result.stream().sorted(Comparator.comparingInt(p -> p.getPlugin().precedence())).toList();
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

            RegisteredResource<Resource<?>> registeredResource = domainRegistry.getResource(resourceType);

            @SuppressWarnings("unchecked")
            Resource<RESOURCE_DTO> resourceConfig = (Resource<RESOURCE_DTO>) registeredResource.getResource();

            List<PluginSettings> pluginSettings = getPluginSettings(
                    registeredOperation,
                    registeredResource
            );

            return processSingleResource(
                    request,
                    resourceConfig,
                    executable::readById,
                    pluginSettings
            );
        }

        public <RESOURCE_DTO> SingleResourceDoc<?> createResource(JsonApiRequest request) {
            RegisteredOperation<CreateResourceOperation<?>> registeredOperation
                    = operationsRegistry.getRegisteredCreateResourceOperation(resourceType, true);

            @SuppressWarnings("unchecked")
            CreateResourceOperation<RESOURCE_DTO> executable
                    = (CreateResourceOperation<RESOURCE_DTO>) registeredOperation.getOperation();

            executable.validate(request);

            RegisteredResource<Resource<?>> registeredResource = domainRegistry.getResource(resourceType);

            @SuppressWarnings("unchecked")
            Resource<RESOURCE_DTO> resourceConfig = (Resource<RESOURCE_DTO>) registeredResource.getResource();

            List<PluginSettings> pluginSettings = getPluginSettings(
                    registeredOperation,
                    registeredResource
            );

            return processSingleResource(
                    request,
                    resourceConfig,
                    executable::create,
                    pluginSettings
            );
        }

        public void updateResource(JsonApiRequest request) {
            RegisteredOperation<UpdateResourceOperation> registeredOperation
                    = operationsRegistry.getRegisteredUpdateResourceOperation(resourceType, true);

            UpdateResourceOperation executable = registeredOperation.getOperation();
            executable.validate(request);

            RegisteredResource<Resource<?>> registeredResource = domainRegistry.getResource(resourceType);

            Resource<?> resourceConfig = registeredResource.getResource();

            List<PluginSettings> pluginSettings = getPluginSettings(
                    registeredOperation,
                    registeredResource
            );

            processSingleResourceNoResponse(
                    request,
                    resourceConfig,
                    executable::update,
                    pluginSettings
            );
        }

        public void deleteResource(JsonApiRequest request) {
            RegisteredOperation<DeleteResourceOperation> registeredOperation
                    = operationsRegistry.getRegisteredDeleteResourceOperation(resourceType, true);

            DeleteResourceOperation executable = registeredOperation.getOperation();

            executable.validate(request);

            RegisteredResource<Resource<?>> registeredResource = domainRegistry.getResource(resourceType);

            Resource<?> resourceConfig = registeredResource.getResource();

            List<PluginSettings> pluginSettings = getPluginSettings(
                    registeredOperation,
                    registeredResource
            );

            processSingleResourceNoResponse(
                    request,
                    resourceConfig,
                    executable::delete,
                    pluginSettings
            );
        }

        private <RESOURCE_DTO> SingleResourceDoc<?> processSingleResource(
                JsonApiRequest request,
                Resource<RESOURCE_DTO> resourceConfig,
                SingleDataItemSupplier<JsonApiRequest, RESOURCE_DTO> dataSupplier,
                List<PluginSettings> pluginSettings
        ) {
            return new SingleResourceProcessor()
                    .forRequest(request)
                    .concurrentRelationshipResolution(executor)
                    .plugins(pluginSettings)
                    .dataSupplier(dataSupplier)
                    .defaultRelationships(getDefaultRelationshipResolvers(resourceConfig::resolveResourceId))
                    .toManyRelationshipResolvers(getToManyRelationshipsResolvers(resourceConfig::resolveResourceId))
                    .batchToManyRelationshipResolvers(getBatchToManyRelationshipResolvers(resourceConfig::resolveResourceId))
                    .toOneRelationshipResolvers(getToOneRelationshipResolvers(resourceConfig::resolveResourceId))
                    .batchToOneRelationshipResolvers(getBatchToOneRelationshipResolvers(resourceConfig::resolveResourceId))
                    .topLevelLinksResolver(getSingleDataItemTopLevelLinksResolver(resourceType, resourceConfig))
                    .topLevelMetaResolver(resourceConfig::resolveTopLevelMetaForSingleResourceDoc)
                    .resourceLinksResolver(getResourceLinksResolver(resourceType, resourceConfig))
                    .resourceMetaResolver(resourceConfig::resolveResourceMeta)
                    .attributesResolver(resourceConfig::resolveAttributes)
                    .resourceTypeAndIdResolver(getResourceTypeAndIdResolver(resourceConfig))
                    .toSingleResourceDoc();
        }

        private <RESOURCE_DTO> SingleDataItemDocLinksResolver<JsonApiRequest, RESOURCE_DTO> getSingleDataItemTopLevelLinksResolver(
                ResourceType resourceType,
                Resource<RESOURCE_DTO> resourceConfig
        ) {
            return (req, dto) -> {
                LinksObject linksObject = resourceConfig.resolveTopLevelLinksForSingleResourceDoc(req, dto);
                if (linksObject == Resource.NOT_IMPLEMENTED_LINKS_STUB) {
                    String id = req.getResourceId();
                    return SingleResourceDocLinksDefaultResolvers.<JsonApiRequest, RESOURCE_DTO>defaultTopLevelLinksResolver(
                            resourceType,
                            d -> id
                    ).resolve(req, dto);
                }
                return linksObject;
            };
        }

        private void processSingleResourceNoResponse(
                JsonApiRequest request,
                Resource<?> resourceConfig,
                Consumer<JsonApiRequest> executable,
                List<PluginSettings> pluginSettings
        ) {
            processSingleResource(
                    request,
                    resourceConfig,
                    req -> {
                        executable.accept(req);
                        return null;
                    },
                    pluginSettings
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
                List<PluginSettings> pluginSettings = getPluginSettings(
                        registeredReadMultipleOperation,
                        domainRegistry.getResource(resourceType)
                );
                return processMultipleResources(
                        request,
                        readAllExecutable::readPage,
                        pluginSettings
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
                    List<PluginSettings> pluginSettings = getPluginSettings(
                            registeredReadByIdOperation,
                            domainRegistry.getResource(resourceType)
                    );
                    return processMultipleResources(
                            request,
                            mimickedReadAllExecutable::readPage,
                            pluginSettings
                    );
                }
            }
            throw new OperationNotFoundException(OperationType.READ_MULTIPLE_RESOURCES, resourceType);
        }

        private <RESOURCE_DTO> ReadMultipleResourcesOperation<RESOURCE_DTO> mimicReadMultipleResourcesOperationViaSequentialReadByIds(
                ReadResourceByIdOperation<RESOURCE_DTO> readByIdExecutable
        ) {
            return request -> {
                List<RESOURCE_DTO> result = request.getFilters().get(ReadMultipleResourcesOperation.ID_FILTER_NAME).stream().map(id -> {
                    JsonApiRequest readByIdRequest = DefaultJsonApiRequest.composeResourceRequest(
                            id,
                            resourceType,
                            OperationType.READ_RESOURCE_BY_ID
                    );
                    return readByIdExecutable.readById(readByIdRequest);
                }).toList();
                return CursorPageableResponse.fromItemsNotPageable(result);
            };
        }

        private <DATA_SOURCE_DTO> MultipleResourcesDoc<?> processMultipleResources(
                JsonApiRequest request,
                MultipleDataItemsSupplier<JsonApiRequest, DATA_SOURCE_DTO> dataSupplier,
                List<PluginSettings> pluginSettings
        ) {
            RegisteredResource<Resource<?>> registeredResource = domainRegistry.getResource(resourceType);
            @SuppressWarnings("unchecked")
            Resource<DATA_SOURCE_DTO> resourceConfig
                    = (Resource<DATA_SOURCE_DTO>) registeredResource.getResource();
            return new MultipleResourcesProcessor()
                    .forRequest(request)
                    .concurrentRelationshipResolution(executor)
                    .plugins(pluginSettings)
                    .dataSupplier(dataSupplier)
                    .defaultRelationships(getDefaultRelationshipResolvers(resourceConfig::resolveResourceId))
                    .toManyRelationshipResolvers(getToManyRelationshipsResolvers(resourceConfig::resolveResourceId))
                    .batchToManyRelationshipResolvers(getBatchToManyRelationshipResolvers(resourceConfig::resolveResourceId))
                    .toOneRelationshipResolvers(getToOneRelationshipResolvers(resourceConfig::resolveResourceId))
                    .batchToOneRelationshipResolvers(getBatchToOneRelationshipResolvers(resourceConfig::resolveResourceId))
                    .topLevelLinksResolver(getMultiDataItemTopLevelLinksResolver(resourceType, resourceConfig))
                    .topLevelMetaResolver(resourceConfig::resolveTopLevelMetaForMultiResourcesDoc)
                    .resourceLinksResolver(getResourceLinksResolver(resourceType, resourceConfig))
                    .resourceMetaResolver(resourceConfig::resolveResourceMeta)
                    .attributesResolver(resourceConfig::resolveAttributes)
                    .resourceTypeAndIdResolver(getResourceTypeAndIdResolver(resourceConfig))
                    .toMultipleResourcesDoc();
        }

        private <DATA_SOURCE_DTO> ResourceLinksResolver<JsonApiRequest, DATA_SOURCE_DTO> getResourceLinksResolver(
                ResourceType resourceType,
                Resource<DATA_SOURCE_DTO> resourceConfig
        ) {
            return (req, dto) -> {
                LinksObject linksObject = resourceConfig.resolveResourceLinks(req, dto);
                if (linksObject == Resource.NOT_IMPLEMENTED_LINKS_STUB) {
                    return ResourceLinksDefaultResolvers.defaultResourceLinksResolver(
                            resourceType,
                            resourceConfig::resolveResourceId
                    ).resolve(req, dto);
                }
                return linksObject;
            };
        }

        private <DATA_SOURCE_DTO> MultipleDataItemsDocLinksResolver<JsonApiRequest, DATA_SOURCE_DTO> getMultiDataItemTopLevelLinksResolver(
                ResourceType resourceType,
                Resource<DATA_SOURCE_DTO> resourceConfig
        ) {
            return (req, dtos, nextCursor) -> {
                LinksObject linksObject = resourceConfig.resolveTopLevelLinksForMultiResourcesDoc(req, dtos, nextCursor);
                if (linksObject == Resource.NOT_IMPLEMENTED_LINKS_STUB) {
                    return MultiResourcesDocLinksDefaultResolvers.<JsonApiRequest, DATA_SOURCE_DTO>defaultTopLevelLinksResolver(
                            resourceType
                    ).resolve(req, dtos, nextCursor);
                }
                return linksObject;
            };
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

        private <RESOURCE_DTO> Map<RelationshipName, ToManyRelationshipResolver<JsonApiRequest, RESOURCE_DTO>> getToManyRelationshipsResolvers(
                IdSupplier<RESOURCE_DTO> resourceIdSupplier
        ) {
            return domainRegistry.getToManyRelationships(resourceType)
                    .stream()
                    .filter(r -> !hasReadToManyRelationshipsBatchImplementation(r))
                    .collect(
                            toMap(
                                    RegisteredRelationship::getRelationshipName,
                                    registeredRelationship ->
                                            (req, dataSourceDto) ->
                                                    resolveToManyRelationships(
                                                            registeredRelationship.getRelationshipName(),
                                                            dataSourceDto,
                                                            resourceIdSupplier,
                                                            req
                                                    )
                            )
                    );
        }

        private <RESOURCE_DTO, RELATIONSHIP_DTO> ToManyRelationshipsDoc resolveToManyRelationships(
                RelationshipName relationshipName,
                RESOURCE_DTO dataSourceDto,
                IdSupplier<RESOURCE_DTO> resourceIdSupplier,
                JsonApiRequest parentRequest
        ) {
            RegisteredOperation<ReadToManyRelationshipOperation<?, ?>> registeredOperation
                    = operationsRegistry.getRegisteredReadToManyRelationshipOperation(resourceType, relationshipName, true);

            @SuppressWarnings("unchecked")
            ReadToManyRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO> executable
                    = (ReadToManyRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO>) registeredOperation.getOperation();

            JsonApiRequest relationshipRequest = getRelationshipRequestSupplier(
                    resourceIdSupplier,
                    registeredOperation.getOperationMeta(),
                    executable::validate
            ).create(parentRequest, dataSourceDto);

            MultipleDataItemsSupplier<JsonApiRequest, RELATIONSHIP_DTO> dataSupplier
                    = relRequest -> executable.readManyForResource(relRequest, dataSourceDto);

            List<PluginSettings> pluginSettings = getPluginSettings(
                registeredOperation,
                domainRegistry.getResource(resourceType),
                domainRegistry.getToManyRelationshipStrict(resourceType, relationshipName)
            );

            return resolveToManyRelationshipsDocCommon(
                    resourceType,
                    relationshipName,
                    relationshipRequest,
                    dataSupplier,
                    pluginSettings
            );
        }

        private <RESOURCE_DTO> Map<RelationshipName, BatchToManyRelationshipResolver<JsonApiRequest, RESOURCE_DTO>> getBatchToManyRelationshipResolvers(
                IdSupplier<RESOURCE_DTO> resourceIdSupplier
        ) {
            return domainRegistry.getToManyRelationships(resourceType)
                    .stream()
                    .filter(this::hasReadToManyRelationshipsBatchImplementation)
                    .collect(
                            toMap(
                                    RegisteredRelationship::getRelationshipName,
                                    registeredRelationship ->
                                            (req, dataSourceDtos) ->
                                                    resolveToManyRelationshipsInBatch(
                                                            registeredRelationship,
                                                            dataSourceDtos,
                                                            resourceIdSupplier,
                                                            req
                                                    )
                            )
                    );
        }

        private <RESOURCE_DTO, RELATIONSHIP_DTO> Map<RESOURCE_DTO, ToManyRelationshipsDoc> resolveToManyRelationshipsInBatch(
                RegisteredRelationship<ToManyRelationship<?>> registeredRelationship,
                List<RESOURCE_DTO> dataSourceDtos,
                IdSupplier<RESOURCE_DTO> resourceIdSupplier,
                JsonApiRequest parentRequest
        ) {

            RelationshipName relationshipName = registeredRelationship.getRelationshipName();

            @SuppressWarnings("unchecked")
            ToManyRelationship<RELATIONSHIP_DTO> toManyRelationshipCasted
                    = (ToManyRelationship<RELATIONSHIP_DTO>) registeredRelationship.getRelationship();

            RegisteredOperation<ReadToManyRelationshipOperation<?, ?>> registeredOperation
                    = operationsRegistry.getRegisteredReadToManyRelationshipOperation(resourceType, relationshipName, true);

            @SuppressWarnings("unchecked")
            BatchReadToManyRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO> executable
                    = (BatchReadToManyRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO>) registeredOperation.getOperation();

            RelationshipRequestSupplier<JsonApiRequest, RESOURCE_DTO> relationshipRequestSupplier = getRelationshipRequestSupplier(
                    resourceIdSupplier,
                    registeredOperation.getOperationMeta(),
                    executable::validate
            );

            ResourceTypeAndIdResolver<RELATIONSHIP_DTO> resourceIdentifierTypeAndIdResolver
                    = getResourceIdentifierTypeAndIdResolver(toManyRelationshipCasted);

            MultipleDataItemsDocLinksResolver<JsonApiRequest, RELATIONSHIP_DTO> topLevelLinksResolver
                    = getMultipleDataItemsTopLevelLinksResolver(toManyRelationshipCasted, resourceType, relationshipName);

            List<PluginSettings> pluginSettings = getPluginSettings(
                    registeredOperation,
                    domainRegistry.getResource(resourceType),
                    domainRegistry.getToManyRelationshipStrict(resourceType, relationshipName)
            );

            return new BatchToManyRelationshipsProcessor()
                    .plugins(pluginSettings)
                    .dataSupplier(executable::readBatches)
                    .resourceIdentifierMetaResolver(toManyRelationshipCasted::resolveResourceIdentifierMeta)
                    .topLevelLinksResolver(topLevelLinksResolver)
                    .topLevelMetaResolver(toManyRelationshipCasted::resolveRelationshipMeta)
                    .resourceIdentifierTypeAndIdResolver(resourceIdentifierTypeAndIdResolver)
                    .toManyRelationshipsDocBatch(parentRequest, dataSourceDtos, relationshipRequestSupplier);
        }

        private boolean hasReadToManyRelationshipsBatchImplementation(RegisteredRelationship<?> registeredRelationship) {
            RegisteredOperation<ReadToManyRelationshipOperation<?, ?>> operation = operationsRegistry.getRegisteredReadToManyRelationshipOperation(
                    resourceType,
                    registeredRelationship.getRelationshipName(),
                    false
            );
            return operation != null && operation.getOperation() instanceof BatchReadToManyRelationshipOperation;
        }

        private <RESOURCE_DTO> Map<RelationshipName, ToOneRelationshipResolver<JsonApiRequest, RESOURCE_DTO>> getToOneRelationshipResolvers(
                IdSupplier<RESOURCE_DTO> resourceIdSupplier
        ) {
            return domainRegistry.getToOneRelationships(resourceType)
                    .stream()
                    .filter(r -> !hasReadToOneRelationshipBatchImplementation(r))
                    .collect(
                            toMap(
                                    RegisteredRelationship::getRelationshipName,
                                    registeredRelationship ->
                                            (req, dataSourceDto) ->
                                                    resolveToOneRelationshipDoc(
                                                            registeredRelationship.getRelationshipName(),
                                                            dataSourceDto,
                                                            resourceIdSupplier,
                                                            req
                                                    )
                            )
                    );
        }

        private <RESOURCE_DTO, RELATIONSHIP_DTO> ToOneRelationshipDoc resolveToOneRelationshipDoc(
                RelationshipName relationshipName,
                RESOURCE_DTO dataSourceDto,
                IdSupplier<RESOURCE_DTO> resourceIdSupplier,
                JsonApiRequest parentRequest
        ) {
            RegisteredOperation<ReadToOneRelationshipOperation<?, ?>> registeredOperation
                    = operationsRegistry.getRegisteredReadToOneRelationshipOperation(resourceType, relationshipName, true);

            @SuppressWarnings("unchecked")
            ReadToOneRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO> executable
                    = (ReadToOneRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO>) registeredOperation.getOperation();

            JsonApiRequest relationshipRequest = getRelationshipRequestSupplier(
                    resourceIdSupplier,
                    registeredOperation.getOperationMeta(),
                    executable::validate
            ).create(parentRequest, dataSourceDto);

            SingleDataItemSupplier<JsonApiRequest, RELATIONSHIP_DTO> dataSupplier
                    = relRequest -> executable.readOneForResource(relRequest, dataSourceDto);

            List<PluginSettings> pluginSettings = getPluginSettings(
                    registeredOperation,
                    domainRegistry.getResource(resourceType),
                    domainRegistry.getToOneRelationshipStrict(resourceType, relationshipName)
            );

            return resolveToOneRelationshipDocCommon(
                    resourceType,
                    relationshipName,
                    relationshipRequest,
                    dataSupplier,
                    pluginSettings
            );
        }

        private <RESOURCE_DTO> Map<RelationshipName, BatchToOneRelationshipResolver<JsonApiRequest, RESOURCE_DTO>> getBatchToOneRelationshipResolvers(
                IdSupplier<RESOURCE_DTO> resourceIdSupplier
        ) {
            return domainRegistry.getToOneRelationships(resourceType)
                    .stream()
                    .filter(this::hasReadToOneRelationshipBatchImplementation)
                    .collect(
                            toMap(
                                    RegisteredRelationship::getRelationshipName,
                                    registeredRelationship ->
                                            (req, dataSourceDtos) ->
                                                    resolveToOneRelationshipInBatch(
                                                            registeredRelationship,
                                                            dataSourceDtos,
                                                            resourceIdSupplier,
                                                            req
                                                    )
                            )
                    );
        }

        private <RESOURCE_DTO, RELATIONSHIP_DTO> Map<RESOURCE_DTO, ToOneRelationshipDoc> resolveToOneRelationshipInBatch(
                RegisteredRelationship<ToOneRelationship<?>> registeredRelationship,
                List<RESOURCE_DTO> dataSourceDtos,
                IdSupplier<RESOURCE_DTO> resourceIdSupplier,
                JsonApiRequest request
        ) {

            RelationshipName relationshipName = registeredRelationship.getRelationshipName();

            @SuppressWarnings("unchecked")
            ToOneRelationship<RELATIONSHIP_DTO> toOneRelationshipCasted
                    = (ToOneRelationship<RELATIONSHIP_DTO>) registeredRelationship.getRelationship();

            RegisteredOperation<ReadToOneRelationshipOperation<?, ?>> registeredOperation
                    = operationsRegistry.getRegisteredReadToOneRelationshipOperation(resourceType, relationshipName, true);

            @SuppressWarnings("unchecked")
            BatchReadToOneRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO> executable
                    = (BatchReadToOneRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO>) registeredOperation.getOperation();

            List<PluginSettings> pluginSettings = getPluginSettings(
                registeredOperation,
                domainRegistry.getResource(resourceType),
                domainRegistry.getToOneRelationshipStrict(resourceType, relationshipName)
            );

            RelationshipRequestSupplier<JsonApiRequest, RESOURCE_DTO> relationshipRequestSupplier = getRelationshipRequestSupplier(
                    resourceIdSupplier,
                    registeredOperation.getOperationMeta(),
                    executable::validate
            );

            ResourceTypeAndIdResolver<RELATIONSHIP_DTO> resourceIdentifierTypeAndIdResolver
                    = getResourceIdentifierTypeAndIdResolver(toOneRelationshipCasted);

            SingleDataItemDocLinksResolver<JsonApiRequest, RELATIONSHIP_DTO> topLevelLinksResolver
                    = getSingleDataTopLevelLinksResolver(toOneRelationshipCasted, resourceType, relationshipName);

            return new BatchToOneRelationshipProcessor()
                    .plugins(pluginSettings)
                    .dataSupplier(executable::readBatches)
                    .resourceIdentifierMetaResolver(toOneRelationshipCasted::resolveResourceIdentifierMeta)
                    .topLevelLinksResolver(topLevelLinksResolver)
                    .topLevelMetaResolver(toOneRelationshipCasted::resolveRelationshipMeta)
                    .resourceIdentifierTypeAndIdResolver(resourceIdentifierTypeAndIdResolver)
                    .toOneRelationshipDocBatch(request, dataSourceDtos, relationshipRequestSupplier);
        }

        private boolean hasReadToOneRelationshipBatchImplementation(RegisteredRelationship<?> registeredRelationship) {
            RegisteredOperation<ReadToOneRelationshipOperation<?, ?>> registeredOperation = operationsRegistry.getRegisteredReadToOneRelationshipOperation(
                    resourceType,
                    registeredRelationship.getRelationshipName(),
                    false
            );
            return registeredOperation != null && registeredOperation.getOperation() instanceof BatchReadToOneRelationshipOperation;
        }

        private <RESOURCE_DTO> RelationshipRequestSupplier<JsonApiRequest, RESOURCE_DTO> getRelationshipRequestSupplier(
                IdSupplier<RESOURCE_DTO> resourceIdSupplier,
                RegisteredOperation.OperationMeta operationMeta,
                Consumer<JsonApiRequest> validator
        ) {
            return (originalRequest, dataSourceDto) -> {
                JsonApiRequest relationshipRequest = DefaultJsonApiRequest.composeRelationshipRequest(
                        resourceIdSupplier.getId(dataSourceDto),
                        operationMeta.getResourceType(),
                        operationMeta.getRelationshipName(),
                        operationMeta.getOperationType()
                );

                validator.accept(relationshipRequest);

                return relationshipRequest;
            };
        }

        private <RESOURCE_DTO> ResourceTypeAndIdResolver<RESOURCE_DTO> getResourceTypeAndIdResolver(
                Resource<RESOURCE_DTO> resourceConfig) {
            return dto -> new IdAndType(
                    resourceConfig.resolveResourceId(dto),
                    resourceType
            );
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

            List<PluginSettings> pluginSettings = getPluginSettings(
                    registeredOperation,
                    domainRegistry.getResource(resourceType),
                    domainRegistry.getToManyRelationshipStrict(resourceType, relationshipName)
            );

            return resolveToManyRelationshipsDocCommon(
                    resourceType,
                    relationshipName,
                    relationshipRequest,
                    (MultipleDataItemsSupplier<JsonApiRequest, RELATIONSHIP_DTO>) request -> {
                        try {
                            return executable.readMany(request);
                        } catch (OperationNotFoundException onfe) {
                            throw new OperationNotFoundException(
                                    registeredOperation.getOperationType(),
                                    registeredOperation.getResourceType(),
                                    registeredOperation.getRelationshipName(),
                                    onfe
                            );
                        }
                    },
                    pluginSettings
            );
        }

        public void updateToManyRelationship(JsonApiRequest relationshipRequest) {
            RegisteredOperation<UpdateToManyRelationshipOperation> registeredOperation
                    = operationsRegistry.getRegisteredUpdateToManyRelationshipOperation(resourceType, relationshipName, true);

            UpdateToManyRelationshipOperation executable
                    = registeredOperation.getOperation();

            executable.validate(relationshipRequest);

            List<PluginSettings> pluginSettings = getPluginSettings(
                    registeredOperation,
                    domainRegistry.getResource(resourceType),
                    domainRegistry.getToManyRelationshipStrict(resourceType, relationshipName)
            );

            executeCastedNoResponse(
                    relationshipRequest,
                    request -> {
                        try {
                            executable.update(request);
                        } catch (OperationNotFoundException onfe) {
                            throw new OperationNotFoundException(
                                    registeredOperation.getOperationType(),
                                    registeredOperation.getResourceType(),
                                    registeredOperation.getRelationshipName(),
                                    onfe
                            );
                        }
                    },
                    pluginSettings
            );
        }

        private void executeCastedNoResponse(
                JsonApiRequest relationshipRequest,
                Consumer<JsonApiRequest> executable,
                List<PluginSettings> pluginSettings
        ) {
            resolveToManyRelationshipsDocCommon(
                    resourceType,
                    relationshipName,
                    relationshipRequest,
                    req -> {
                        executable.accept(req);
                        return null;
                    },
                    pluginSettings
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

            List<PluginSettings> pluginSettings = getPluginSettings(
                    registeredOperation,
                    domainRegistry.getResource(resourceType),
                    domainRegistry.getToOneRelationshipStrict(resourceType, relationshipName)
            );

            return resolveToOneRelationshipDocCommon(
                    resourceType,
                    relationshipName,
                    relationshipRequest,
                    (SingleDataItemSupplier<JsonApiRequest, RELATIONSHIP_DTO>) request -> {
                        try {
                            return executable.readOne(request);
                        } catch (OperationNotFoundException onfe) {
                            throw new OperationNotFoundException(
                                    registeredOperation.getOperationType(),
                                    registeredOperation.getResourceType(),
                                    registeredOperation.getRelationshipName(),
                                    onfe
                            );
                        }
                    },
                    pluginSettings
            );
        }

        public void updateToOneRelationship(JsonApiRequest relationshipRequest) {
            RegisteredOperation<UpdateToOneRelationshipOperation> registeredOperation
                    = operationsRegistry.getRegisteredUpdateToOneRelationshipOperation(resourceType, relationshipName, true);

            UpdateToOneRelationshipOperation executable
                    = registeredOperation.getOperation();

            executable.validate(relationshipRequest);

            List<PluginSettings> pluginSettings = getPluginSettings(
                    registeredOperation,
                    domainRegistry.getResource(resourceType),
                    domainRegistry.getToOneRelationshipStrict(resourceType, relationshipName)
            );

            executeCastedNoResponse(
                    relationshipRequest,
                    request -> {
                        try {
                            executable.update(request);
                        } catch (OperationNotFoundException onfe) {
                            throw new OperationNotFoundException(
                                    registeredOperation.getOperationType(),
                                    registeredOperation.getResourceType(),
                                    registeredOperation.getRelationshipName(),
                                    onfe
                            );
                        }
                    },
                    pluginSettings
            );
        }

        private void executeCastedNoResponse(
                JsonApiRequest relationshipRequest,
                Consumer<JsonApiRequest> executable,
                List<PluginSettings> pluginSettings
        ) {
            resolveToOneRelationshipDocCommon(
                    resourceType,
                    relationshipName,
                    relationshipRequest,
                    req -> {
                        executable.accept(req);
                        return null;
                    },
                    pluginSettings
            );
        }
    }
}
