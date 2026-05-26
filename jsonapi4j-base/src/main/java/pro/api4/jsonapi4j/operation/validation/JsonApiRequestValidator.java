package pro.api4.jsonapi4j.operation.validation;

import org.apache.commons.collections4.MapUtils;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.model.document.data.RelationshipObject;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipObject;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipObject;
import pro.api4.jsonapi4j.operation.validation.JsonApiRequestValidator.SingleResourceDocValidationBuilder.ToManyRelationshipObjectValidationBuilder;
import pro.api4.jsonapi4j.operation.validation.JsonApiRequestValidator.SingleResourceDocValidationBuilder.ToOneRelationshipObjectValidationBuilder;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.request.SortAwareRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

public final class JsonApiRequestValidator {

    private JsonApiRequestValidator() {

    }

    public static RequestValidationBuilder forRequest(JsonApiRequest request) {
        return new RequestValidationBuilder(request);
    }

    private static void validateResourceIdentifier(ValidationErrorCollector collector,
                                                   ResourceIdentifierObject resourceIdentifier,
                                                   ErrorSources.JsonPointerBuilder.DataJsonPointerBuilder initPath,
                                                   Consumer<String> resourceIdValidator,
                                                   Consumer<String> resourceTypeValidator,
                                                   Consumer<Object> metaValidator) {
        if (resourceIdValidator != null && resourceIdentifier.getId() != null) {
            collector.collect(() -> resourceIdValidator.accept(resourceIdentifier.getId()), initPath.id());
        }
        if (resourceTypeValidator != null) {
            collector.collect(() -> resourceTypeValidator.accept(resourceIdentifier.getType()), initPath.type());
        }
        if (metaValidator != null) {
            collector.collect(() -> metaValidator.accept(resourceIdentifier.getMeta()), initPath.meta());
        }
    }

    public static class RequestValidationBuilder {

        private final JsonApiRequest request;
        private PathValidationBuilder pathValidationState;
        private ParametersValidationBuilder parametersValidationState;
        private HeadersValidationBuilder headersValidationState;
        private Runnable bodyValidation;

        private RequestValidationBuilder(JsonApiRequest request) {
            this.request = request;
        }

        public RequestValidationBuilder path(Consumer<PathValidationBuilder> configurator) {
            this.pathValidationState = new PathValidationBuilder();
            configurator.accept(this.pathValidationState);
            return this;
        }

        public RequestValidationBuilder parameters(Consumer<ParametersValidationBuilder> configurator) {
            this.parametersValidationState = new ParametersValidationBuilder();
            configurator.accept(this.parametersValidationState);
            return this;
        }

        public RequestValidationBuilder headers(Consumer<HeadersValidationBuilder> configurator) {
            this.headersValidationState = new HeadersValidationBuilder();
            configurator.accept(this.headersValidationState);
            return this;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public RequestValidationBuilder singleResourceBody(
                Consumer<SingleResourceDocValidationBuilder<LinkedHashMap>> configurator) {
            configureSingleResourceBody(request.getSingleResourceDocPayload(), (Consumer) configurator);
            return this;
        }

        public <ATTRIBUTES> RequestValidationBuilder singleResourceBody(
                Class<ATTRIBUTES> attType,
                Consumer<SingleResourceDocValidationBuilder<ATTRIBUTES>> configurator) {
            configureSingleResourceBody(request.getSingleResourceDocPayload(attType), configurator);
            return this;
        }

        public RequestValidationBuilder toOneRelationshipBody(
                Consumer<ToOneRelationshipObjectValidationBuilder> configurator) {
            ToOneRelationshipObjectValidationBuilder builder = new ToOneRelationshipObjectValidationBuilder();
            configurator.accept(builder);
            this.bodyValidation = () -> builder.validate(request.getToOneRelationshipDocPayload());
            return this;
        }

        public RequestValidationBuilder toManyRelationshipBody(
                Consumer<ToManyRelationshipObjectValidationBuilder> configurator) {
            ToManyRelationshipObjectValidationBuilder builder = new ToManyRelationshipObjectValidationBuilder();
            configurator.accept(builder);
            this.bodyValidation = () -> builder.validate(request.getToManyRelationshipDocPayload());
            return this;
        }

        private <ATTRIBUTES> void configureSingleResourceBody(
                SingleResourceDoc<? extends ResourceObject<ATTRIBUTES, LinkedHashMap<String, RelationshipObject>>> singleResourceDoc,
                Consumer<SingleResourceDocValidationBuilder<ATTRIBUTES>> configurator) {
            SingleResourceDocValidationBuilder<ATTRIBUTES> builder = new SingleResourceDocValidationBuilder<>(singleResourceDoc);
            configurator.accept(builder);
            this.bodyValidation = builder::validate;
        }

        public void validate() {
            ValidationErrorCollector collector = new ValidationErrorCollector();
            if (pathValidationState != null) {
                collector.collect(() -> pathValidationState.validate(request));
            }
            if (parametersValidationState != null) {
                collector.collect(() -> parametersValidationState.validate(request));
            }
            if (headersValidationState != null) {
                collector.collect(() -> headersValidationState.validate(request));
            }
            if (bodyValidation != null) {
                collector.collect(bodyValidation);
            }
            collector.throwIfErrors();
        }

    }

    public static class HeadersValidationBuilder {

        private final Map<String, Consumer<String>> headerValidators = new LinkedHashMap<>();

        public HeadersValidationBuilder withHeaderValidator(String headerName, Consumer<String> headerValidator) {
            this.headerValidators.put(headerName, headerValidator);
            return this;
        }

        private void validate(JsonApiRequest request) {
            ValidationErrorCollector collector = new ValidationErrorCollector();
            MapUtils.emptyIfNull(headerValidators).forEach((headerName, headerValidator) -> {
                collector.collect(
                        () -> headerValidator.accept(request.getHeader(headerName)),
                        ErrorSources.header(headerName)
                );
            });
            collector.throwIfErrors();
        }

    }

    public static class ParametersValidationBuilder {
        private final Map<String, Consumer<List<String>>> filterValidators = new LinkedHashMap<>();
        private Consumer<Map<String, List<String>>> filtersValidator;
        private Consumer<List<String>> includeValidator;
        private Consumer<Map<String, SortAwareRequest.SortOrder>> sortValidator;
        private Consumer<String> cursorValidator;
        private Consumer<Long> limitValidator;
        private Consumer<Long> offsetValidator;
        private final Map<String, Consumer<List<String>>> fieldSetsValidators = new LinkedHashMap<>();
        private final Map<String, Consumer<List<String>>> customQueryParamsValidators = new LinkedHashMap<>();

        public ParametersValidationBuilder withFilterValidator(String filterName, Consumer<List<String>> filterValidator) {
            this.filterValidators.put(filterName, filterValidator);
            return this;
        }

        public ParametersValidationBuilder withFiltersValidator(Consumer<Map<String, List<String>>> filtersValidator) {
            this.filtersValidator = filtersValidator;
            return this;
        }

        public ParametersValidationBuilder withIncludeValidator(Consumer<List<String>> includeValidator) {
            this.includeValidator = includeValidator;
            return this;
        }

        public ParametersValidationBuilder withSortValidator(Consumer<Map<String, SortAwareRequest.SortOrder>> sortValidator) {
            this.sortValidator = sortValidator;
            return this;
        }

        public ParametersValidationBuilder withCursorValidator(Consumer<String> cursorValidator) {
            this.cursorValidator = cursorValidator;
            return this;
        }

        public ParametersValidationBuilder withLimitValidator(Consumer<Long> limitValidator) {
            this.limitValidator = limitValidator;
            return this;
        }

        public ParametersValidationBuilder withOffsetValidator(Consumer<Long> offsetValidator) {
            this.offsetValidator = offsetValidator;
            return this;
        }

        public ParametersValidationBuilder withFieldSetsValidator(String resourceType, Consumer<List<String>> fieldSetsValidator) {
            this.fieldSetsValidators.put(resourceType, fieldSetsValidator);
            return this;
        }

        public ParametersValidationBuilder withCustomQueryParamValidator(String paramName, Consumer<List<String>> customQueryParamValidator) {
            this.customQueryParamsValidators.put(paramName, customQueryParamValidator);
            return this;
        }

        private void validate(JsonApiRequest request) {
            ValidationErrorCollector collector = new ValidationErrorCollector();
            if (filtersValidator != null) {
                collector.collect(() -> filtersValidator.accept(request.getFilters()));
            }
            MapUtils.emptyIfNull(filterValidators).forEach((filterName, filterValidator) -> {
                collector.collect(
                        () -> filterValidator.accept(request.getFilters().get(filterName)),
                        ErrorSources.parameter().filter(filterName)
                );
            });
            if (includeValidator != null) {
                collector.collect(
                        () -> includeValidator.accept(request.getOriginalIncludes()),
                        ErrorSources.parameter().include()
                );
            }
            if (sortValidator != null) {
                collector.collect(
                        () -> sortValidator.accept(request.getSortBy()),
                        ErrorSources.parameter().sort()
                );
            }
            if (cursorValidator != null) {
                collector.collect(
                        () -> cursorValidator.accept(request.getCursor()),
                        ErrorSources.parameter().cursor()
                );
            }
            if (limitValidator != null) {
                collector.collect(
                        () -> limitValidator.accept(request.getLimit()),
                        ErrorSources.parameter().limit()
                );
            }
            if (offsetValidator != null) {
                collector.collect(
                        () -> offsetValidator.accept(request.getOffset()),
                        ErrorSources.parameter().offset()
                );
            }
            MapUtils.emptyIfNull(fieldSetsValidators).forEach((resourceType, fieldSetsValidator) -> {
                collector.collect(
                        () -> fieldSetsValidator.accept(request.getFieldSets().get(resourceType)),
                        ErrorSources.parameter().fieldSets(resourceType)
                );
            });
            MapUtils.emptyIfNull(customQueryParamsValidators).forEach((paramName, customQueryParamValidator) -> {
                collector.collect(
                        () -> customQueryParamValidator.accept(request.getCustomQueryParams().get(paramName)),
                        ErrorSources.parameter().custom(paramName)
                );
            });
            collector.throwIfErrors();
        }
    }

    public static class PathValidationBuilder {

        private Consumer<ResourceType> resourceTypeValidator;
        private Consumer<String> resourceIdValidator;
        private Consumer<RelationshipName> relationshipNameValidator;

        public PathValidationBuilder withResourceTypeValidator(Consumer<ResourceType> resourceTypeValidator) {
            this.resourceTypeValidator = resourceTypeValidator;
            return this;
        }

        public PathValidationBuilder withResourceIdValidator(Consumer<String> resourceIdValidator) {
            this.resourceIdValidator = resourceIdValidator;
            return this;
        }

        public PathValidationBuilder withRelationshipNameValidator(Consumer<RelationshipName> relationshipNameValidator) {
            this.relationshipNameValidator = relationshipNameValidator;
            return this;
        }

        private void validate(JsonApiRequest request) {
            ValidationErrorCollector collector = new ValidationErrorCollector();
            if (resourceTypeValidator != null) {
                collector.collect(
                        () -> resourceTypeValidator.accept(request.getTargetResourceType()),
                        ErrorSources.path().resourceType()
                );
            }
            if (resourceIdValidator != null) {
                collector.collect(
                        () -> resourceIdValidator.accept(request.getResourceId()),
                        ErrorSources.path().resourceId()
                );
            }
            if (relationshipNameValidator != null) {
                collector.collect(
                        () -> relationshipNameValidator.accept(request.getTargetRelationshipName()),
                        ErrorSources.path().relationshipName()
                );
            }
            collector.throwIfErrors();
        }
    }

    public static class SingleResourceDocValidationBuilder<ATTRIBUTES> {

        private final SingleResourceDoc<? extends ResourceObject<ATTRIBUTES, LinkedHashMap<String, RelationshipObject>>> singleResourceDoc;
        private final Map<String, ToOneRelationshipObjectValidationBuilder> toOneRelationshipValidators = new LinkedHashMap<>();
        private final Map<String, ToManyRelationshipObjectValidationBuilder> toManyRelationshipValidators = new LinkedHashMap<>();
        private Consumer<ResourceObject<ATTRIBUTES, LinkedHashMap<String, RelationshipObject>>> dataValidator;
        private Consumer<String> resourceIdValidator;
        private Consumer<String> resourceTypeValidator;
        private Consumer<ObjectValidationAssert<?, ATTRIBUTES>> attributesValidator;
        private Consumer<LinkedHashMap<String, RelationshipObject>> relationshipsValidator;

        private SingleResourceDocValidationBuilder(SingleResourceDoc<? extends ResourceObject<ATTRIBUTES, LinkedHashMap<String, RelationshipObject>>> singleResourceDoc) {
            this.singleResourceDoc = singleResourceDoc;
        }

        public SingleResourceDocValidationBuilder<ATTRIBUTES> withDataValidator(
                Consumer<ResourceObject<ATTRIBUTES, LinkedHashMap<String, RelationshipObject>>> dataValidator) {
            this.dataValidator = dataValidator;
            return this;
        }

        public SingleResourceDocValidationBuilder<ATTRIBUTES> withResourceIdValidator(Consumer<String> resourceIdValidator) {
            this.resourceIdValidator = resourceIdValidator;
            return this;
        }

        public SingleResourceDocValidationBuilder<ATTRIBUTES> withResourceTypeValidator(Consumer<String> resourceTypeValidator) {
            this.resourceTypeValidator = resourceTypeValidator;
            return this;
        }

        public SingleResourceDocValidationBuilder<ATTRIBUTES> withAttributesValidator(Consumer<ObjectValidationAssert<?, ATTRIBUTES>> attributesValidator) {
            this.attributesValidator = attributesValidator;
            return this;
        }

        public SingleResourceDocValidationBuilder<ATTRIBUTES> withRelationshipsValidator(Consumer<LinkedHashMap<String, RelationshipObject>> relationshipsValidator) {
            this.relationshipsValidator = relationshipsValidator;
            return this;
        }

        public SingleResourceDocValidationBuilder<ATTRIBUTES> withToOneRelationship(String relationshipName, Consumer<ToOneRelationshipObjectValidationBuilder> configurator) {
            ToOneRelationshipObjectValidationBuilder builder = new ToOneRelationshipObjectValidationBuilder();
            configurator.accept(builder);
            this.toOneRelationshipValidators.put(relationshipName, builder);
            return this;
        }

        public SingleResourceDocValidationBuilder<ATTRIBUTES> withToManyRelationship(String relationshipName, Consumer<ToManyRelationshipObjectValidationBuilder> configurator) {
            ToManyRelationshipObjectValidationBuilder builder = new ToManyRelationshipObjectValidationBuilder();
            configurator.accept(builder);
            this.toManyRelationshipValidators.put(relationshipName, builder);
            return this;
        }

        @SuppressWarnings("unchecked")
        public void validate() {
            ValidationErrorCollector collector = new ValidationErrorCollector();
            var data = singleResourceDoc.getData();
            if (dataValidator != null) {
                collector.collect(() -> dataValidator.accept(
                        (ResourceObject<ATTRIBUTES, LinkedHashMap<String, RelationshipObject>>) data));
            }
            if (data == null) {
                collector.throwIfErrors();
                return;
            }
            if (resourceIdValidator != null) {
                collector.collect(() -> resourceIdValidator.accept(data.getId()), ErrorSources.pointer().data().id());
            }
            if (resourceTypeValidator != null) {
                collector.collect(() -> resourceTypeValidator.accept(data.getType()), ErrorSources.pointer().data().type());
            }
            if (attributesValidator != null) {
                collector.collect(() -> {
                    var attAssert = new ObjectValidationAssert<>(data.getAttributes(), ErrorSources.pointer().data().attributes());
                    attributesValidator.accept(attAssert);
                }, ErrorSources.pointer().data().attributes());
            }
            if (relationshipsValidator != null && data.getRelationships() != null) {
                collector.collect(() -> relationshipsValidator.accept(data.getRelationships()));
            }
            if (data.getRelationships() != null) {
                MapUtils.emptyIfNull(toOneRelationshipValidators).forEach((relationshipName, relationshipValidator) -> {
                    ToOneRelationshipObject relationshipObject = (ToOneRelationshipObject) singleResourceDoc.getData().getRelationships().get(relationshipName);
                    if (relationshipObject != null && relationshipObject.getData() != null) {
                        relationshipValidator.setInitPath(ErrorSources.pointer().data().relationship(relationshipName));
                        collector.collect(() -> relationshipValidator.validate(relationshipObject));
                    }
                });
                MapUtils.emptyIfNull(toManyRelationshipValidators).forEach((relationshipName, relationshipValidator) -> {
                    ToManyRelationshipObject relationshipObject = (ToManyRelationshipObject) singleResourceDoc.getData().getRelationships().get(relationshipName);
                    if (relationshipObject != null) {
                        relationshipValidator.setInitPath(ErrorSources.pointer().data().relationship(relationshipName));
                        collector.collect(() -> relationshipValidator.validate(relationshipObject));
                    }
                });
            }
            collector.throwIfErrors();
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

            private void validate(ToOneRelationshipObject toOneRelationshipObject) {
                if (toOneRelationshipObject.getData() != null) {
                    ValidationErrorCollector collector = new ValidationErrorCollector();
                    validateResourceIdentifier(
                            collector,
                            toOneRelationshipObject.getData(),
                            initPath,
                            resourceIdValidator,
                            resourceTypeValidator,
                            metaValidator
                    );
                    collector.throwIfErrors();
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

            private void validate(ToManyRelationshipObject toManyRelationshipObject) {
                ValidationErrorCollector collector = new ValidationErrorCollector();
                List<ResourceIdentifierObject> emptyIfNull = emptyIfNull(toManyRelationshipObject.getData());
                for (int i = 0; i < emptyIfNull.size(); i++) {
                    ResourceIdentifierObject ri = emptyIfNull.get(i);
                    if (ri != null) {
                        validateResourceIdentifier(
                                collector,
                                ri,
                                initPath.index(i),
                                resourceIdValidator,
                                resourceTypeValidator,
                                metaValidator
                        );
                    }
                }
                collector.throwIfErrors();
            }
        }

    }
}
