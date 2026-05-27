package pro.api4.jsonapi4j.operation.validation;

import pro.api4.jsonapi4j.domain.RelationshipName;

public class RelationshipNameValidationAssert extends ObjectValidationAssert<RelationshipNameValidationAssert, RelationshipName> {

    RelationshipNameValidationAssert(RelationshipName actual, ErrorSources.Source source) {
        super(actual, source);
    }

    public StringValidationAssert name() {
        return field("name", RelationshipName::getName).asString();
    }

}
