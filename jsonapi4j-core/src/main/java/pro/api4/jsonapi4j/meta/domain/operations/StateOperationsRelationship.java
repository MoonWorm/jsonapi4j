package pro.api4.jsonapi4j.meta.domain.operations;

import pro.api4.jsonapi4j.domain.ToManyRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.meta.Ref;
import pro.api4.jsonapi4j.meta.domain.state.StateResource;

@JsonApiRelationship(relationshipName = OperationsResource.OPERATIONS, parentResource = StateResource.class)
public class StateOperationsRelationship implements ToManyRelationship<Ref> {

    @Override
    public String resolveResourceIdentifierType(Ref ref) {
        return ref.type();
    }

    @Override
    public String resolveResourceIdentifierId(Ref ref) {
        return ref.id();
    }

}
