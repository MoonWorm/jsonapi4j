package pro.api4.jsonapi4j.processor.resolvers.links.toplevel;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import pro.api4.jsonapi4j.compatibility.JsonApi4jCompatibilityMode;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
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

    private static final String RELATED_LINKS_DESCRIBED_BY = "https://api4.pro/oas-schema-to-many-relationships-related-link.yaml";

    private ToManyRelationshipLinksDefaultResolvers() {

    }

    public static <REQUEST, RELATIONSHIP_DTO> MultipleDataItemsDocLinksResolver<REQUEST, RELATIONSHIP_DTO> defaultLinksResolver(
            ResourceType resourceType,
            String parentResourceId,
            RelationshipName relationshipName,
            ResourceTypeSupplier<RELATIONSHIP_DTO> relationshipResourceTypeResolver,
            IdSupplier<RELATIONSHIP_DTO> relationshipIdSupplier
    ) {
        return defaultLinksResolver(
                resourceType,
                parentResourceId,
                relationshipName,
                relationshipResourceTypeResolver,
                relationshipIdSupplier,
                JsonApi4jCompatibilityMode.STRICT
        );
    }

    public static <REQUEST, RELATIONSHIP_DTO> MultipleDataItemsDocLinksResolver<REQUEST, RELATIONSHIP_DTO> defaultLinksResolver(
            ResourceType resourceType,
            String parentResourceId,
            RelationshipName relationshipName,
            ResourceTypeSupplier<RELATIONSHIP_DTO> relationshipResourceTypeResolver,
            IdSupplier<RELATIONSHIP_DTO> relationshipIdSupplier,
            JsonApi4jCompatibilityMode compatibilityMode
    ) {
        return (request, dataSourceDtos, nextCursor) -> {
            LinksGenerator linksGenerator = new LinksGenerator(request, compatibilityMode);

            String relationshipBasePath = LinksGenerator.relationshipBasePath(resourceType, parentResourceId, relationshipName);

            String selfLink = linksGenerator.generateSelfLink(
                    relationshipBasePath,
                    true,
                    true,
                    true,
                    true,
                    true
            );

            Map<String, RelatedLinkObject> relatedLinks = null;
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
                                            return new RelatedLinkObject(
                                                    href,
                                                    RELATED_LINKS_DESCRIBED_BY,
                                                    Map.of("ids", ids)
                                            );
                                        })
                        );
            }

            String nextLink = linksGenerator.generateNextLink(
                    relationshipBasePath, nextCursor, true, true, true, true, true
            );

            return LinksObject.builder().self(selfLink).next(nextLink).related(relatedLinks).build();
        };
    }

    public record RelatedLinkObject(String href, String describedby, Map<String, Object> meta) {
    }

}
