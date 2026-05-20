package pro.api4.jsonapi4j.operation.validation;

import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.exception.JsonApi4jException;
import pro.api4.jsonapi4j.exception.JsonApiRequestValidationException;
import pro.api4.jsonapi4j.model.document.data.RelationshipObject;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipObject;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipObject;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;

import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

public class JsonApi4jDefaultValidator {

    private static void validateResourceIdentifier(ResourceIdentifierObject resourceIdentifier,
                                                   ErrorSources.PayloadSources.PayloadDataSources initPath,
                                                   Consumer<String> resourceIdValidator,
                                                   Consumer<String> resourceTypeValidator,
                                                   Consumer<Object> metaValidator) {
        if (resourceIdValidator != null && resourceIdentifier.getId() != null) {
            try {
                resourceIdValidator.accept(resourceIdentifier.getId());
            } catch (JsonApiRequestValidationException e) {
                throw JsonApiRequestValidationException.withParameter(e, initPath.id());
            } catch (JsonApi4jException e) {
                throw e;
            } catch (Exception e) {
                throw new JsonApiRequestValidationException(
                        DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                        e.getMessage(),
                        initPath.id()
                );
            }
        }
        if (resourceTypeValidator != null) {
            try {
                resourceTypeValidator.accept(resourceIdentifier.getType());
            } catch (JsonApiRequestValidationException e) {
                throw JsonApiRequestValidationException.withParameter(e, initPath.type());
            } catch (JsonApi4jException e) {
                throw e;
            } catch (Exception e) {
                throw new JsonApiRequestValidationException(
                        DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                        e.getMessage(),
                        initPath.type()
                );
            }
        }
        if (metaValidator != null) {
            try {
                metaValidator.accept(resourceIdentifier.getMeta());
            } catch (JsonApiRequestValidationException e) {
                throw JsonApiRequestValidationException.withParameter(e, initPath.meta());
            } catch (JsonApi4jException e) {
                throw e;
            } catch (Exception e) {
                throw new JsonApiRequestValidationException(
                        DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                        e.getMessage(),
                        initPath.meta()
                );
            }
        }
    }

    public void validateNonNull(Object object, ErrorSources.ParameterPath parameterPath) {
        if (object == null) {
            throw new JsonApiRequestValidationException("value can't be null", parameterPath);
        }
    }

    public void validateEqualTo(Object actual, Object expected, ErrorSources.ParameterPath parameterPath) {
        if (!actual.equals(expected)) {
            throw new JsonApiRequestValidationException(MessageFormat.format("value should match {0}", expected), parameterPath);
        }
    }

    public void validateNonBlank(String value, ErrorSources.ParameterPath parameterPath) {
        if (StringUtils.isBlank(value)) {
            throw new JsonApiRequestValidationException("value can't be blank", parameterPath);
        }
    }

    public void validateIsNull(Object value, ErrorSources.ParameterPath parameterPath) {
        if (value != null) {
            throw new JsonApiRequestValidationException("value must be null", parameterPath);
        }
    }

    public void validateValueAnyOf(String value,
                                   Set<String> allowedValues,
                                   ErrorSources.ParameterPath parameterPath) {
        for (String allowedValue : allowedValues) {
            if (allowedValue.equalsIgnoreCase(value)) {
                return;
            }
        }
        throw new JsonApiRequestValidationException(
                MessageFormat.format("''{0}'' value is not allowed, available values: [{1}]", value, String.join(", ", allowedValues)),
                parameterPath
        );
    }

    public void validateValueAnyOf(String value,
                                   Set<String> allowedValues) {
        validateValueAnyOf(value, allowedValues, null);
    }

    public <ATTRIBUTES> SingleResourceDocValidationState<ATTRIBUTES> validateSingleResourceDoc(SingleResourceDoc<? extends ResourceObject<ATTRIBUTES, LinkedHashMap<String, RelationshipObject>>> singleResourceDoc) {
        return new SingleResourceDocValidationState<>(singleResourceDoc);
    }

    public ToOneRelationshipObjectValidationState validateToOneRelationshipObject() {
        return new ToOneRelationshipObjectValidationState();
    }

    public ToManyRelationshipObjectValidationState validateToManyRelationshipsObject() {
        return new ToManyRelationshipObjectValidationState();
    }

    public static class SingleResourceDocValidationState<ATTRIBUTES> {

        private final SingleResourceDoc<? extends ResourceObject<ATTRIBUTES, LinkedHashMap<String, RelationshipObject>>> singleResourceDoc;
        private final Map<String, ToOneRelationshipObjectValidationState> toOneRelationshipValidators = new LinkedHashMap<>();
        private final Map<String, ToManyRelationshipObjectValidationState> toManyRelationshipValidators = new LinkedHashMap<>();
        private Consumer<String> resourceIdValidator;
        private Consumer<String> resourceTypeValidator;
        private Consumer<ATTRIBUTES> attributesValidator;

        public SingleResourceDocValidationState(SingleResourceDoc<? extends ResourceObject<ATTRIBUTES, LinkedHashMap<String, RelationshipObject>>> singleResourceDoc) {
            this.singleResourceDoc = singleResourceDoc;
        }

        public SingleResourceDocValidationState<ATTRIBUTES> withResourceIdValidator(Consumer<String> resourceIdValidator) {
            this.resourceIdValidator = resourceIdValidator;
            return this;
        }

        public SingleResourceDocValidationState<ATTRIBUTES> withResourceTypeValidator(Consumer<String> resourceTypeValidator) {
            this.resourceTypeValidator = resourceTypeValidator;
            return this;
        }

        public SingleResourceDocValidationState<ATTRIBUTES> withAttributesValidator(Consumer<ATTRIBUTES> attributesValidator) {
            this.attributesValidator = attributesValidator;
            return this;
        }

        public SingleResourceDocValidationState<ATTRIBUTES> withToOneRelationshipValidator(String relationshipName, ToOneRelationshipObjectValidationState relationshipValidator) {
            this.toOneRelationshipValidators.put(relationshipName, relationshipValidator);
            return this;
        }

        public SingleResourceDocValidationState<ATTRIBUTES> withToManyRelationshipValidator(String relationshipName, ToManyRelationshipObjectValidationState relationshipValidator) {
            this.toManyRelationshipValidators.put(relationshipName, relationshipValidator);
            return this;
        }

        public void validate() {
            if (resourceIdValidator != null && singleResourceDoc.getData().getId() != null) {
                try {
                    resourceIdValidator.accept(singleResourceDoc.getData().getId());
                } catch (JsonApiRequestValidationException e) {
                    throw JsonApiRequestValidationException.withParameter(e, ErrorSources.payload().data().id());
                } catch (JsonApi4jException e) {
                    throw e;
                } catch (Exception e) {
                    throw new JsonApiRequestValidationException(
                            DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                            e.getMessage(),
                            ErrorSources.payload().data().id()
                    );
                }
            }
            if (resourceTypeValidator != null) {
                try {
                    resourceTypeValidator.accept(singleResourceDoc.getData().getType());
                } catch (JsonApiRequestValidationException e) {
                    throw JsonApiRequestValidationException.withParameter(e, ErrorSources.payload().data().type());
                } catch (JsonApi4jException e) {
                    throw e;
                } catch (Exception e) {
                    throw new JsonApiRequestValidationException(
                            DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                            e.getMessage(),
                            ErrorSources.payload().data().type()
                    );
                }
            }
            if (attributesValidator != null) {
                try {
                    attributesValidator.accept(singleResourceDoc.getData().getAttributes());
                } catch (JsonApiRequestValidationException e) {
                    throw JsonApiRequestValidationException.withParameter(e, ErrorSources.payload().data().attributes());
                } catch (JsonApi4jException e) {
                    throw e;
                } catch (Exception e) {
                    throw new JsonApiRequestValidationException(
                            DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                            e.getMessage(),
                            ErrorSources.payload().data().attributes()
                    );
                }
            }
            if (!toOneRelationshipValidators.isEmpty() && singleResourceDoc.getData().getRelationships() != null) {
                toOneRelationshipValidators.forEach((relationshipName, relationshipValidator) -> {
                    try {
                        ToOneRelationshipObject relationshipObject = (ToOneRelationshipObject) singleResourceDoc.getData().getRelationships().get(relationshipName);
                        if (relationshipObject != null && relationshipObject.getData() != null) {
                            relationshipValidator.setInitPath(ErrorSources.payload().data().relationship(relationshipName));
                            relationshipValidator.validate(relationshipObject);
                        }
                    } catch (JsonApiRequestValidationException e) {
                        throw JsonApiRequestValidationException.withParameter(e, ErrorSources.payload().data().relationship(relationshipName).toParameter());
                    } catch (JsonApi4jException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new JsonApiRequestValidationException(
                                DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                                e.getMessage(),
                                ErrorSources.payload().data().relationship(relationshipName).toParameter()
                        );
                    }
                });
                toManyRelationshipValidators.forEach((relationshipName, relationshipValidator) -> {
                    try {
                        ToManyRelationshipObject relationshipObject = (ToManyRelationshipObject) singleResourceDoc.getData().getRelationships().get(relationshipName);
                        if (relationshipObject != null) {
                            relationshipValidator.setInitPath(ErrorSources.payload().data().relationship(relationshipName));
                            relationshipValidator.validate(relationshipObject);
                        }
                    } catch (JsonApiRequestValidationException e) {
                        throw JsonApiRequestValidationException.withParameter(e, ErrorSources.payload().data().relationship(relationshipName).toParameter());
                    } catch (JsonApi4jException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new JsonApiRequestValidationException(
                                DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                                e.getMessage(),
                                ErrorSources.payload().data().relationship(relationshipName).toParameter()
                        );
                    }
                });
            }
        }

    }

    public static class ToOneRelationshipObjectValidationState {

        private ErrorSources.PayloadSources.PayloadDataSources initPath;

        private Consumer<String> resourceIdValidator;
        private Consumer<String> resourceTypeValidator;
        private Consumer<Object> metaValidator;

        private ToOneRelationshipObjectValidationState() {
            this.initPath = ErrorSources.payload().data();
        }

        private void setInitPath(ErrorSources.PayloadSources.PayloadDataSources initPath) {
            this.initPath = initPath;
        }

        public ToOneRelationshipObjectValidationState withResourceIdValidator(Consumer<String> resourceIdValidator) {
            this.resourceIdValidator = resourceIdValidator;
            return this;
        }

        public ToOneRelationshipObjectValidationState withResourceTypeValidator(Consumer<String> resourceTypeValidator) {
            this.resourceTypeValidator = resourceTypeValidator;
            return this;
        }

        public ToOneRelationshipObjectValidationState withResourceIdentifierMetaValidator(Consumer<Object> metaValidator) {
            this.metaValidator = metaValidator;
            return this;
        }

        public void validate(ToOneRelationshipObject toOneRelationshipObject) {
            if (toOneRelationshipObject.getData() != null) {
                validateResourceIdentifier(
                        toOneRelationshipObject.getData(),
                        initPath,
                        resourceIdValidator,
                        resourceTypeValidator,
                        metaValidator
                );
            }
        }

    }

    public static class ToManyRelationshipObjectValidationState {

        private ErrorSources.PayloadSources.PayloadDataSources initPath;

        private Consumer<String> resourceIdValidator;
        private Consumer<String> resourceTypeValidator;
        private Consumer<Object> metaValidator;

        private ToManyRelationshipObjectValidationState() {
            this.initPath = ErrorSources.payload().data();
        }

        private void setInitPath(ErrorSources.PayloadSources.PayloadDataSources initPath) {
            this.initPath = initPath;
        }

        public ToManyRelationshipObjectValidationState withResourceIdValidator(Consumer<String> resourceIdValidator) {
            this.resourceIdValidator = resourceIdValidator;
            return this;
        }

        public ToManyRelationshipObjectValidationState withResourceTypeValidator(Consumer<String> resourceTypeValidator) {
            this.resourceTypeValidator = resourceTypeValidator;
            return this;
        }

        public ToManyRelationshipObjectValidationState withResourceIdentifierMetaValidator(Consumer<Object> metaValidator) {
            this.metaValidator = metaValidator;
            return this;
        }

        public void validate(ToManyRelationshipObject toManyRelationshipObject) {
            List<ResourceIdentifierObject> emptyIfNull = emptyIfNull(toManyRelationshipObject.getData());
            for (int i = 0; i < emptyIfNull.size(); i++) {
                ResourceIdentifierObject ri = emptyIfNull.get(i);
                if (ri != null) {
                    validateResourceIdentifier(
                            ri,
                            initPath.index(i),
                            resourceIdValidator,
                            resourceTypeValidator,
                            metaValidator
                    );
                }
            }
        }
    }

}
