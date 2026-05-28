package pro.api4.jsonapi4j.operation.annotation;

import pro.api4.jsonapi4j.domain.Relationship;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Associates a relationship operation implementation with its JSON:API relationship definition.
 * <p>
 * Apply this annotation at the type level on any class that implements one or more relationship
 * operation interfaces ({@link pro.api4.jsonapi4j.operation.ReadToOneRelationshipOperation},
 * {@link pro.api4.jsonapi4j.operation.ReadToManyRelationshipOperation},
 * {@link pro.api4.jsonapi4j.operation.UpdateToOneRelationshipOperation},
 * {@link pro.api4.jsonapi4j.operation.UpdateToManyRelationshipOperation},
 * {@link pro.api4.jsonapi4j.operation.AddToManyRelationshipOperation},
 * {@link pro.api4.jsonapi4j.operation.DeleteToManyRelationshipOperation}, or the composite
 * {@link pro.api4.jsonapi4j.operation.ToOneRelationshipOperations} /
 * {@link pro.api4.jsonapi4j.operation.ToManyRelationshipOperations}).
 * The framework uses this annotation at startup to register the operation
 * in the {@link pro.api4.jsonapi4j.operation.OperationsRegistry} under the correct relationship.
 * <p>
 * Example:
 * <pre>{@code
 * @JsonApiRelationshipOperation(relationship = UserCitizenshipsRelationship.class)
 * public class UserCitizenshipsOperations implements ToManyRelationshipOperations<UserEntity, CountryEntity> { ... }
 * }</pre>
 *
 * @see pro.api4.jsonapi4j.operation.ToOneRelationshipOperations
 * @see pro.api4.jsonapi4j.operation.ToManyRelationshipOperations
 * @see pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonApiRelationshipOperation {

    /**
     * The {@link Relationship} implementation this operation belongs to.
     * Used by the framework to look up the relationship during operation registration.
     *
     * @return the relationship class annotated with {@link pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship}
     */
    Class<? extends Relationship<?>> relationship();

}
