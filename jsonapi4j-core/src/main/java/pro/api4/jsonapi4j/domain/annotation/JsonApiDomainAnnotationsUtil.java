package pro.api4.jsonapi4j.domain.annotation;

import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.domain.Relationship;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.domain.exception.DomainMisconfigurationException;

public final class JsonApiDomainAnnotationsUtil {

    private JsonApiDomainAnnotationsUtil() {

    }

    public static ResourceType resolveResourceType(Class<?> resourceClass) {
        JsonApiResource jsonApiResource = resourceClass.getAnnotation(JsonApiResource.class);
        if (jsonApiResource == null) {
            throw new DomainMisconfigurationException("Each resource implementation must has " + JsonApiResource.class.getSimpleName() + " annotation placed on the type level.");
        }
        if (StringUtils.isBlank(jsonApiResource.resourceType())) {
            throw new DomainMisconfigurationException(JsonApiResource.class.getSimpleName() + " annotation 'resourceType()' parameter declaration must not be blank");
        }
        return new ResourceType(jsonApiResource.resourceType());
    }

    public static RelationshipName resolveRelationshipName(Class<?> relationshipClass) {
        JsonApiRelationship jsonApiRelationship = relationshipClass.getAnnotation(JsonApiRelationship.class);
        if (jsonApiRelationship == null) {
            throw new DomainMisconfigurationException("Each relationship implementation must has " + JsonApiRelationship.class.getSimpleName() + " annotation placed on the type level.");
        }
        if (StringUtils.isBlank(jsonApiRelationship.relationshipName())) {
            throw new DomainMisconfigurationException(JsonApiRelationship.class.getSimpleName() + " annotation 'relationshipName()' parameter declaration must not be blank");
        }
        return new RelationshipName(jsonApiRelationship.relationshipName());
    }

    public static ResourceType resolveParentResourceType(Class<? extends Relationship<?>> relationshipClass) {
        JsonApiRelationship jsonApiRelationship = relationshipClass.getAnnotation(JsonApiRelationship.class);
        if (jsonApiRelationship == null) {
            throw new DomainMisconfigurationException("Each relationship implementation must has " + JsonApiRelationship.class.getSimpleName() + " annotation placed on the type level.");
        }
        return resolveResourceType(jsonApiRelationship.parentResource());
    }

}
