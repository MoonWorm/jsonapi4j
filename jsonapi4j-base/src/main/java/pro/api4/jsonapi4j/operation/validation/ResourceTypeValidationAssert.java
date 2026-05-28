package pro.api4.jsonapi4j.operation.validation;

import pro.api4.jsonapi4j.domain.ResourceType;

/**
 * Fluent assertion class for validating {@link ResourceType} values in JSON:API requests.
 *
 * <p>Extends {@link ObjectValidationAssert} and provides a {@link #type()} accessor that navigates
 * into the underlying type string for further string-level validation.
 *
 * <p>Usage example:
 * {@snippet :
 *   JsonApiRequestValidator.forRequest(request)
 *       .path(p -> p.withResourceTypeValidator(rt -> rt.type().isOneOf("users", "articles")))
 *       .validate();
 * }
 *
 * @see ObjectValidationAssert
 * @see StringValidationAssert
 */
public class ResourceTypeValidationAssert extends ObjectValidationAssert<ResourceTypeValidationAssert, ResourceType> {

    ResourceTypeValidationAssert(ResourceType actual, ErrorSources.Source source) {
        super(actual, source);
    }

    /** Navigates into the resource type's string value for further string-level validation. */
    public StringValidationAssert type() {
        return field("type", ResourceType::getType).asString();
    }

}
