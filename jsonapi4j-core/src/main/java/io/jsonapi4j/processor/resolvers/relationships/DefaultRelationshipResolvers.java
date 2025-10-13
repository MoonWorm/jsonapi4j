package io.jsonapi4j.processor.resolvers.relationships;

import io.jsonapi4j.processor.IdSupplier;
import io.jsonapi4j.processor.resolvers.DefaultRelationshipResolver;
import io.jsonapi4j.processor.resolvers.links.LinksGenerator;
import io.jsonapi4j.domain.RelationshipName;
import io.jsonapi4j.domain.ResourceType;
import io.jsonapi4j.model.document.LinksObject;
import io.jsonapi4j.model.document.data.BaseDoc;

import java.util.Arrays;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public final class DefaultRelationshipResolvers {

    private DefaultRelationshipResolvers() {

    }

    public static <REQUEST, DATA_SOURCE_DTO> DefaultRelationshipResolver<REQUEST, DATA_SOURCE_DTO> defaultRelationshipResolver(
            ResourceType parentResourceType,
            IdSupplier<DATA_SOURCE_DTO> idSupplier
    ) {
        return (relationship, request, dataSourceDto) -> {
            String relationshipBasePath = LinksGenerator.relationshipBasePath(
                    parentResourceType,
                    idSupplier.getId(dataSourceDto),
                    relationship
            );
            String selfLink = new LinksGenerator(request).generateSelfLink(
                    relationshipBasePath, false, false, false, false, true
            );
            return new BaseDoc(LinksObject.builder().self(selfLink).build());
        };
    }

    public static <REQUEST, DATA_SOURCE_DTO> Map<RelationshipName, DefaultRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> all(ResourceType parentResourceType,
                                                                                                                              IdSupplier<DATA_SOURCE_DTO> idSupplier,
                                                                                                                              RelationshipName[] relationshipNames) {
        return Arrays.stream(relationshipNames)
                .collect(
                        toMap(
                                r -> r,
                                r -> defaultRelationshipResolver(
                                        parentResourceType,
                                        idSupplier
                                )
                        )
                );
    }

}
