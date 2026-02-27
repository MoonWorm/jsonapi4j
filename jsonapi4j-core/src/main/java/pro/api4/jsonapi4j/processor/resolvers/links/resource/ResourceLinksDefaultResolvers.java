package pro.api4.jsonapi4j.processor.resolvers.links.resource;

import pro.api4.jsonapi4j.compatibility.JsonApi4jCompatibilityMode;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.processor.resolvers.links.LinksGenerator;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.processor.resolvers.ResourceLinksResolver;
import pro.api4.jsonapi4j.processor.IdSupplier;

public final class ResourceLinksDefaultResolvers {

    private ResourceLinksDefaultResolvers() {

    }

    public static <REQUEST, DATA_SOURCE_DTO> ResourceLinksResolver<REQUEST, DATA_SOURCE_DTO> defaultResourceLinksResolver(
            ResourceType resourceType,
            IdSupplier<DATA_SOURCE_DTO> idSupplier
    ) {
        return defaultResourceLinksResolver(resourceType, idSupplier, JsonApi4jCompatibilityMode.STRICT);
    }

    public static <REQUEST, DATA_SOURCE_DTO> ResourceLinksResolver<REQUEST, DATA_SOURCE_DTO> defaultResourceLinksResolver(
            ResourceType resourceType,
            IdSupplier<DATA_SOURCE_DTO> idSupplier,
            JsonApi4jCompatibilityMode compatibilityMode
    ) {
        return (request, dataSourceDto) -> {
            String basePath = LinksGenerator.resourceBasePath(resourceType, () -> idSupplier.getId(dataSourceDto));
            String selfLink = new LinksGenerator(request, compatibilityMode)
                    .generateSelfLink(basePath, false, false, false,false, true);
            return LinksObject.builder().self(selfLink).build();
        };
    }

}
