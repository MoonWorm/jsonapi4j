package pro.api4.jsonapi4j.domain.annotation;

import pro.api4.jsonapi4j.domain.Resource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an interface as a JSON:API relationship definition.
 * <p>
 * Apply this annotation at the type level on any class or interface that implements
 * {@link pro.api4.jsonapi4j.domain.ToOneRelationship} or
 * {@link pro.api4.jsonapi4j.domain.ToManyRelationship}. The framework uses it at startup to
 * discover and register the relationship in the {@link pro.api4.jsonapi4j.domain.DomainRegistry},
 * associating it with its parent resource.
 * <p>
 * Example:
 * <pre>{@code
 * @JsonApiRelationship(relationshipName = "citizenships", parentResource = UserResource.class)
 * public class UserCitizenshipsRelationship implements ToManyRelationship<CountryEntity> { ... }
 * }</pre>
 *
 * @see pro.api4.jsonapi4j.domain.ToOneRelationship
 * @see pro.api4.jsonapi4j.domain.ToManyRelationship
 * @see pro.api4.jsonapi4j.operation.annotation.JsonApiRelationshipOperation
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonApiRelationship {

    /**
     * The name of the relationship as it appears in the JSON:API response, e.g.
     * {@code "citizenships"}, {@code "placeOfBirth"}, {@code "userProperties"}.
     * <p>
     * Must be unique within the parent resource.
     *
     * @return a String that represents the name of the relationship
     */
    String relationshipName();

    /**
     * The {@link Resource} implementation that owns this relationship.
     * Used by the framework to associate the relationship with its parent resource
     * in the {@link pro.api4.jsonapi4j.domain.DomainRegistry}.
     *
     * @return the parent resource class
     */
    Class<? extends Resource<?>> parentResource();

}
