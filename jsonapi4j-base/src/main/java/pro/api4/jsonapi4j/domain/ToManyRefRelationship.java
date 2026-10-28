package pro.api4.jsonapi4j.domain;

/**
 * A {@link ToManyRelationship} whose DTO is a {@link ResourceRef}.
 * <p>
 * Each related resource's {@code "type"} and {@code "id"} are resolved straight from the {@link ResourceRef}, so
 * in the most common case - a plain {@link DefaultResourceRef} - an implementation needs nothing but the
 * {@link pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship} annotation:
 * {@snippet :
 * @JsonApiRelationship(relationshipName = "citizenships", parentResource = UserResource.class)
 * public class UserCitizenshipsRelationship implements ToManyRefRelationship<ResourceRef> {
 * }
 * }
 * When the identifiers need more data, e.g. for their {@code "meta"} member, use your own DTO that implements
 * {@link ResourceRef} as {@code REF} and override only what is needed, e.g.
 * {@link #resolveResourceIdentifierMeta(pro.api4.jsonapi4j.request.JsonApiRequest, Object)}.
 *
 * @param <REF> the relationship DTO, a {@link ResourceRef} implementation
 */
public interface ToManyRefRelationship<REF extends ResourceRef> extends ToManyRelationship<REF> {

    @Override
    default String resolveResourceIdentifierType(REF resourceRef) {
        return resourceRef.type();
    }

    @Override
    default String resolveResourceIdentifierId(REF resourceRef) {
        return resourceRef.id();
    }

}
