package io.jsonapi4j.processor.resolvers.links.toplevel;

import io.jsonapi4j.domain.RelationshipName;
import io.jsonapi4j.domain.ResourceType;
import io.jsonapi4j.model.document.LinksObject;
import io.jsonapi4j.processor.IdSupplier;
import io.jsonapi4j.processor.resolvers.SingleDataItemDocLinksResolver;
import io.jsonapi4j.processor.resolvers.links.LinksGenerator;
import io.jsonapi4j.processor.resolvers.links.ResourceTypeSupplier;

public final class ToOneRelationshipLinksDefaultResolvers {

    private ToOneRelationshipLinksDefaultResolvers() {

    }

    public static <REQUEST, RELATIONSHIP_DTO> SingleDataItemDocLinksResolver<REQUEST, RELATIONSHIP_DTO> defaultLinksResolver(
            ResourceType parentResourceType,
            String parentResourceId,
            RelationshipName relationshipName,
            ResourceTypeSupplier<RELATIONSHIP_DTO> relationshipResourceTypeResolver,
            IdSupplier<RELATIONSHIP_DTO> relationshipIdSupplier
    ) {
        return (request, dataSourceDto) -> {
            LinksGenerator linkGenerator = new LinksGenerator(request);
            String selfLinkBasePath = LinksGenerator.relationshipBasePath(
                    parentResourceType,
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
