package pro.api4.jsonapi4j.processor.resolvers.links.toplevel;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.model.document.LinkObject;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.processor.IdSupplier;
import pro.api4.jsonapi4j.processor.resolvers.MultipleDataItemsDocLinksResolver;
import pro.api4.jsonapi4j.processor.resolvers.links.LinksGenerator;
import pro.api4.jsonapi4j.processor.resolvers.links.ResourceTypeSupplier;
import pro.api4.jsonapi4j.request.FiltersAwareRequest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ToManyRelationshipLinksDefaultResolvers {

    private ToManyRelationshipLinksDefaultResolvers() {

    }

    public static <REQUEST, RELATIONSHIP_DTO> MultipleDataItemsDocLinksResolver<REQUEST, RELATIONSHIP_DTO> defaultLinksResolver(
            ResourceType resourceType,
            String parentResourceId,
            RelationshipName relationshipName,
            ResourceTypeSupplier<RELATIONSHIP_DTO> relationshipResourceTypeResolver,
            IdSupplier<RELATIONSHIP_DTO> relationshipIdSupplier
    ) {
        return (request, dataSourceDtos, nextCursor) -> {
            LinksGenerator linksGenerator = new LinksGenerator(request);

            String relationshipBasePath = LinksGenerator.relationshipBasePath(resourceType, parentResourceId, relationshipName);

            String selfLink = linksGenerator.generateSelfLink(
                    relationshipBasePath,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true
            );

            Map<String, LinkObject> relatedLinks = null;
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
                                        e -> String.format("related:%s", e.getKey().getType()),
                                        e -> {
                                            List<String> ids = e.getValue()
                                                    .stream()
                                                    .map(ImmutablePair::getRight)
                                                    .sorted()
                                                    .toList();
                                            String href = String.format(
                                                    "%s?%s",
                                                    LinksGenerator.resourcesBasePath(e.getKey()),
                                                    FiltersAwareRequest.getFilterParamWithValue("id", ids)
                                            );
                                            return LinkObject.builder()
                                                    .href(href)
                                                    .meta(Map.of("ids", ids))
                                                    .build();
                                        }
                                )
                        );
            }

            String nextLink = linksGenerator.generateNextLink(
                    relationshipBasePath, nextCursor, true, true, true, true, true, true
            );

            return LinksObject.builder().self(selfLink).next(nextLink).putAll(relatedLinks).build();
        };
    }

}
