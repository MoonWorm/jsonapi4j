package pro.api4.jsonapi4j.processor.resolvers.links.toplevel;

import pro.api4.jsonapi4j.compatibility.JsonApi4jCompatibilityMode;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.processor.resolvers.links.LinksGenerator;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.processor.resolvers.SingleDataItemDocLinksResolver;
import pro.api4.jsonapi4j.processor.IdSupplier;


public final class SingleResourceDocLinksDefaultResolvers {

    private SingleResourceDocLinksDefaultResolvers() {

    }

    public static <REQUEST, DATA_SOURCE_DTO> SingleDataItemDocLinksResolver<REQUEST, DATA_SOURCE_DTO> defaultTopLevelLinksResolver(
            ResourceType resourceType,
            IdSupplier<DATA_SOURCE_DTO> idSupplier
    ) {
        return defaultTopLevelLinksResolver(resourceType, idSupplier, JsonApi4jCompatibilityMode.STRICT);
    }

    public static <REQUEST, DATA_SOURCE_DTO> SingleDataItemDocLinksResolver<REQUEST, DATA_SOURCE_DTO> defaultTopLevelLinksResolver(
            ResourceType resourceType,
            IdSupplier<DATA_SOURCE_DTO> idSupplier,
            JsonApi4jCompatibilityMode compatibilityMode
    ) {
        return (request, dataSourceDto) -> {
            String basePath = LinksGenerator.resourceBasePath(
                    resourceType,
                    () -> idSupplier.getId(dataSourceDto)
            );
            String selfLink = new LinksGenerator(request, compatibilityMode).generateSelfLink(
                    basePath, true, true, true, true, true
            );
            return LinksObject.builder().self(selfLink).build();
        };
    }


}
