package pro.api4.jsonapi4j.operation.validation;

import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.exception.ConstraintViolationException;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;

import java.text.MessageFormat;
import java.util.Set;
import java.util.function.Consumer;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

public class JsonApi4jDefaultValidator {

    public void validateNonNull(Object object, String parameter) {
        if (object == null) {
            throw new ConstraintViolationException("value can't be null", parameter);
        }
    }

    public void validateEqualTo(Object actual, Object expected, String parameter) {
        if (!actual.equals(expected)) {
            throw new ConstraintViolationException(MessageFormat.format("value should match {0}", expected), parameter);
        }
    }

    public void validateNonBlank(String value, String parameter) {
        if (StringUtils.isBlank(value)) {
            throw new ConstraintViolationException("value can't be blank", parameter);
        }
    }

    public void validateIsNull(Object value, String parameter) {
        if (value != null) {
            throw new ConstraintViolationException("value must be null", parameter);
        }
    }

    public void validateValueAnyOf(String value,
                                   Set<String> allowedValues,
                                   String parameterName) {
        for (String allowedValue : allowedValues) {
            if (allowedValue.equalsIgnoreCase(value)) {
                return;
            }
        }
        throw new ConstraintViolationException(
                MessageFormat.format("''{0}'' value is not allowed, available values: [{1}]", value, String.join(", ", allowedValues)),
                parameterName
        );
    }
    public static class SingleResourceDocValidationState<ATTRIBUTES, RELATIONSHIPS> {

        private final SingleResourceDoc<? extends ResourceObject<ATTRIBUTES, RELATIONSHIPS>> singleResourceDoc;
        private Consumer<String> resourceIdValidator;
        private Consumer<String> resourceTypeValidator;
        private Consumer<ATTRIBUTES> attributesValidator;
        private Consumer<RELATIONSHIPS> relationshipsValidator;

        public SingleResourceDocValidationState(SingleResourceDoc<? extends ResourceObject<ATTRIBUTES, RELATIONSHIPS>> singleResourceDoc) {
            this.singleResourceDoc = singleResourceDoc;
        }

        public SingleResourceDocValidationState<ATTRIBUTES, RELATIONSHIPS> withResourceIdValidator(Consumer<String> resourceIdValidator) {
            this.resourceIdValidator = resourceIdValidator;
            return this;
        }

        public SingleResourceDocValidationState<ATTRIBUTES, RELATIONSHIPS> withResourceTypeValidator(Consumer<String> resourceTypeValidator) {
            this.resourceTypeValidator = resourceTypeValidator;
            return this;
        }

        public SingleResourceDocValidationState<ATTRIBUTES, RELATIONSHIPS> withAttributesValidator(Consumer<ATTRIBUTES> attributesValidator) {
            this.attributesValidator = attributesValidator;
            return this;
        }

        public SingleResourceDocValidationState<ATTRIBUTES, RELATIONSHIPS> withRelationshipsValidator(Consumer<RELATIONSHIPS> relationshipsValidator) {
            this.relationshipsValidator = relationshipsValidator;
            return this;
        }

        public void validate() {
            if (resourceIdValidator != null && singleResourceDoc.getData().getId() != null) {
                resourceIdValidator.accept(singleResourceDoc.getData().getId());
            }
            if (relationshipsValidator != null) {
                resourceTypeValidator.accept(singleResourceDoc.getData().getType());
            }
            if (attributesValidator != null && singleResourceDoc.getData().getAttributes() != null) {
                attributesValidator.accept(singleResourceDoc.getData().getAttributes());
            }
            if (relationshipsValidator != null && singleResourceDoc.getData().getRelationships() != null) {
                relationshipsValidator.accept(singleResourceDoc.getData().getRelationships());
            }
        }

    }

    public <ATTRIBUTES, RELATIONSHIPS> SingleResourceDocValidationState<ATTRIBUTES, RELATIONSHIPS> validateSingleResourceDoc(SingleResourceDoc<? extends ResourceObject<ATTRIBUTES, RELATIONSHIPS>> singleResourceDoc) {
       return new SingleResourceDocValidationState<>(singleResourceDoc);
    }

    public void validateToOneRelationshipDoc(ToOneRelationshipDoc toOneRelationshipDoc,
                                             Consumer<String> resourceIdValidator,
                                             Consumer<String> resourceTypeValidator) {
        if (toOneRelationshipDoc.getData() != null) {
            resourceIdValidator.accept(toOneRelationshipDoc.getData().getId());
            resourceTypeValidator.accept(toOneRelationshipDoc.getData().getType());
        }
    }

    public void validateToOneRelationshipDoc(ToOneRelationshipDoc toOneRelationshipDoc,
                                             Consumer<String> resourceIdValidator,
                                             Consumer<String> resourceTypeValidator,
                                             Consumer<Object> metaValidator) {
        validateToOneRelationshipDoc(toOneRelationshipDoc, resourceIdValidator, resourceTypeValidator);
        if (toOneRelationshipDoc.getData() != null) {
            if (toOneRelationshipDoc.getData().getMeta() != null) {
                metaValidator.accept(toOneRelationshipDoc.getData().getMeta());
            }
        }
    }

    public void validateToManyRelationshipDoc(ToManyRelationshipsDoc toManyRelationshipDoc,
                                              Consumer<String> resourceIdValidator,
                                              Consumer<String> resourceTypeValidator) {
        emptyIfNull(toManyRelationshipDoc.getData()).forEach(ri -> {
            resourceIdValidator.accept(ri.getId());
            resourceTypeValidator.accept(ri.getType());
        });
    }

    // TODO: make it propagate INDEX so consumers can use it to compose proper parameter e.g. data[1]
    public void validateToManyRelationshipDoc(ToManyRelationshipsDoc toManyRelationshipDoc,
                                              Consumer<String> resourceIdValidator,
                                              Consumer<String> resourceTypeValidator,
                                              Consumer<Object> metaValidator) {
        validateToManyRelationshipDoc(toManyRelationshipDoc, resourceIdValidator, resourceTypeValidator);
        emptyIfNull(toManyRelationshipDoc.getData()).forEach(ri -> metaValidator.accept(ri.getMeta()));
    }

}
