package io.jsonapi4j.processor.resolvers.links.resource;

import io.jsonapi4j.domain.ResourceType;
import io.jsonapi4j.processor.resolvers.links.LinksGenerator;
import io.jsonapi4j.model.document.LinksObject;
import io.jsonapi4j.processor.resolvers.ResourceLinksResolver;
import io.jsonapi4j.processor.IdSupplier;

public final class ResourceLinksDefaultResolvers {

    private ResourceLinksDefaultResolvers() {

    }

    public static <REQUEST, DATA_SOURCE_DTO> ResourceLinksResolver<REQUEST, DATA_SOURCE_DTO> defaultResourceLinksResolver(
            ResourceType resourceType,
            IdSupplier<DATA_SOURCE_DTO> idSupplier
    ) {
        return (request, dataSourceDto) -> {
            String basePath = LinksGenerator.resourceBasePath(resourceType, () -> idSupplier.getId(dataSourceDto));
            String selfLink = new LinksGenerator(request)
                    .generateSelfLink(basePath, false, false, false,false, true);
            return LinksObject.builder().self(selfLink).build();
        };
    }

}
