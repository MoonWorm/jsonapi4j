package pro.api4.jsonapi4j.processor.single.resource;

import pro.api4.jsonapi4j.processor.RelationshipsSupplier;
import pro.api4.jsonapi4j.processor.ResourceJsonApiMembersResolver;
import pro.api4.jsonapi4j.processor.exception.ResourceNotFoundException;
import pro.api4.jsonapi4j.processor.util.CustomCollectors;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import pro.api4.jsonapi4j.plugin.ac.principal.AuthenticatedPrincipalContextHolder;
import pro.api4.jsonapi4j.plugin.ac.principal.Principal;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class SingleResourceJsonApiMembersResolver<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES>
        extends ResourceJsonApiMembersResolver<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> {

    private static final Logger log = LoggerFactory.getLogger(SingleResourceJsonApiMembersResolver.class);

    private final SingleResourceJsonApiContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> jsonApiContext;
    private final Executor executor;

    public SingleResourceJsonApiMembersResolver(
            SingleResourceJsonApiContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> jsonApiContext,
            Executor executor
    ) {
        super(jsonApiContext);
        this.jsonApiContext = jsonApiContext;
        this.executor = executor;
    }

    public LinksObject resolveDocLinks(REQUEST request, DATA_SOURCE_DTO dataSourceDto) {
        return jsonApiContext.getTopLevelLinksResolver() != null
                ? jsonApiContext.getTopLevelLinksResolver().resolve(request, dataSourceDto)
                : null;
    }

    public Object resolveDocMeta(REQUEST request, DATA_SOURCE_DTO dataSourceDto) {
        return jsonApiContext.getTopLevelMetaResolver() != null
                ? jsonApiContext.getTopLevelMetaResolver().resolve(request, dataSourceDto)
                : null;
    }

    public <RELATIONSHIPS> RELATIONSHIPS resolveResourceRelationshipsInParallel(
            REQUEST request,
            DATA_SOURCE_DTO dataSourceDto,
            RelationshipsSupplier<RELATIONSHIPS> relationshipsSupplier
    ) {
        if (relationshipsSupplier == null) {
            return null;
        }

        // supply async futures and execute everything in parallel
        SingleResourceRelationshipDocFutures futures = supplyAsyncRelationshipDataFutures(request, dataSourceDto);

        // wait for toManyRelationshipsDocs map
        Map<RelationshipName, ToManyRelationshipsDoc> toManyRelationshipsDocs = unwrapCompletionException(
                () -> resolveToManyRelationshipsDocs(
                        futures.toManyRelationshipsDocBasicFutures(),
                        request,
                        dataSourceDto
                )
        );

        // wait for toOneRelationshipDocs map
        Map<RelationshipName, ToOneRelationshipDoc> toOneRelationshipDocs = unwrapCompletionException(
                () -> resolveToOneRelationshipsDocs(
                        futures.toOneRelationshipDocBasicFutures(),
                        request,
                        dataSourceDto
                )
        );

        // instantiate RELATIONSHIPS object
        return relationshipsSupplier.get(toManyRelationshipsDocs, toOneRelationshipDocs);
    }

    private SingleResourceRelationshipDocFutures supplyAsyncRelationshipDataFutures(
            REQUEST request,
            DATA_SOURCE_DTO dto
    ) {
        // to many relationships - basic resolvers
        Map<RelationshipName, CompletableFuture<ToManyRelationshipsDoc>> toManyRelationshipsDocBasicFutures =
                getDefaultRelationshipResolvers().keySet().stream()
                        .filter(this::isToManyRelationship)
                        .filter(rel -> getToManyRelationshipResolvers().containsKey(rel) || getBatchToManyRelationshipResolvers().containsKey(rel))
                        .collect(Collectors.toMap(
                                rel -> rel,
                                rel -> {
                                    Principal principalCopy = AuthenticatedPrincipalContextHolder.copy();
                                    return CompletableFuture.supplyAsync(
                                            () -> {
                                                AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(principalCopy);
                                                try {
                                                    if (getToManyRelationshipResolvers().containsKey(rel)) {
                                                        return getToManyRelationshipResolvers()
                                                                .get(rel)
                                                                .resolveRequestedData(request, dto);
                                                    } else {
                                                        Map<DATA_SOURCE_DTO, ToManyRelationshipsDoc> batchResult = getBatchToManyRelationshipResolvers()
                                                                .get(rel)
                                                                .resolveRequestedData(request, Collections.singletonList(dto));
                                                        return MapUtils.emptyIfNull(batchResult).values().stream().findFirst().orElse(null);
                                                    }
                                                } catch (ResourceNotFoundException nfe) {
                                                    return createToManyRelationshipsDocWithEmptyData(rel, request, dto);
                                                }
                                            },
                                            executor
                                    );
                                }));

        // to one relationship - basic resolvers
        Map<RelationshipName, CompletableFuture<ToOneRelationshipDoc>> toOneRelationshipDocBasicFutures =
                getDefaultRelationshipResolvers().keySet().stream()
                        .filter(this::isToOneRelationship)
                        .filter(rel -> getToOneRelationshipResolvers().containsKey(rel) || getBatchToOneRelationshipResolvers().containsKey(rel))
                        .collect(Collectors.toMap(
                                rel -> rel,
                                rel -> {
                                    Principal principalCopy = AuthenticatedPrincipalContextHolder.copy();
                                    return CompletableFuture.supplyAsync(
                                            () -> {
                                                AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(principalCopy);
                                                try {
                                                    if (getToOneRelationshipResolvers().containsKey(rel)) {
                                                        return getToOneRelationshipResolvers()
                                                                .get(rel)
                                                                .resolveRequestedData(request, dto);
                                                    } else {
                                                        Map<DATA_SOURCE_DTO, ToOneRelationshipDoc> batchResult = getBatchToOneRelationshipResolvers()
                                                                .get(rel)
                                                                .resolveRequestedData(request, Collections.singletonList(dto));
                                                        return MapUtils.emptyIfNull(batchResult).values().stream().findFirst().orElse(null);
                                                    }
                                                } catch (ResourceNotFoundException nfe) {
                                                    return createToOneRelationshipDocWithNullData(rel, request, dto);
                                                }
                                            },
                                            executor
                                    );
                                }
                        ));

        return new SingleResourceRelationshipDocFutures(
                toManyRelationshipsDocBasicFutures,
                toOneRelationshipDocBasicFutures
        );
    }

    private Map<RelationshipName, ToOneRelationshipDoc> resolveToOneRelationshipsDocs(Map<RelationshipName, CompletableFuture<ToOneRelationshipDoc>> futures,
                                                                                      REQUEST request,
                                                                                      DATA_SOURCE_DTO dto) {
        return getDefaultRelationshipResolvers().keySet()
                .stream()
                .filter(this::isToOneRelationship)
                .collect(CustomCollectors.toMapThatSupportsNullValues(
                        relName -> relName,
                        relName -> {
                            if (futures.containsKey(relName)) {
                                log.info("Processing '{}' relationship. Relationship was requested in 'include'. Simple To-one-relationship resolver is found. Executing.", relName);
                                return futures.get(relName).join();
                            } else {
                                log.info("Processing '{}' relationship. Relationship wasn't requested in 'include'. To-one-relationship resolvers is not invoking. Relying on the default relationship resolver.", relName);
                                return createToOneRelationshipDocWithNullData(relName, request, dto);
                            }
                        }
                ));
    }

    private Map<RelationshipName, ToManyRelationshipsDoc> resolveToManyRelationshipsDocs(
            Map<RelationshipName, CompletableFuture<ToManyRelationshipsDoc>> futures,
            REQUEST request,
            DATA_SOURCE_DTO dto
    ) {
        return getDefaultRelationshipResolvers().keySet()
                .stream()
                .filter(this::isToManyRelationship)
                .collect(CustomCollectors.toMapThatSupportsNullValues(
                        relName -> relName,
                        relName -> {
                            if (futures.containsKey(relName)) {
                                log.info("Processing '{}' relationship. Relationship was requested in 'include'. Simple To-many-relationship resolver is found. Executing.", relName);
                                return futures.get(relName).join();
                            } else {
                                log.info("Processing '{}' relationship. Relationship wasn't requested in 'include'. To-many-relationship resolvers is not invoking. Relying on the default relationship resolver.", relName);
                                return createToManyRelationshipsDocWithNullData(relName, request, dto);
                            }
                        }
                ));
    }

    record SingleResourceRelationshipDocFutures(
            Map<RelationshipName, CompletableFuture<ToManyRelationshipsDoc>> toManyRelationshipsDocBasicFutures,
            Map<RelationshipName, CompletableFuture<ToOneRelationshipDoc>> toOneRelationshipDocBasicFutures) {
    }

}
