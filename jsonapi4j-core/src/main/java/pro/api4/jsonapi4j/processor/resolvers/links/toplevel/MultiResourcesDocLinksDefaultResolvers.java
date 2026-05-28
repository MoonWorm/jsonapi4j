package pro.api4.jsonapi4j.processor.resolvers.links.toplevel;

import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.processor.resolvers.links.LinksGenerator;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.processor.resolvers.MultipleDataItemsDocLinksResolver;

public final class MultiResourcesDocLinksDefaultResolvers {

    private MultiResourcesDocLinksDefaultResolvers() {

    }

    public static <REQUEST, DATA_SOURCE_DTO> MultipleDataItemsDocLinksResolver<REQUEST, DATA_SOURCE_DTO> defaultTopLevelLinksResolver(ResourceType resourceType) {
        return (request, dataSourceDtos, paginationContext) -> {
            String basePath = LinksGenerator.resourcesBasePath(resourceType);
            LinksGenerator linksGenerator = new LinksGenerator(request);
            String selfLink = linksGenerator.generateSelfLink(
                    basePath, true, true, true, true, true, true
            );
            String firstLink = linksGenerator.generateFirstLink(
                    basePath, paginationContext, true, true, true, true, true
            );
            String prevLink = linksGenerator.generatePrevLink(
                    basePath, paginationContext, true, true, true, true, true
            );
            String nextLink = linksGenerator.generateNextLink(
                    basePath, paginationContext, true, true, true, true, true
            );
            String lastLink = linksGenerator.generateLastLink(
                    basePath, paginationContext, true, true, true, true, true
            );
            return LinksObject.builder()
                    .self(selfLink)
                    .first(firstLink)
                    .prev(prevLink)
                    .next(nextLink)
                    .last(lastLink)
                    .build();
        };
    }

}
