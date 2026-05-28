package pro.api4.jsonapi4j.domain.annotation;

import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.domain.Relationship;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.domain.exception.DomainMisconfigurationException;
import pro.api4.jsonapi4j.util.ReflectionUtils;

/**
 * Internal utility class for resolving JSON:API domain metadata from class-level annotations.
 * <p>
 * Used by the {@code DomainRegistry} builder to extract {@link ResourceType} and
 * {@link RelationshipName} values from the {@link JsonApiResource} and
 * {@link JsonApiRelationship} annotations at startup time.
 */
public final class JsonApiDomainAnnotationsUtil {

    private JsonApiDomainAnnotationsUtil() {

    }

    /**
     * Reads the {@link JsonApiResource} annotation from the given class and returns its
     * {@code resourceType} as a {@link ResourceType}.
     *
     * @param resourceClass the class annotated with {@link JsonApiResource}
     * @return the resolved {@link ResourceType}
     * @throws pro.api4.jsonapi4j.domain.exception.DomainMisconfigurationException if the annotation is
     *         absent or its {@code resourceType} is blank
     */
    public static ResourceType resolveResourceType(Class<?> resourceClass) {
        JsonApiResource jsonApiResource = ReflectionUtils.findAnnotationForClass(resourceClass, JsonApiResource.class);
        if (jsonApiResource == null) {
            throw new DomainMisconfigurationException("Each resource implementation must has @" + JsonApiResource.class.getSimpleName() + " annotation placed on the type level.");
        }
        if (StringUtils.isBlank(jsonApiResource.resourceType())) {
            throw new DomainMisconfigurationException(JsonApiResource.class.getSimpleName() + " annotation 'resourceType()' parameter declaration must not be blank");
        }
        return new ResourceType(jsonApiResource.resourceType());
    }

    /**
     * Reads the {@link JsonApiRelationship} annotation from the given class and returns its
     * {@code relationshipName} as a {@link RelationshipName}.
     *
     * @param relationshipClass the class annotated with {@link JsonApiRelationship}
     * @return the resolved {@link RelationshipName}
     * @throws pro.api4.jsonapi4j.domain.exception.DomainMisconfigurationException if the annotation is
     *         absent or its {@code relationshipName} is blank
     */
    public static RelationshipName resolveRelationshipName(Class<?> relationshipClass) {
        JsonApiRelationship jsonApiRelationship = ReflectionUtils.findAnnotationForClass(relationshipClass, JsonApiRelationship.class);
        if (jsonApiRelationship == null) {
            throw new DomainMisconfigurationException("Each relationship implementation must has " + JsonApiRelationship.class.getSimpleName() + " annotation placed on the type level.");
        }
        if (StringUtils.isBlank(jsonApiRelationship.relationshipName())) {
            throw new DomainMisconfigurationException(JsonApiRelationship.class.getSimpleName() + " annotation 'relationshipName()' parameter declaration must not be blank");
        }
        return new RelationshipName(jsonApiRelationship.relationshipName());
    }

    /**
     * Reads the {@link JsonApiRelationship} annotation from the given relationship class and
     * returns the {@link ResourceType} of its {@link JsonApiRelationship#parentResource()}.
     *
     * @param relationshipClass the relationship class annotated with {@link JsonApiRelationship}
     * @return the {@link ResourceType} of the parent resource
     * @throws pro.api4.jsonapi4j.domain.exception.DomainMisconfigurationException if the annotation is
     *         absent or the parent resource class is missing its {@link JsonApiResource} annotation
     */
    public static ResourceType resolveParentResourceType(Class<? extends Relationship<?>> relationshipClass) {
        JsonApiRelationship jsonApiRelationship = ReflectionUtils.findAnnotationForClass(relationshipClass, JsonApiRelationship.class);
        if (jsonApiRelationship == null) {
            throw new DomainMisconfigurationException("Each relationship implementation must has " + JsonApiRelationship.class.getSimpleName() + " annotation placed on the type level.");
        }
        return resolveResourceType(jsonApiRelationship.parentResource());
    }

}
