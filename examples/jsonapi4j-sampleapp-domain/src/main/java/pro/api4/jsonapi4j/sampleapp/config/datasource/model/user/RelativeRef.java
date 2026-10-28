package pro.api4.jsonapi4j.sampleapp.config.datasource.model.user;

import pro.api4.jsonapi4j.domain.ResourceRef;

import static pro.api4.jsonapi4j.sampleapp.domain.user.UserResource.USERS;

/**
 * Reference to a relative of a user. Besides the relative's id it carries the kind of family relation, which the
 * {@code relatives} relationship exposes as the resource identifier's {@code meta}.
 */
public record RelativeRef(String id, RelationshipType relationshipType) implements ResourceRef {

    @Override
    public String type() {
        return USERS;
    }

    public enum RelationshipType {
        HUSBAND, WIFE, SON, DAUGHTER, MOTHER, FATHER, BROTHER
    }
}
