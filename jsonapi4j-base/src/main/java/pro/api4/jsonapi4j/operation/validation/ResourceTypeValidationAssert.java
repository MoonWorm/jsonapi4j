package pro.api4.jsonapi4j.operation.validation;

import pro.api4.jsonapi4j.domain.ResourceType;

public class ResourceTypeValidationAssert extends ObjectValidationAssert<ResourceTypeValidationAssert, ResourceType> {

    ResourceTypeValidationAssert(ResourceType actual, ErrorSources.Source source) {
        super(actual, source);
    }

    public StringValidationAssert type() {
        return field("type", ResourceType::getType).asString();
    }

}
