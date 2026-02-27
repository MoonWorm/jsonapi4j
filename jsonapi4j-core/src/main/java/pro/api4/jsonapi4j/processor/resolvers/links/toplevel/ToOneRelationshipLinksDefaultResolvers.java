package pro.api4.jsonapi4j.processor.resolvers.links.toplevel;

import pro.api4.jsonapi4j.compatibility.JsonApi4jCompatibilityMode;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.processor.IdSupplier;
import pro.api4.jsonapi4j.processor.resolvers.SingleDataItemDocLinksResolver;
import pro.api4.jsonapi4j.processor.resolvers.links.LinksGenerator;
import pro.api4.jsonapi4j.processor.resolvers.links.ResourceTypeSupplier;

public final class ToOneRelationshipLinksDefaultResolvers {

    private ToOneRelationshipLinksDefaultResolvers() {

    }

    public static <REQUEST, RELATIONSHIP_DTO> SingleDataItemDocLinksResolver<REQUEST, RELATIONSHIP_DTO> defaultLinksResolver(
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

    public static <REQUEST, RELATIONSHIP_DTO> SingleDataItemDocLinksResolver<REQUEST, RELATIONSHIP_DTO> defaultLinksResolver(
            ResourceType resourceType,
            String parentResourceId,
            RelationshipName relationshipName,
            ResourceTypeSupplier<RELATIONSHIP_DTO> relationshipResourceTypeResolver,
            IdSupplier<RELATIONSHIP_DTO> relationshipIdSupplier,
            JsonApi4jCompatibilityMode compatibilityMode
    ) {
        return (request, dataSourceDto) -> {
            LinksGenerator linkGenerator = new LinksGenerator(request, compatibilityMode);
            String selfLinkBasePath = LinksGenerator.relationshipBasePath(
                    resourceType,
                    parentResourceId,
                    relationshipName
            );
            String selfLink = linkGenerator.generateSelfLink(
                    selfLinkBasePath, true, true, true, true, true
            );

            String relatedLinkBasePath = LinksGenerator.resourceBasePath(
                    relationshipResourceTypeResolver.getResourceType(dataSourceDto),
                    () -> relationshipIdSupplier.getId(dataSourceDto)
            );
            String relatedLink = linkGenerator.generateRelatedLink(
                    relatedLinkBasePath, false, false, false, false, true
            );

            return LinksObject.builder().self(selfLink).related(relatedLink).build();
        };
    }

}
