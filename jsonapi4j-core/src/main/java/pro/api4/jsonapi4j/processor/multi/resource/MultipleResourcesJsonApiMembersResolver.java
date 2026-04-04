package pro.api4.jsonapi4j.processor.multi.resource;

import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipObject;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipObject;
import pro.api4.jsonapi4j.processor.IdAndType;
import pro.api4.jsonapi4j.processor.RelationshipsSupplier;
import pro.api4.jsonapi4j.processor.ResourceJsonApiMembersResolver;
import pro.api4.jsonapi4j.processor.exception.ResourceNotFoundException;
import pro.api4.jsonapi4j.processor.resolvers.DefaultRelationshipResolver;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.principal.AuthenticatedPrincipalContextHolder;
import pro.api4.jsonapi4j.principal.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.api4.jsonapi4j.util.CustomCollectors;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static pro.api4.jsonapi4j.util.CustomCollectors.toOrderedMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toMap;

public class MultipleResourcesJsonApiMembersResolver<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES>
        extends ResourceJsonApiMembersResolver<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> {

    private static final Logger log = LoggerFactory.getLogger(MultipleResourcesJsonApiMembersResolver.class);

    private final MultipleResourcesJsonApiContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> jsonApiContext;
    private final Executor executor;

    public MultipleResourcesJsonApiMembersResolver(
            MultipleResourcesJsonApiContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> jsonApiContext,
            Executor executor
    ) {
        super(jsonApiContext);
        this.jsonApiContext = jsonApiContext;
        this.executor = executor;
    }

    public LinksObject resolveDocLinks(REQUEST request,
                                       List<DATA_SOURCE_DTO> dtos,
                                       String nextCursor) {
        return jsonApiContext.getTopLevelLinksResolver() != null
                ? jsonApiContext.getTopLevelLinksResolver().resolve(request, dtos, nextCursor)
                : null;
    }

    public Object resolveDocMeta(REQUEST request, List<DATA_SOURCE_DTO> dtos) {
        return jsonApiContext.getTopLevelMetaResolver() != null
                ? jsonApiContext.getTopLevelMetaResolver().resolve(request, dtos)
                : null;
    }

    public <RELATIONSHIPS> Map<IdAndType, RELATIONSHIPS> resolveResourceRelationshipsInParallel(
            REQUEST request,
            List<DATA_SOURCE_DTO> dtos,
            RelationshipsSupplier<RELATIONSHIPS> relationshipsSupplier
    ) {
        if (relationshipsSupplier == null) {
            return null;
        }

        MultipleResourcesRelationshipFutures<DATA_SOURCE_DTO> futures
                = supplyAsyncRelationshipFutures(request, dtos);

        Map<DATA_SOURCE_DTO, Map<RelationshipName, ToOneRelationshipObject>> toOneRelationshipByDto = unwrapCompletionException(
                () -> resolveToOneRelationships(
                        request,
                        dtos,
                        futures.toOneRelationshipBatchFutures(),
                        futures.toOneRelationshipBasicFutures()
                )
        );

        Map<DATA_SOURCE_DTO, Map<RelationshipName, ToManyRelationshipObject>> toManyRelationshipsByDto = unwrapCompletionException(
                () -> resolveToManyRelationships(
                        request,
                        dtos,
                        futures.toManyRelationshipBatchFutures(),
                        futures.toManyRelationshipBasicFutures()
                )
        );

        return dtos.stream()
                .collect(
                        CustomCollectors.toMapThatSupportsNullValues(
                                this::resolveResourceIdAndType,
                                dto -> relationshipsSupplier.get(
                                        toManyRelationshipsByDto.get(dto),
                                        toOneRelationshipByDto.get(dto)
                                )
                        )
                );
    }

    private MultipleResourcesRelationshipFutures<DATA_SOURCE_DTO> supplyAsyncRelationshipFutures(
            REQUEST request,
            List<DATA_SOURCE_DTO> dtos
    ) {
        Map<DATA_SOURCE_DTO, Map<RelationshipName, CompletableFuture<ToManyRelationshipObject>>> toManyRelationshipsBasicFutures
                = new HashMap<>();
        Map<DATA_SOURCE_DTO, Map<RelationshipName, CompletableFuture<ToOneRelationshipObject>>> toOneRelationshipBasicFutures
                = new HashMap<>();

        for (DATA_SOURCE_DTO dto : dtos) {
            // to many relationships - basic resolvers
            toManyRelationshipsBasicFutures.put(
                    dto,
                    getToManyRelationshipResolvers().entrySet()
                            .stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> {
                                        Principal principalCopy = AuthenticatedPrincipalContextHolder.copy();
                                        return CompletableFuture.supplyAsync(() -> {
                                            AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(principalCopy);
                                            try {
                                                return e.getValue().resolveRequestedData(request, dto);
                                            } catch (ResourceNotFoundException nfe) {
                                                return createToManyRelationshipsWithEmptyData(e.getKey(), request, dto);
                                            }
                                        }, executor);
                                    })
                            )
            );

            // to one relationship - basic resolvers
            toOneRelationshipBasicFutures.put(
                    dto,
                    getToOneRelationshipResolvers().entrySet()
                            .stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> {
                                        Principal principalCopy = AuthenticatedPrincipalContextHolder.copy();
                                        return CompletableFuture.supplyAsync(() -> {
                                            AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(principalCopy);
                                            try {
                                                return e.getValue().resolveRequestedData(request, dto);
                                            } catch (ResourceNotFoundException nfe) {
                                                return createToOneRelationshipWithNullData(e.getKey(), request, dto);
                                            }
                                        }, executor);
                                    })
                            )
            );
        }

        // to many relationships - batch resolvers
        Map<RelationshipName, CompletableFuture<Map<DATA_SOURCE_DTO, ToManyRelationshipObject>>> toManyRelationshipsBatchFutures =
                getBatchToManyRelationshipResolvers().entrySet()
                        .stream()
                        .collect(toMap(
                                Map.Entry::getKey,
                                e -> {
                                    Principal principalCopy = AuthenticatedPrincipalContextHolder.copy();
                                    return CompletableFuture.supplyAsync(
                                            () -> {
                                                AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(principalCopy);
                                                log.info("Batch processing of '{}' To-Many relationship", e.getKey());
                                                Map<DATA_SOURCE_DTO, ToManyRelationshipObject> result
                                                        = new HashMap<>(e.getValue().resolveRequestedData(request, dtos));
                                                return unmodifiableMap(result);
                                            },
                                            executor
                                    );
                                }));

        // to one relationship - batch resolvers
        Map<RelationshipName, CompletableFuture<Map<DATA_SOURCE_DTO, ToOneRelationshipObject>>> toOneRelationshipBatchFutures =
                getBatchToOneRelationshipResolvers().entrySet()
                        .stream()
                        .collect(toMap(
                                Map.Entry::getKey,
                                e -> {
                                    Principal principalCopy = AuthenticatedPrincipalContextHolder.copy();
                                    return CompletableFuture.supplyAsync(
                                            () -> {
                                                AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(principalCopy);
                                                log.info("Batch processing of '{}' To-One relationship", e.getKey());
                                                Map<DATA_SOURCE_DTO, ToOneRelationshipObject> resolvedResult
                                                        = e.getValue().resolveRequestedData(request, dtos);
                                                Map<DATA_SOURCE_DTO, ToOneRelationshipObject> result
                                                        = new HashMap<>(resolvedResult);
                                                return unmodifiableMap(result);
                                            },
                                            executor
                                    );
                                })
                        );

        return new MultipleResourcesRelationshipFutures<>(
                toManyRelationshipsBatchFutures,
                toManyRelationshipsBasicFutures,
                toOneRelationshipBatchFutures,
                toOneRelationshipBasicFutures
        );
    }

    private Map<DATA_SOURCE_DTO, Map<RelationshipName, ToManyRelationshipObject>> resolveToManyRelationships(
            REQUEST request,
            List<DATA_SOURCE_DTO> dtos,
            Map<RelationshipName, CompletableFuture<Map<DATA_SOURCE_DTO, ToManyRelationshipObject>>> batchFutures,
            Map<DATA_SOURCE_DTO, Map<RelationshipName, CompletableFuture<ToManyRelationshipObject>>> basicFutures) {

        Map<RelationshipName, DefaultRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> defaultRelationshipResolvers
                = getDefaultRelationshipResolvers();

        // initialize result map
        Map<DATA_SOURCE_DTO, Map<RelationshipName, ToManyRelationshipObject>> toManyRelationshipObjectsMap = dtos
                .stream()
                .collect(
                        toOrderedMap(
                                dto -> dto,
                                dto -> new LinkedHashMap<>()
                        )
                );

        for (Map.Entry<RelationshipName, DefaultRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> e
                : defaultRelationshipResolvers.entrySet()) {
            RelationshipName relName = e.getKey();
            if (isToManyRelationship(relName)) {
                if (getBatchToManyRelationshipResolvers().containsKey(relName)) {
                    // relationship was requested and Batch To Many Relationship Resolver is configured
                    log.info("Processing '{}' relationship. Relationship was requested in 'include'. Batch To-many-relationship resolver is found. Executing.", relName);
                    Map<DATA_SOURCE_DTO, ToManyRelationshipObject> docsMap = batchFutures.get(relName).join();
                    for (DATA_SOURCE_DTO dto : dtos) {
                        ToManyRelationshipObject toManyRelationshipObject = docsMap.get(dto);
                        toManyRelationshipObjectsMap.get(dto).put(relName, toManyRelationshipObject);
                    }
                } else {
                    for (DATA_SOURCE_DTO dto : dtos) {
                        if (getToManyRelationshipResolvers().containsKey(relName)) {
                            log.info("Processing '{}' relationship. Relationship was requested in 'include'. Simple To-many-relationship resolver is found. Executing.", relName);
                            ToManyRelationshipObject toManyRelationshipObject = basicFutures.get(dto).get(relName).join();
                            toManyRelationshipObjectsMap.get(dto).put(relName, toManyRelationshipObject);
                        } else {
                            log.info("Processing '{}' relationship. Relationship wasn't requested in 'include'. To-many-relationship resolvers is not invoking. Relying on the default relationship resolver.", relName);
                            toManyRelationshipObjectsMap.get(dto).put(relName, createToManyRelationshipsWithNullData(relName, request, dto));
                        }
                    }
                }
            }
        }
        return toManyRelationshipObjectsMap;
    }

    private Map<DATA_SOURCE_DTO, Map<RelationshipName, ToOneRelationshipObject>> resolveToOneRelationships(
            REQUEST request,
            List<DATA_SOURCE_DTO> dtos,
            Map<RelationshipName, CompletableFuture<Map<DATA_SOURCE_DTO, ToOneRelationshipObject>>> batchFutures,
            Map<DATA_SOURCE_DTO, Map<RelationshipName, CompletableFuture<ToOneRelationshipObject>>> basicFutures) {

        Map<RelationshipName, DefaultRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> defaultRelationshipResolvers = getDefaultRelationshipResolvers();

        //initialize result map
        Map<DATA_SOURCE_DTO, Map<RelationshipName, ToOneRelationshipObject>> toOneRelationshipObjectsMap = dtos
                .stream()
                .collect(
                        toOrderedMap(
                                dto -> dto,
                                dataSourceDto -> new LinkedHashMap<>()
                        )
                );

        for (Map.Entry<RelationshipName, DefaultRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> e
                : defaultRelationshipResolvers.entrySet()) {
            RelationshipName relName = e.getKey();
            if (isToOneRelationship(relName)) {
                if (getBatchToOneRelationshipResolvers().containsKey(relName)) {
                    log.info("Processing '{}' relationship. Relationship was requested in 'include'. Batch To-one-relationship resolver is found. Executing.", relName);
                    Map<DATA_SOURCE_DTO, ToOneRelationshipObject> docsMap = batchFutures.get(relName).join();
                    for (DATA_SOURCE_DTO dto : dtos) {
                        ToOneRelationshipObject toOneRelationshipObject = docsMap.get(dto);
                        toOneRelationshipObjectsMap.get(dto).put(relName, toOneRelationshipObject);
                    }
                } else {
                    // relationship wasn't requested
                    for (DATA_SOURCE_DTO dto : dtos) {
                        if (getToOneRelationshipResolvers().containsKey(relName)) {
                            log.info("Processing '{}' relationship. Relationship was requested in 'include'. Simple To-one-relationship resolver is found. Executing.", relName);
                            ToOneRelationshipObject toOneRelationshipObject = basicFutures.get(dto).get(relName).join();
                            toOneRelationshipObjectsMap.get(dto).put(relName, toOneRelationshipObject);
                        } else {
                            log.info("Processing '{}' relationship. Relationship wasn't requested in 'include'. To-one-relationship resolvers is not invoking. Relying on the default relationship resolver.", relName);
                            toOneRelationshipObjectsMap.get(dto).put(relName, createToOneRelationshipWithNullData(relName, request, dto));
                        }
                    }
                }
            }
        }
        return toOneRelationshipObjectsMap;
    }

    record MultipleResourcesRelationshipFutures<DATA_SOURCE_DTO>(
            Map<RelationshipName, CompletableFuture<Map<DATA_SOURCE_DTO, ToManyRelationshipObject>>> toManyRelationshipBatchFutures,
            Map<DATA_SOURCE_DTO, Map<RelationshipName, CompletableFuture<ToManyRelationshipObject>>> toManyRelationshipBasicFutures,
            Map<RelationshipName, CompletableFuture<Map<DATA_SOURCE_DTO, ToOneRelationshipObject>>> toOneRelationshipBatchFutures,
            Map<DATA_SOURCE_DTO, Map<RelationshipName, CompletableFuture<ToOneRelationshipObject>>> toOneRelationshipBasicFutures) {
    }

}
