package pro.api4.jsonapi4j.processor.resolvers.relationships;

import pro.api4.jsonapi4j.compatibility.JsonApi4jCompatibilityMode;
import pro.api4.jsonapi4j.processor.IdSupplier;
import pro.api4.jsonapi4j.processor.resolvers.DefaultRelationshipResolver;
import pro.api4.jsonapi4j.processor.resolvers.links.LinksGenerator;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.BaseDoc;

import java.util.Arrays;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public final class DefaultRelationshipResolvers {

    private DefaultRelationshipResolvers() {

    }

    public static <REQUEST, DATA_SOURCE_DTO> DefaultRelationshipResolver<REQUEST, DATA_SOURCE_DTO> defaultRelationshipResolver(
            ResourceType resourceType,
            IdSupplier<DATA_SOURCE_DTO> idSupplier
    ) {
        return defaultRelationshipResolver(resourceType, idSupplier, JsonApi4jCompatibilityMode.STRICT);
    }

    public static <REQUEST, DATA_SOURCE_DTO> DefaultRelationshipResolver<REQUEST, DATA_SOURCE_DTO> defaultRelationshipResolver(
            ResourceType resourceType,
            IdSupplier<DATA_SOURCE_DTO> idSupplier,
            JsonApi4jCompatibilityMode compatibilityMode
    ) {
        return (relationship, request, dataSourceDto) -> {
            String relationshipBasePath = LinksGenerator.relationshipBasePath(
                    resourceType,
                    idSupplier.getId(dataSourceDto),
                    relationship
            );
            String selfLink = new LinksGenerator(request, compatibilityMode).generateSelfLink(
                    relationshipBasePath, false, false, false, false, true
            );
            return new BaseDoc(LinksObject.builder().self(selfLink).build());
        };
    }

    public static <REQUEST, DATA_SOURCE_DTO> Map<RelationshipName, DefaultRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> all(ResourceType resourceType,
                                                                                                                              IdSupplier<DATA_SOURCE_DTO> idSupplier,
                                                                                                                              RelationshipName[] relationshipNames) {
        return all(resourceType, idSupplier, relationshipNames, JsonApi4jCompatibilityMode.STRICT);
    }

    public static <REQUEST, DATA_SOURCE_DTO> Map<RelationshipName, DefaultRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> all(
            ResourceType resourceType,
            IdSupplier<DATA_SOURCE_DTO> idSupplier,
            RelationshipName[] relationshipNames,
            JsonApi4jCompatibilityMode compatibilityMode
    ) {
        return Arrays.stream(relationshipNames)
                .collect(
                        toMap(
                                r -> r,
                                r -> defaultRelationshipResolver(
                                        resourceType,
                                        idSupplier,
                                        compatibilityMode
                                )
                        )
                );
    }

}
