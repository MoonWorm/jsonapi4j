package pro.api4.jsonapi4j.request;

import pro.api4.jsonapi4j.domain.RelationshipName;

public interface RelationshipAwareRequest {

    /**
     * For relationship-related operations only, e.g. GET /countries/NO/relationships/currencies.
     * <p>
     * This will hold the enum {@link RelationshipName} representation of the 'countries' -> 'currencies' relationship.
     *
     * @return relationship representation, can be <code>null</code>
     */
    RelationshipName getTargetRelationshipName();

}
