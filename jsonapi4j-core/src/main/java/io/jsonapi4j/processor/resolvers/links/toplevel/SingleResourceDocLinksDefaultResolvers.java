package io.jsonapi4j.processor.resolvers.links.toplevel;

import io.jsonapi4j.domain.ResourceType;
import io.jsonapi4j.processor.resolvers.links.LinksGenerator;
import io.jsonapi4j.model.document.LinksObject;
import io.jsonapi4j.processor.resolvers.SingleDataItemDocLinksResolver;
import io.jsonapi4j.processor.IdSupplier;


public final class SingleResourceDocLinksDefaultResolvers {

    private SingleResourceDocLinksDefaultResolvers() {

    }

    public static <REQUEST, DATA_SOURCE_DTO> SingleDataItemDocLinksResolver<REQUEST, DATA_SOURCE_DTO> defaultTopLevelLinksResolver(
            ResourceType resourceType,
            IdSupplier<DATA_SOURCE_DTO> idSupplier
    ) {
        return (request, dataSourceDto) -> {
            String basePath = LinksGenerator.resourceBasePath(
                    resourceType,
                    () -> idSupplier.getId(dataSourceDto)
            );
            String selfLink = new LinksGenerator(request).generateSelfLink(
                    basePath, true, true, true, true, true
            );
            return LinksObject.builder().self(selfLink).build();
        };
    }


}
