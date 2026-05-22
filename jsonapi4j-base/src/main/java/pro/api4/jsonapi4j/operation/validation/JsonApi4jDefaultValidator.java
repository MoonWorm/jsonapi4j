package pro.api4.jsonapi4j.operation.validation;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.exception.JsonApiRequestValidationException;
import pro.api4.jsonapi4j.model.document.data.RelationshipObject;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipObject;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipObject;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.operation.validation.JsonApi4jDefaultValidator.SingleResourceDocValidationBuilder.ToManyRelationshipObjectValidationBuilder;
import pro.api4.jsonapi4j.operation.validation.JsonApi4jDefaultValidator.SingleResourceDocValidationBuilder.ToOneRelationshipObjectValidationBuilder;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.request.SortAwareRequest;

import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

// TODO: accept JsonApiRequest as a parameter everywhere
// TODO: introduce the only uber method - validate requests - as a builder
public class JsonApi4jDefaultValidator {

    private static void wrapExceptions(Runnable runnable, ErrorSources.Source source) {
        try {
            runnable.run();
        } catch (JsonApiRequestValidationException e) {
            throw JsonApiRequestValidationException.withSource(e, source);
        }
    }

    private static void validateResourceIdentifier(ResourceIdentifierObject resourceIdentifier,
                                                   ErrorSources.JsonPointerBuilder.DataJsonPointerBuilder initPath,
                                                   Consumer<String> resourceIdValidator,
                                                   Consumer<String> resourceTypeValidator,
                                                   Consumer<Object> metaValidator) {
        if (resourceIdValidator != null && resourceIdentifier.getId() != null) {
            wrapExceptions(() -> resourceIdValidator.accept(resourceIdentifier.getId()), initPath.id());
        }
        if (resourceTypeValidator != null) {
            wrapExceptions(() -> resourceTypeValidator.accept(resourceIdentifier.getType()), initPath.type());
        }
        if (metaValidator != null) {
            wrapExceptions(() -> metaValidator.accept(resourceIdentifier.getMeta()), initPath.meta());
        }
    }

    public static void validateNonNull(Object object, ErrorSources.Source source) {
        if (object == null) {
            throw new JsonApiRequestValidationException(
                    DefaultErrorCodes.VALUE_IS_ABSENT,
                    "value can't be null",
                    source
            );
        }
    }

    public static void validateNonNull(Object object) {
        validateNonNull(object, null);
    }

    public static void validateEqualTo(Object actual, Object expected, ErrorSources.Source source) {
        if (!actual.equals(expected)) {
            throw new JsonApiRequestValidationException(
                    DefaultErrorCodes.VALUE_IS_NOT_EQUAL_TO,
                    MessageFormat.format("value should match {0}", expected),
                    source
            );
        }
    }

    public static void validateNonBlank(String value, ErrorSources.Source source) {
        if (StringUtils.isBlank(value)) {
            throw new JsonApiRequestValidationException(
                    DefaultErrorCodes.VALUE_EMPTY,
                    "value can't be blank",
                    source
            );
        }
    }

    public static void validateIsNull(Object value, ErrorSources.Source source) {
        if (value != null) {
            throw new JsonApiRequestValidationException(
                    DefaultErrorCodes.VALUE_IS_NOT_ABSENT,
                    "value must be null",
                    source
            );
        }
    }

    public static void validateValueAnyOf(String value,
                                          Set<String> allowedValues,
                                          ErrorSources.Source source) {
        for (String allowedValue : allowedValues) {
            if (allowedValue.equalsIgnoreCase(value)) {
                return;
            }
        }
        throw new JsonApiRequestValidationException(
                DefaultErrorCodes.INVALID_ENUM_VALUE,
                MessageFormat.format("''{0}'' value is not allowed, available values: [{1}]", value, String.join(", ", allowedValues)),
                source
        );
    }

    public static void validateValueAnyOf(String value,
                                          Set<String> allowedValues) {
        validateValueAnyOf(value, allowedValues, null);
    }

    public PathValidationState validatePath() {
        return new PathValidationState();
    }

    public ParametersValidationState validateParameters() {
        return new ParametersValidationState();
    }

    public <ATTRIBUTES> SingleResourceDocValidationBuilder<ATTRIBUTES> validateSingleResourceDoc(SingleResourceDoc<? extends ResourceObject<ATTRIBUTES, LinkedHashMap<String, RelationshipObject>>> singleResourceDoc) {
        return new SingleResourceDocValidationBuilder<>(singleResourceDoc);
    }

    public ToOneRelationshipObjectValidationBuilder validateToOneRelationshipObject() {
        return new ToOneRelationshipObjectValidationBuilder();
    }

    public ToManyRelationshipObjectValidationBuilder validateToManyRelationshipsObject() {
        return new ToManyRelationshipObjectValidationBuilder();
    }

    public static class HeadersValidationState {

        private final Map<String, Consumer<String>> headerValidators = new LinkedHashMap<>();

        public HeadersValidationState withHeaderValidator(String headerName, Consumer<String> headerValidator) {
            this.headerValidators.put(headerName, headerValidator);
            return this;
        }

        public void validate(JsonApiRequest request) {
            MapUtils.emptyIfNull(headerValidators).forEach((headerName, headerValidator) -> {
                wrapExceptions(
                        () -> headerValidator.accept(request.getHeader(headerName)),
                        ErrorSources.header(headerName)
                );
            });
        }

    }

    public static class ParametersValidationState {
        private final Map<String, Consumer<List<String>>> filterValidators = new LinkedHashMap<>();
        private Consumer<List<String>> includeValidator;
        private Consumer<Map<String, SortAwareRequest.SortOrder>> sortValidator;
        private Consumer<String> cursorValidator;
        private Consumer<Long> limitValidator;
        private Consumer<Long> offsetValidator;
        private final Map<String, Consumer<List<String>>> fieldSetsValidators = new LinkedHashMap<>();
        private final Map<String, Consumer<List<String>>> customQueryParamsValidators = new LinkedHashMap<>();

        public ParametersValidationState withFilterValidator(String filterName, Consumer<List<String>> filterValidator) {
            this.filterValidators.put(filterName, filterValidator);
            return this;
        }

        public ParametersValidationState withIncludeValidator(Consumer<List<String>> includeValidator) {
            this.includeValidator = includeValidator;
            return this;
        }

        public ParametersValidationState withSortValidator(Consumer<Map<String, SortAwareRequest.SortOrder>> sortValidator) {
            this.sortValidator = sortValidator;
            return this;
        }

        public ParametersValidationState withCursorValidator(Consumer<String> cursorValidator) {
            this.cursorValidator = cursorValidator;
            return this;
        }

        public ParametersValidationState withLimitValidator(Consumer<Long> limitValidator) {
            this.limitValidator = limitValidator;
            return this;
        }

        public ParametersValidationState withOffsetValidator(Consumer<Long> offsetValidator) {
            this.offsetValidator = offsetValidator;
            return this;
        }

        public ParametersValidationState withFieldSetsValidator(String resourceType, Consumer<List<String>> fieldSetsValidator) {
            this.fieldSetsValidators.put(resourceType, fieldSetsValidator);
            return this;
        }

        public ParametersValidationState withCustomQueryParamValidator(String paramName, Consumer<List<String>> customQueryParamValidator) {
            this.customQueryParamsValidators.put(paramName, customQueryParamValidator);
            return this;
        }

        public void validate(JsonApiRequest request) {
            MapUtils.emptyIfNull(filterValidators).forEach((filterName, filterValidator) -> {
                wrapExceptions(
                        () -> filterValidator.accept(request.getFilters().get(filterName)),
                        ErrorSources.parameter().filter(filterName)
                );
            });
            if (includeValidator != null) {
                wrapExceptions(
                        () -> includeValidator.accept(request.getOriginalIncludes()),
                        ErrorSources.parameter().include()
                );
            }
            if (sortValidator != null) {
                wrapExceptions(
                        () -> sortValidator.accept(request.getSortBy()),
                        ErrorSources.parameter().sort()
                );
            }
            if (cursorValidator != null) {
                wrapExceptions(
                        () -> cursorValidator.accept(request.getCursor()),
                        ErrorSources.parameter().cursor()
                );
            }
            if (limitValidator != null) {
                wrapExceptions(
                        () -> limitValidator.accept(request.getLimit()),
                        ErrorSources.parameter().limit()
                );
            }
            if (offsetValidator != null) {
                wrapExceptions(
                        () -> offsetValidator.accept(request.getOffset()),
                        ErrorSources.parameter().offset()
                );
            }
            MapUtils.emptyIfNull(fieldSetsValidators).forEach((resourceType, fieldSetsValidator) -> {
                wrapExceptions(
                        () -> fieldSetsValidator.accept(request.getFieldSets().get(resourceType)),
                        ErrorSources.parameter().fieldSets(resourceType)
                );
            });
            MapUtils.emptyIfNull(customQueryParamsValidators).forEach((paramName, customQueryParamValidator) -> {
                wrapExceptions(
                        () -> customQueryParamValidator.accept(request.getCustomQueryParams().get(paramName)),
                        ErrorSources.parameter().custom(paramName)
                );

            });
        }
    }

    public static class PathValidationState {

        private Consumer<ResourceType> resourceTypeValidator;
        private Consumer<String> resourceIdValidator;
        private Consumer<RelationshipName> relationshipNameValidator;

        public PathValidationState withResourceTypeValidator(Consumer<ResourceType> resourceTypeValidator) {
            this.resourceTypeValidator = resourceTypeValidator;
            return this;
        }

        public PathValidationState withResourceIdValidator(Consumer<String> resourceIdValidator) {
            this.resourceIdValidator = resourceIdValidator;
            return this;
        }

        public PathValidationState withRelationshipNameValidator(Consumer<RelationshipName> relationshipNameValidator) {
            this.relationshipNameValidator = relationshipNameValidator;
            return this;
        }

        public void validate(JsonApiRequest request) {
            if (resourceTypeValidator != null) {
                wrapExceptions(
                        () -> resourceTypeValidator.accept(request.getTargetResourceType()),
                        ErrorSources.path().resourceType()
                );
            }
            if (resourceIdValidator != null) {
                wrapExceptions(
                        () -> resourceIdValidator.accept(request.getResourceId()),
                        ErrorSources.path().resourceId()
                );
            }
            if (relationshipNameValidator != null) {
                wrapExceptions(
                        () -> relationshipNameValidator.accept(request.getTargetRelationshipName()),
                        ErrorSources.path().relationshipName()
                );
            }
        }
    }

    public static class SingleResourceDocValidationBuilder<ATTRIBUTES> {

        private final SingleResourceDoc<? extends ResourceObject<ATTRIBUTES, LinkedHashMap<String, RelationshipObject>>> singleResourceDoc;
        private final Map<String, ToOneRelationshipObjectValidationBuilder> toOneRelationshipValidators = new LinkedHashMap<>();
        private final Map<String, ToManyRelationshipObjectValidationBuilder> toManyRelationshipValidators = new LinkedHashMap<>();
        private Consumer<String> resourceIdValidator;
        private Consumer<String> resourceTypeValidator;
        private Consumer<ATTRIBUTES> attributesValidator;

        private SingleResourceDocValidationBuilder(SingleResourceDoc<? extends ResourceObject<ATTRIBUTES, LinkedHashMap<String, RelationshipObject>>> singleResourceDoc) {
            this.singleResourceDoc = singleResourceDoc;
        }

        public SingleResourceDocValidationBuilder<ATTRIBUTES> withResourceIdValidator(Consumer<String> resourceIdValidator) {
            this.resourceIdValidator = resourceIdValidator;
            return this;
        }

        public SingleResourceDocValidationBuilder<ATTRIBUTES> withResourceTypeValidator(Consumer<String> resourceTypeValidator) {
            this.resourceTypeValidator = resourceTypeValidator;
            return this;
        }

        public SingleResourceDocValidationBuilder<ATTRIBUTES> withAttributesValidator(Consumer<ATTRIBUTES> attributesValidator) {
            this.attributesValidator = attributesValidator;
            return this;
        }

        public SingleResourceDocValidationBuilder<ATTRIBUTES> withToOneRelationshipValidator(String relationshipName, ToOneRelationshipObjectValidationBuilder relationshipValidator) {
            this.toOneRelationshipValidators.put(relationshipName, relationshipValidator);
            return this;
        }

        public SingleResourceDocValidationBuilder<ATTRIBUTES> withToManyRelationshipValidator(String relationshipName, ToManyRelationshipObjectValidationBuilder relationshipValidator) {
            this.toManyRelationshipValidators.put(relationshipName, relationshipValidator);
            return this;
        }

        public void validate() {
            var data = singleResourceDoc.getData();
            if (resourceIdValidator != null && data.getId() != null) {
                wrapExceptions(() -> resourceIdValidator.accept(data.getId()), ErrorSources.pointer().data().id());
            }
            if (resourceTypeValidator != null) {
                wrapExceptions(() -> resourceTypeValidator.accept(data.getType()), ErrorSources.pointer().data().type());
            }
            if (attributesValidator != null) {
                attributesValidator.accept(data.getAttributes());
            }
            if (data.getRelationships() != null) {
                MapUtils.emptyIfNull(toOneRelationshipValidators).forEach((relationshipName, relationshipValidator) -> {
                    ToOneRelationshipObject relationshipObject = (ToOneRelationshipObject) singleResourceDoc.getData().getRelationships().get(relationshipName);
                    if (relationshipObject != null && relationshipObject.getData() != null) {
                        relationshipValidator.setInitPath(ErrorSources.pointer().data().relationship(relationshipName));
                        relationshipValidator.validate(relationshipObject);
                    }
                });
                MapUtils.emptyIfNull(toManyRelationshipValidators).forEach((relationshipName, relationshipValidator) -> {
                    ToManyRelationshipObject relationshipObject = (ToManyRelationshipObject) singleResourceDoc.getData().getRelationships().get(relationshipName);
                    if (relationshipObject != null) {
                        relationshipValidator.setInitPath(ErrorSources.pointer().data().relationship(relationshipName));
                        relationshipValidator.validate(relationshipObject);
                    }
                });
            }
        }

        public static class ToOneRelationshipObjectValidationBuilder {

            private ErrorSources.JsonPointerBuilder.DataJsonPointerBuilder initPath;

            private Consumer<String> resourceIdValidator;
            private Consumer<String> resourceTypeValidator;
            private Consumer<Object> metaValidator;

            private ToOneRelationshipObjectValidationBuilder() {
                this.initPath = ErrorSources.pointer().data();
            }

            private void setInitPath(ErrorSources.JsonPointerBuilder.DataJsonPointerBuilder initPath) {
                this.initPath = initPath;
            }

            public ToOneRelationshipObjectValidationBuilder withResourceIdValidator(Consumer<String> resourceIdValidator) {
                this.resourceIdValidator = resourceIdValidator;
                return this;
            }

            public ToOneRelationshipObjectValidationBuilder withResourceTypeValidator(Consumer<String> resourceTypeValidator) {
                this.resourceTypeValidator = resourceTypeValidator;
                return this;
            }

            public ToOneRelationshipObjectValidationBuilder withResourceIdentifierMetaValidator(Consumer<Object> metaValidator) {
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

        public static class ToManyRelationshipObjectValidationBuilder {

            private ErrorSources.JsonPointerBuilder.DataJsonPointerBuilder initPath;

            private Consumer<String> resourceIdValidator;
            private Consumer<String> resourceTypeValidator;
            private Consumer<Object> metaValidator;

            private ToManyRelationshipObjectValidationBuilder() {
                this.initPath = ErrorSources.pointer().data();
            }

            private void setInitPath(ErrorSources.JsonPointerBuilder.DataJsonPointerBuilder initPath) {
                this.initPath = initPath;
            }

            public ToManyRelationshipObjectValidationBuilder withResourceIdValidator(Consumer<String> resourceIdValidator) {
                this.resourceIdValidator = resourceIdValidator;
                return this;
            }

            public ToManyRelationshipObjectValidationBuilder withResourceTypeValidator(Consumer<String> resourceTypeValidator) {
                this.resourceTypeValidator = resourceTypeValidator;
                return this;
            }

            public ToManyRelationshipObjectValidationBuilder withResourceIdentifierMetaValidator(Consumer<Object> metaValidator) {
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
}