package pro.api4.jsonapi4j.processor.single.resource;

import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipObject;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipObject;
import pro.api4.jsonapi4j.processor.RelationshipsSupplier;
import pro.api4.jsonapi4j.processor.ResourceJsonApiMembersResolver;
import pro.api4.jsonapi4j.processor.exception.ResourceNotFoundException;
import pro.api4.jsonapi4j.util.CustomCollectors;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.principal.AuthenticatedPrincipalContextHolder;
import pro.api4.jsonapi4j.principal.Principal;
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
        SingleResourceRelationshipFutures futures = supplyAsyncRelationshipDataFutures(request, dataSourceDto);

        // wait for toManyRelationships map
        Map<RelationshipName, ToManyRelationshipObject> toManyRelationships = unwrapCompletionException(
                () -> resolveToManyRelationships(
                        futures.toManyRelationshipBasicFutures(),
                        request,
                        dataSourceDto
                )
        );

        // wait for toOneRelationship map
        Map<RelationshipName, ToOneRelationshipObject> toOneRelationships = unwrapCompletionException(
                () -> resolveToOneRelationships(
                        futures.toOneRelationshipBasicFutures(),
                        request,
                        dataSourceDto
                )
        );

        // instantiate RELATIONSHIPS object
        return relationshipsSupplier.get(toManyRelationships, toOneRelationships);
    }

    private SingleResourceRelationshipFutures supplyAsyncRelationshipDataFutures(
            REQUEST request,
            DATA_SOURCE_DTO dto
    ) {
        // to many relationships - basic resolvers
        Map<RelationshipName, CompletableFuture<ToManyRelationshipObject>> toManyRelationshipBasicFutures =
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
                                                        Map<DATA_SOURCE_DTO, ToManyRelationshipObject> batchResult = getBatchToManyRelationshipResolvers()
                                                                .get(rel)
                                                                .resolveRequestedData(request, Collections.singletonList(dto));
                                                        return MapUtils.emptyIfNull(batchResult).values().stream().findFirst().orElse(null);
                                                    }
                                                } catch (ResourceNotFoundException nfe) {
                                                    return createToManyRelationshipsWithEmptyData(rel, request, dto);
                                                }
                                            },
                                            executor
                                    );
                                }));

        // to one relationship - basic resolvers
        Map<RelationshipName, CompletableFuture<ToOneRelationshipObject>> toOneRelationshipBasicFutures =
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
                                                        Map<DATA_SOURCE_DTO, ToOneRelationshipObject> batchResult = getBatchToOneRelationshipResolvers()
                                                                .get(rel)
                                                                .resolveRequestedData(request, Collections.singletonList(dto));
                                                        return MapUtils.emptyIfNull(batchResult).values().stream().findFirst().orElse(null);
                                                    }
                                                } catch (ResourceNotFoundException nfe) {
                                                    return createToOneRelationshipWithNullData(rel, request, dto);
                                                }
                                            },
                                            executor
                                    );
                                }
                        ));

        return new SingleResourceRelationshipFutures(
                toManyRelationshipBasicFutures,
                toOneRelationshipBasicFutures
        );
    }

    private Map<RelationshipName, ToOneRelationshipObject> resolveToOneRelationships(Map<RelationshipName, CompletableFuture<ToOneRelationshipObject>> futures,
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
                                return createToOneRelationshipWithNullData(relName, request, dto);
                            }
                        }
                ));
    }

    private Map<RelationshipName, ToManyRelationshipObject> resolveToManyRelationships(
            Map<RelationshipName, CompletableFuture<ToManyRelationshipObject>> futures,
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
                                return createToManyRelationshipsWithNullData(relName, request, dto);
                            }
                        }
                ));
    }

    record SingleResourceRelationshipFutures(
            Map<RelationshipName, CompletableFuture<ToManyRelationshipObject>> toManyRelationshipBasicFutures,
            Map<RelationshipName, CompletableFuture<ToOneRelationshipObject>> toOneRelationshipBasicFutures) {
    }

}
