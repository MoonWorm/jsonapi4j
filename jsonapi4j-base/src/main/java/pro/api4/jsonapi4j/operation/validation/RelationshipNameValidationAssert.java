package pro.api4.jsonapi4j.operation.validation;

import pro.api4.jsonapi4j.domain.RelationshipName;

/**
 * Fluent assertion class for validating {@link RelationshipName} values in JSON:API requests.
 *
 * <p>Extends {@link ObjectValidationAssert} and provides a {@link #name()} accessor that navigates
 * into the underlying name string for further string-level validation.
 *
 * <p>Usage example:
 * {@snippet :
 *   JsonApiRequestValidator.forRequest(request)
 *       .path(p -> p.withRelationshipNameValidator(rn -> rn.name().isOneOf("author", "comments")))
 *       .validate();
 * }
 *
 * @see ObjectValidationAssert
 * @see StringValidationAssert
 */
public class RelationshipNameValidationAssert extends ObjectValidationAssert<RelationshipNameValidationAssert, RelationshipName> {

    RelationshipNameValidationAssert(RelationshipName actual, ErrorSources.Source source) {
        super(actual, source);
    }

    /** Navigates into the relationship name's string value for further string-level validation. */
    public StringValidationAssert name() {
        return field("name", RelationshipName::getName).asString();
    }

}
