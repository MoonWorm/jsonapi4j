package pro.api4.jsonapi4j.processor.multi.resource;

import pro.api4.jsonapi4j.processor.RelationshipsSupplier;
import pro.api4.jsonapi4j.processor.ResourceJsonApiMembersResolver;
import pro.api4.jsonapi4j.processor.exception.ResourceNotFoundException;
import pro.api4.jsonapi4j.processor.resolvers.DefaultRelationshipResolver;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import pro.api4.jsonapi4j.plugin.ac.AuthenticatedPrincipalContextHolder;
import pro.api4.jsonapi4j.plugin.ac.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static pro.api4.jsonapi4j.processor.util.CustomCollectors.toOrderedMap;
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

    public <RELATIONSHIPS> Map<DATA_SOURCE_DTO, RELATIONSHIPS> resolveResourceRelationshipsInParallel(
            REQUEST request,
            List<DATA_SOURCE_DTO> dtos,
            RelationshipsSupplier<RELATIONSHIPS> relationshipsSupplier
    ) {
        if (relationshipsSupplier == null) {
            return null;
        }

        MultipleResourcesRelationshipDocFutures<DATA_SOURCE_DTO> futures
                = supplyAsyncRelationshipDocFutures(request, dtos);

        Map<DATA_SOURCE_DTO, Map<RelationshipName, ToOneRelationshipDoc>> toOneRelationshipByDto = unwrapCompletionException(
                () -> resolveToOneRelationshipDocs(
                        request,
                        dtos,
                        futures.toOneRelationshipDocBatchFutures(),
                        futures.toOneRelationshipDocBasicFutures()
                )
        );

        Map<DATA_SOURCE_DTO, Map<RelationshipName, ToManyRelationshipsDoc>> toManyRelationshipsByDto = unwrapCompletionException(
                () -> resolveToManyRelationshipDocs(
                        request,
                        dtos,
                        futures.toManyRelationshipsDocBatchFutures(),
                        futures.toManyRelationshipsDocBasicFutures()
                )
        );

        return dtos.stream()
                .collect(
                        Collectors.toMap(
                                dto -> dto,
                                dto -> relationshipsSupplier.get(
                                        toManyRelationshipsByDto.get(dto),
                                        toOneRelationshipByDto.get(dto)
                                )
                        )
                );
    }

    private MultipleResourcesRelationshipDocFutures<DATA_SOURCE_DTO> supplyAsyncRelationshipDocFutures(
            REQUEST request,
            List<DATA_SOURCE_DTO> dtos
    ) {
        Map<DATA_SOURCE_DTO, Map<RelationshipName, CompletableFuture<ToManyRelationshipsDoc>>> toManyRelationshipsBasicFutures
                = new HashMap<>();
        Map<DATA_SOURCE_DTO, Map<RelationshipName, CompletableFuture<ToOneRelationshipDoc>>> toOneRelationshipBasicFutures
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
                                                return createToManyRelationshipsDocWithEmptyData(e.getKey(), request, dto);
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
                                                return createToOneRelationshipDocWithNullData(e.getKey(), request, dto);
                                            }
                                        }, executor);
                                    })
                            )
            );
        }

        // to many relationships - batch resolvers
        Map<RelationshipName, CompletableFuture<Map<DATA_SOURCE_DTO, ToManyRelationshipsDoc>>> toManyRelationshipsBatchFutures =
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
                                                Map<DATA_SOURCE_DTO, ToManyRelationshipsDoc> result
                                                        = new HashMap<>(e.getValue().resolveRequestedData(request, dtos));
                                                return unmodifiableMap(result);
                                            },
                                            executor
                                    );
                                }));

        // to one relationship - batch resolvers
        Map<RelationshipName, CompletableFuture<Map<DATA_SOURCE_DTO, ToOneRelationshipDoc>>> toOneRelationshipBatchFutures =
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
                                                Map<DATA_SOURCE_DTO, ToOneRelationshipDoc> resolvedResult
                                                        = e.getValue().resolveRequestedData(request, dtos);
                                                Map<DATA_SOURCE_DTO, ToOneRelationshipDoc> result
                                                        = new HashMap<>(resolvedResult);
                                                return unmodifiableMap(result);
                                            },
                                            executor
                                    );
                                })
                        );

        return new MultipleResourcesRelationshipDocFutures<>(
                toManyRelationshipsBatchFutures,
                toManyRelationshipsBasicFutures,
                toOneRelationshipBatchFutures,
                toOneRelationshipBasicFutures
        );
    }

    private Map<DATA_SOURCE_DTO, Map<RelationshipName, ToManyRelationshipsDoc>> resolveToManyRelationshipDocs(
            REQUEST request,
            List<DATA_SOURCE_DTO> dtos,
            Map<RelationshipName, CompletableFuture<Map<DATA_SOURCE_DTO, ToManyRelationshipsDoc>>> batchFutures,
            Map<DATA_SOURCE_DTO, Map<RelationshipName, CompletableFuture<ToManyRelationshipsDoc>>> basicFutures) {

        Map<RelationshipName, DefaultRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> defaultRelationshipResolvers
                = getDefaultRelationshipResolvers();

        // initialize result map
        Map<DATA_SOURCE_DTO, Map<RelationshipName, ToManyRelationshipsDoc>> toManyRelationshipDocMap = dtos
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
                    Map<DATA_SOURCE_DTO, ToManyRelationshipsDoc> docsMap = batchFutures.get(relName).join();
                    for (DATA_SOURCE_DTO dto : dtos) {
                        ToManyRelationshipsDoc multiRelationshipsDoc = docsMap.get(dto);
                        toManyRelationshipDocMap.get(dto).put(relName, multiRelationshipsDoc);
                    }
                } else {
                    for (DATA_SOURCE_DTO dto : dtos) {
                        if (getToManyRelationshipResolvers().containsKey(relName)) {
                            log.info("Processing '{}' relationship. Relationship was requested in 'include'. Simple To-many-relationship resolver is found. Executing.", relName);
                            ToManyRelationshipsDoc toManyRelationshipsDoc = basicFutures.get(dto).get(relName).join();
                            toManyRelationshipDocMap.get(dto).put(relName, toManyRelationshipsDoc);
                        } else {
                            log.info("Processing '{}' relationship. Relationship wasn't requested in 'include'. To-many-relationship resolvers is not invoking. Relying on the default relationship resolver.", relName);
                            toManyRelationshipDocMap.get(dto).put(relName, createToManyRelationshipsDocWithNullData(relName, request, dto));
                        }
                    }
                }
            }
        }
        return toManyRelationshipDocMap;
    }

    private Map<DATA_SOURCE_DTO, Map<RelationshipName, ToOneRelationshipDoc>> resolveToOneRelationshipDocs(
            REQUEST request,
            List<DATA_SOURCE_DTO> dtos,
            Map<RelationshipName, CompletableFuture<Map<DATA_SOURCE_DTO, ToOneRelationshipDoc>>> batchFutures,
            Map<DATA_SOURCE_DTO, Map<RelationshipName, CompletableFuture<ToOneRelationshipDoc>>> basicFutures) {

        Map<RelationshipName, DefaultRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> defaultRelationshipResolvers = getDefaultRelationshipResolvers();

        //initialize result map
        Map<DATA_SOURCE_DTO, Map<RelationshipName, ToOneRelationshipDoc>> toOneRelationshipsDocMap = dtos
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
                    Map<DATA_SOURCE_DTO, ToOneRelationshipDoc> docsMap = batchFutures.get(relName).join();
                    for (DATA_SOURCE_DTO dto : dtos) {
                        ToOneRelationshipDoc toOneRelationshipDoc = docsMap.get(dto);
                        toOneRelationshipsDocMap.get(dto).put(relName, toOneRelationshipDoc);
                    }
                } else {
                    // relationship wasn't requested
                    for (DATA_SOURCE_DTO dto : dtos) {
                        if (getToOneRelationshipResolvers().containsKey(relName)) {
                            log.info("Processing '{}' relationship. Relationship was requested in 'include'. Simple To-one-relationship resolver is found. Executing.", relName);
                            ToOneRelationshipDoc toOneRelationshipDoc = basicFutures.get(dto).get(relName).join();
                            toOneRelationshipsDocMap.get(dto).put(relName, toOneRelationshipDoc);
                        } else {
                            log.info("Processing '{}' relationship. Relationship wasn't requested in 'include'. To-one-relationship resolvers is not invoking. Relying on the default relationship resolver.", relName);
                            toOneRelationshipsDocMap.get(dto).put(relName, createToOneRelationshipDocWithNullData(relName, request, dto));
                        }
                    }
                }
            }
        }
        return toOneRelationshipsDocMap;
    }

    record MultipleResourcesRelationshipDocFutures<DATA_SOURCE_DTO>(
            Map<RelationshipName, CompletableFuture<Map<DATA_SOURCE_DTO, ToManyRelationshipsDoc>>> toManyRelationshipsDocBatchFutures,
            Map<DATA_SOURCE_DTO, Map<RelationshipName, CompletableFuture<ToManyRelationshipsDoc>>> toManyRelationshipsDocBasicFutures,
            Map<RelationshipName, CompletableFuture<Map<DATA_SOURCE_DTO, ToOneRelationshipDoc>>> toOneRelationshipDocBatchFutures,
            Map<DATA_SOURCE_DTO, Map<RelationshipName, CompletableFuture<ToOneRelationshipDoc>>> toOneRelationshipDocBasicFutures) {
    }

}
