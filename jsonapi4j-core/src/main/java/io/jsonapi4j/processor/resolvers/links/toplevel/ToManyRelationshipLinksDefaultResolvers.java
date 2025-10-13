package io.jsonapi4j.processor.resolvers.links.toplevel;

import io.jsonapi4j.domain.RelationshipName;
import io.jsonapi4j.domain.ResourceType;
import io.jsonapi4j.request.FiltersAwareRequest;
import io.jsonapi4j.model.document.LinksObject;
import io.jsonapi4j.processor.resolvers.MultipleDataItemsDocLinksResolver;
import io.jsonapi4j.processor.IdSupplier;
import io.jsonapi4j.processor.resolvers.links.LinksGenerator;
import io.jsonapi4j.processor.resolvers.links.ResourceTypeSupplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Map;
import java.util.stream.Collectors;

public final class ToManyRelationshipLinksDefaultResolvers {

    private ToManyRelationshipLinksDefaultResolvers() {

    }

    public static <REQUEST, RELATIONSHIP_DTO> MultipleDataItemsDocLinksResolver<REQUEST, RELATIONSHIP_DTO> defaultLinksResolver(
            ResourceType parentResourceType,
            String parentResourceId,
            RelationshipName relationshipName,
            ResourceTypeSupplier<RELATIONSHIP_DTO> relationshipResourceTypeResolver,
            IdSupplier<RELATIONSHIP_DTO> relationshipIdSupplier
    ) {
        return (request, dataSourceDtos, nextCursor) -> {
            LinksGenerator linksGenerator = new LinksGenerator(request);

            String relationshipBasePath = LinksGenerator.relationshipBasePath(parentResourceType, parentResourceId, relationshipName);

            String selfLink = linksGenerator.generateSelfLink(
                    relationshipBasePath,
                    true,
                    true,
                    true,
                    true,
                    true
            );

            Map<String, String> relatedLinks = null;
            if (dataSourceDtos != null) {
                relatedLinks = dataSourceDtos.stream().map(dto -> {
                            ResourceType type = relationshipResourceTypeResolver.getResourceType(dto);
                            String id = relationshipIdSupplier.getId(dto);
                            return ImmutablePair.of(type, id);
                        })
                        .filter(p -> p.getLeft() != null && StringUtils.isNotBlank(p.getRight()))
                        .collect(
                                Collectors.groupingBy(
                                        ImmutablePair::getLeft,
                                        Collectors.toSet()
                                )
                        ).entrySet().stream().collect(
                                Collectors.toMap(
                                        e -> e.getKey().getType(),
                                        e -> String.format(
                                                "%s?%s",
                                                LinksGenerator.resourcesBasePath(e.getKey()),
                                                FiltersAwareRequest.getFilterParamWithValue("id", e.getValue().stream().map(ImmutablePair::getRight).sorted().toList())
                                        ))
                        );
            }

            String nextLink = linksGenerator.generateNextLink(
                    relationshipBasePath, nextCursor, true, true, true, true, true
            );

            return LinksObject.builder().self(selfLink).next(nextLink).related(relatedLinks).build();
        };
    }

}
