package pro.api4.jsonapi4j.operation.validation;

import org.apache.commons.collections4.MapUtils;
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

    private static void collectResourceIdentifier(ValidationErrorCollector collector,
                                                   ResourceIdentifierObject resourceIdentifier,
                                                   ErrorSources.JsonPointerBuilder.DataJsonPointerBuilder initPath,
                                                   Consumer<StringValidationAssert> resourceIdValidator,
                                                   Consumer<StringValidationAssert> resourceTypeValidator,
                                                   Consumer<ObjectValidationAssert<?, Object>> metaValidator) {
        if (resourceIdValidator != null && resourceIdentifier.getId() != null) {
            collector.collect(() -> resourceIdValidator.accept(
                    new StringValidationAssert(resourceIdentifier.getId(), initPath.id())),
                    initPath.id());
        }
        if (resourceTypeValidator != null) {
            collector.collect(() -> resourceTypeValidator.accept(
                    new StringValidationAssert(resourceIdentifier.getType(), initPath.type())),
                    initPath.type());
        }
        if (metaValidator != null) {
            collector.collect(() -> metaValidator.accept(
                    new ObjectValidationAssert<>(resourceIdentifier.getMeta(), initPath.meta())),
                    initPath.meta());
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

        private final Map<String, Consumer<StringValidationAssert>> headerValidators = new LinkedHashMap<>();

        public HeadersValidationBuilder withHeaderValidator(String headerName, Consumer<StringValidationAssert> headerValidator) {
            this.headerValidators.put(headerName, headerValidator);
            return this;
        }

        private void validate(JsonApiRequest request) {
            ValidationErrorCollector collector = new ValidationErrorCollector();
            MapUtils.emptyIfNull(headerValidators).forEach((headerName, headerValidator) -> {
                collector.collect(
                        () -> headerValidator.accept(new StringValidationAssert(request.getHeader(headerName), ErrorSources.header(headerName))),
                        ErrorSources.header(headerName)
                );
            });
            collector.throwIfErrors();
        }

    }

    public static class ParametersValidationBuilder {
        private final Map<String, Consumer<CollectionValidationAssert<String>>> filterValidators = new LinkedHashMap<>();
        private Consumer<MapValidationAssert<String, List<String>>> filtersValidator;
        private Consumer<CollectionValidationAssert<String>> includeValidator;
        private Consumer<MapValidationAssert<String, SortAwareRequest.SortOrder>> sortValidator;
        private Consumer<StringValidationAssert> cursorValidator;
        private Consumer<NumberValidationAssert<Long>> limitValidator;
        private Consumer<NumberValidationAssert<Long>> offsetValidator;
        private final Map<String, Consumer<CollectionValidationAssert<String>>> fieldSetsValidators = new LinkedHashMap<>();
        private final Map<String, Consumer<CollectionValidationAssert<String>>> customQueryParamsValidators = new LinkedHashMap<>();

        public ParametersValidationBuilder withFilterValidator(String filterName, Consumer<CollectionValidationAssert<String>> filterValidator) {
            this.filterValidators.put(filterName, filterValidator);
            return this;
        }

        public ParametersValidationBuilder withFiltersValidator(Consumer<MapValidationAssert<String, List<String>>> filtersValidator) {
            this.filtersValidator = filtersValidator;
            return this;
        }

        public ParametersValidationBuilder withIncludeValidator(Consumer<CollectionValidationAssert<String>> includeValidator) {
            this.includeValidator = includeValidator;
            return this;
        }

        public ParametersValidationBuilder withSortValidator(Consumer<MapValidationAssert<String, SortAwareRequest.SortOrder>> sortValidator) {
            this.sortValidator = sortValidator;
            return this;
        }

        public ParametersValidationBuilder withCursorValidator(Consumer<StringValidationAssert> cursorValidator) {
            this.cursorValidator = cursorValidator;
            return this;
        }

        public ParametersValidationBuilder withLimitValidator(Consumer<NumberValidationAssert<Long>> limitValidator) {
            this.limitValidator = limitValidator;
            return this;
        }

        public ParametersValidationBuilder withOffsetValidator(Consumer<NumberValidationAssert<Long>> offsetValidator) {
            this.offsetValidator = offsetValidator;
            return this;
        }

        public ParametersValidationBuilder withFieldSetsValidator(String resourceType, Consumer<CollectionValidationAssert<String>> fieldSetsValidator) {
            this.fieldSetsValidators.put(resourceType, fieldSetsValidator);
            return this;
        }

        public ParametersValidationBuilder withCustomQueryParamValidator(String paramName, Consumer<CollectionValidationAssert<String>> customQueryParamValidator) {
            this.customQueryParamsValidators.put(paramName, customQueryParamValidator);
            return this;
        }

        private void validate(JsonApiRequest request) {
            ValidationErrorCollector collector = new ValidationErrorCollector();
            if (filtersValidator != null) {
                collector.collect(() -> filtersValidator.accept(
                        new MapValidationAssert<>(request.getFilters(), null)));
            }
            MapUtils.emptyIfNull(filterValidators).forEach((filterName, filterValidator) -> {
                collector.collect(
                        () -> filterValidator.accept(
                                new CollectionValidationAssert<>(request.getFilters().get(filterName), ErrorSources.parameter().filter(filterName))),
                        ErrorSources.parameter().filter(filterName)
                );
            });
            if (includeValidator != null) {
                collector.collect(
                        () -> includeValidator.accept(
                                new CollectionValidationAssert<>(request.getOriginalIncludes(), ErrorSources.parameter().include())),
                        ErrorSources.parameter().include()
                );
            }
            if (sortValidator != null) {
                collector.collect(
                        () -> sortValidator.accept(
                                new MapValidationAssert<>(request.getSortBy(), ErrorSources.parameter().sort())),
                        ErrorSources.parameter().sort()
                );
            }
            if (cursorValidator != null) {
                collector.collect(
                        () -> cursorValidator.accept(
                                new StringValidationAssert(request.getCursor(), ErrorSources.parameter().cursor())),
                        ErrorSources.parameter().cursor()
                );
            }
            if (limitValidator != null) {
                collector.collect(
                        () -> limitValidator.accept(
                                new NumberValidationAssert<>(request.getLimit(), ErrorSources.parameter().limit())),
                        ErrorSources.parameter().limit()
                );
            }
            if (offsetValidator != null) {
                collector.collect(
                        () -> offsetValidator.accept(
                                new NumberValidationAssert<>(request.getOffset(), ErrorSources.parameter().offset())),
                        ErrorSources.parameter().offset()
                );
            }
            MapUtils.emptyIfNull(fieldSetsValidators).forEach((resourceType, fieldSetsValidator) -> {
                collector.collect(
                        () -> fieldSetsValidator.accept(
                                new CollectionValidationAssert<>(request.getFieldSets().get(resourceType), ErrorSources.parameter().fieldSets(resourceType))),
                        ErrorSources.parameter().fieldSets(resourceType)
                );
            });
            MapUtils.emptyIfNull(customQueryParamsValidators).forEach((paramName, customQueryParamValidator) -> {
                collector.collect(
                        () -> customQueryParamValidator.accept(
                                new CollectionValidationAssert<>(request.getCustomQueryParams().get(paramName), ErrorSources.parameter().custom(paramName))),
                        ErrorSources.parameter().custom(paramName)
                );
            });
            collector.throwIfErrors();
        }
    }

    public static class PathValidationBuilder {

        private Consumer<ResourceTypeValidationAssert> resourceTypeValidator;
        private Consumer<StringValidationAssert> resourceIdValidator;
        private Consumer<RelationshipNameValidationAssert> relationshipNameValidator;

        public PathValidationBuilder withResourceTypeValidator(Consumer<ResourceTypeValidationAssert> resourceTypeValidator) {
            this.resourceTypeValidator = resourceTypeValidator;
            return this;
        }

        public PathValidationBuilder withResourceIdValidator(Consumer<StringValidationAssert> resourceIdValidator) {
            this.resourceIdValidator = resourceIdValidator;
            return this;
        }

        public PathValidationBuilder withRelationshipNameValidator(Consumer<RelationshipNameValidationAssert> relationshipNameValidator) {
            this.relationshipNameValidator = relationshipNameValidator;
            return this;
        }

        private void validate(JsonApiRequest request) {
            ValidationErrorCollector collector = new ValidationErrorCollector();
            if (resourceTypeValidator != null) {
                collector.collect(
                        () -> resourceTypeValidator.accept(
                                new ResourceTypeValidationAssert(request.getTargetResourceType(), ErrorSources.path().resourceType())),
                        ErrorSources.path().resourceType()
                );
            }
            if (resourceIdValidator != null) {
                collector.collect(
                        () -> resourceIdValidator.accept(
                                new StringValidationAssert(request.getResourceId(), ErrorSources.path().resourceId())),
                        ErrorSources.path().resourceId()
                );
            }
            if (relationshipNameValidator != null) {
                collector.collect(
                        () -> relationshipNameValidator.accept(
                                new RelationshipNameValidationAssert(request.getTargetRelationshipName(), ErrorSources.path().relationshipName())),
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
        private Consumer<ObjectValidationAssert<?, ResourceObject<ATTRIBUTES, LinkedHashMap<String, RelationshipObject>>>> dataValidator;
        private Consumer<StringValidationAssert> resourceIdValidator;
        private Consumer<StringValidationAssert> resourceTypeValidator;
        private Consumer<ObjectValidationAssert<?, ATTRIBUTES>> attributesValidator;
        private Consumer<MapValidationAssert<String, RelationshipObject>> relationshipsValidator;

        private SingleResourceDocValidationBuilder(SingleResourceDoc<? extends ResourceObject<ATTRIBUTES, LinkedHashMap<String, RelationshipObject>>> singleResourceDoc) {
            this.singleResourceDoc = singleResourceDoc;
        }

        public SingleResourceDocValidationBuilder<ATTRIBUTES> withDataValidator(
                Consumer<ObjectValidationAssert<?, ResourceObject<ATTRIBUTES, LinkedHashMap<String, RelationshipObject>>>> dataValidator) {
            this.dataValidator = dataValidator;
            return this;
        }

        public SingleResourceDocValidationBuilder<ATTRIBUTES> withResourceIdValidator(Consumer<StringValidationAssert> resourceIdValidator) {
            this.resourceIdValidator = resourceIdValidator;
            return this;
        }

        public SingleResourceDocValidationBuilder<ATTRIBUTES> withResourceTypeValidator(Consumer<StringValidationAssert> resourceTypeValidator) {
            this.resourceTypeValidator = resourceTypeValidator;
            return this;
        }

        public SingleResourceDocValidationBuilder<ATTRIBUTES> withAttributesValidator(Consumer<ObjectValidationAssert<?, ATTRIBUTES>> attributesValidator) {
            this.attributesValidator = attributesValidator;
            return this;
        }

        public SingleResourceDocValidationBuilder<ATTRIBUTES> withRelationshipsValidator(Consumer<MapValidationAssert<String, RelationshipObject>> relationshipsValidator) {
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
                        new ObjectValidationAssert<>((ResourceObject<ATTRIBUTES, LinkedHashMap<String, RelationshipObject>>) data,
                                ErrorSources.pointer().data().toPointer())),
                        ErrorSources.pointer().data().toPointer());
            }
            if (data == null) {
                collector.throwIfErrors();
                return;
            }
            if (resourceIdValidator != null) {
                collector.collect(() -> resourceIdValidator.accept(
                        new StringValidationAssert(data.getId(), ErrorSources.pointer().data().id())),
                        ErrorSources.pointer().data().id());
            }
            if (resourceTypeValidator != null) {
                collector.collect(() -> resourceTypeValidator.accept(
                        new StringValidationAssert(data.getType(), ErrorSources.pointer().data().type())),
                        ErrorSources.pointer().data().type());
            }
            if (attributesValidator != null) {
                collector.collect(() -> attributesValidator.accept(
                        new ObjectValidationAssert<>(data.getAttributes(), ErrorSources.pointer().data().attributes())),
                        ErrorSources.pointer().data().attributes());
            }
            if (relationshipsValidator != null && data.getRelationships() != null) {
                collector.collect(() -> relationshipsValidator.accept(
                        new MapValidationAssert<>(data.getRelationships(), null)));
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

            private Consumer<StringValidationAssert> resourceIdValidator;
            private Consumer<StringValidationAssert> resourceTypeValidator;
            private Consumer<ObjectValidationAssert<?, Object>> metaValidator;

            private ToOneRelationshipObjectValidationBuilder() {
                this.initPath = ErrorSources.pointer().data();
            }

            private void setInitPath(ErrorSources.JsonPointerBuilder.DataJsonPointerBuilder initPath) {
                this.initPath = initPath;
            }

            public ToOneRelationshipObjectValidationBuilder withResourceIdValidator(Consumer<StringValidationAssert> resourceIdValidator) {
                this.resourceIdValidator = resourceIdValidator;
                return this;
            }

            public ToOneRelationshipObjectValidationBuilder withResourceTypeValidator(Consumer<StringValidationAssert> resourceTypeValidator) {
                this.resourceTypeValidator = resourceTypeValidator;
                return this;
            }

            public ToOneRelationshipObjectValidationBuilder withResourceIdentifierMetaValidator(Consumer<ObjectValidationAssert<?, Object>> metaValidator) {
                this.metaValidator = metaValidator;
                return this;
            }

            private void validate(ToOneRelationshipObject toOneRelationshipObject) {
                if (toOneRelationshipObject.getData() != null) {
                    ValidationErrorCollector collector = new ValidationErrorCollector();
                    collectResourceIdentifier(
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

            private Consumer<StringValidationAssert> resourceIdValidator;
            private Consumer<StringValidationAssert> resourceTypeValidator;
            private Consumer<ObjectValidationAssert<?, Object>> metaValidator;

            private ToManyRelationshipObjectValidationBuilder() {
                this.initPath = ErrorSources.pointer().data();
            }

            private void setInitPath(ErrorSources.JsonPointerBuilder.DataJsonPointerBuilder initPath) {
                this.initPath = initPath;
            }

            public ToManyRelationshipObjectValidationBuilder withResourceIdValidator(Consumer<StringValidationAssert> resourceIdValidator) {
                this.resourceIdValidator = resourceIdValidator;
                return this;
            }

            public ToManyRelationshipObjectValidationBuilder withResourceTypeValidator(Consumer<StringValidationAssert> resourceTypeValidator) {
                this.resourceTypeValidator = resourceTypeValidator;
                return this;
            }

            public ToManyRelationshipObjectValidationBuilder withResourceIdentifierMetaValidator(Consumer<ObjectValidationAssert<?, Object>> metaValidator) {
                this.metaValidator = metaValidator;
                return this;
            }

            private void validate(ToManyRelationshipObject toManyRelationshipObject) {
                ValidationErrorCollector collector = new ValidationErrorCollector();
                List<ResourceIdentifierObject> emptyIfNull = emptyIfNull(toManyRelationshipObject.getData());
                for (int i = 0; i < emptyIfNull.size(); i++) {
                    ResourceIdentifierObject ri = emptyIfNull.get(i);
                    if (ri != null) {
                        collectResourceIdentifier(
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
