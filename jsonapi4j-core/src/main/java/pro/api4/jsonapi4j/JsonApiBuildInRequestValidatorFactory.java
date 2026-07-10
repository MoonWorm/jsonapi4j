package pro.api4.jsonapi4j;

import pro.api4.jsonapi4j.domain.DomainRegistry;

/**
 * Creates a {@link JsonApiBuildInRequestValidator} bound to a specific {@link DomainRegistry}.
 * <p>
 * The validator needs the <em>final</em> domain registry — the one that actually backs request dispatch, including
 * the built-in meta resources when the meta API is enabled. That registry is only known inside
 * {@link JsonApi4jBuilder#build()} (which augments it with meta), so the validator cannot be constructed up front
 * against the host-only registry without going stale. Integrations therefore contribute a <em>factory</em> instead
 * of a ready validator, and {@code build()} materializes the validator against the final registry it just assembled.
 * <p>
 * Custom implementations plug in exactly the same way: supply your own factory (e.g. via a bean) and it will be
 * handed the final registry.
 */
@FunctionalInterface
public interface JsonApiBuildInRequestValidatorFactory {

    /**
     * A factory that always yields the {@link JsonApiBuildInRequestValidator#NO_OP} validator, ignoring the registry.
     */
    JsonApiBuildInRequestValidatorFactory NO_OP = domainRegistry -> JsonApiBuildInRequestValidator.NO_OP;

    /**
     * @param domainRegistry the final domain registry backing request dispatch (meta-augmented when meta is enabled)
     * @return a validator bound to {@code domainRegistry}
     */
    JsonApiBuildInRequestValidator create(DomainRegistry domainRegistry);

}
