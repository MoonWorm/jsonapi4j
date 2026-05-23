package pro.api4.jsonapi4j.operation.validation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.exception.CompositeJsonApiRequestValidationException;
import pro.api4.jsonapi4j.exception.JsonApiRequestValidationException;
import pro.api4.jsonapi4j.model.document.data.RelationshipObject;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipObject;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipObject;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.request.JsonApiRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static pro.api4.jsonapi4j.operation.validation.JsonApiRequestValidator.forRequest;

@ExtendWith(MockitoExtension.class)
public class JsonApiRequestValidatorTests {

    @Mock
    private JsonApiRequest request;

    // --- No validators configured ---

    @Test
    void validate_noValidatorsConfigured_doesNotThrow() {
        assertThatCode(() -> forRequest(request).validate()).doesNotThrowAnyException();
    }

    // --- Path validation ---

    @Nested
    class PathValidation {

        @Test
        void validate_pathResourceId_valid_doesNotThrow() {
            when(request.getResourceId()).thenReturn("123");

            assertThatCode(() ->
                    forRequest(request)
                            .path(path -> path.withResourceIdValidator(id -> {}))
                            .validate()
            ).doesNotThrowAnyException();
        }

        @Test
        void validate_pathResourceId_invalid_throwsWithCorrectSource() {
            when(request.getResourceId()).thenReturn("bad-id");

            assertThatThrownBy(() ->
                    forRequest(request)
                            .path(path -> path.withResourceIdValidator(id -> {
                                throw new JsonApiRequestValidationException("invalid id");
                            }))
                            .validate()
            )
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> {
                        var ex = (JsonApiRequestValidationException) e;
                        assertThat(ex.getSource()).isInstanceOf(ErrorSources.Path.class);
                        assertThat(ex.getDetail()).isEqualTo("invalid id");
                    });
        }

        @Test
        void validate_pathMultipleValidatorsFail_throwsCompositeWithAllErrors() {
            when(request.getResourceId()).thenReturn("bad");
            when(request.getTargetResourceType()).thenReturn(new ResourceType("unknown"));

            assertThatThrownBy(() ->
                    forRequest(request)
                            .path(path -> path
                                    .withResourceTypeValidator(rt -> {
                                        throw new JsonApiRequestValidationException("bad type");
                                    })
                                    .withResourceIdValidator(id -> {
                                        throw new JsonApiRequestValidationException("bad id");
                                    }))
                            .validate()
            )
                    .isInstanceOf(CompositeJsonApiRequestValidationException.class)
                    .satisfies(e -> {
                        var errors = ((CompositeJsonApiRequestValidationException) e).getValidationErrors();
                        assertThat(errors).hasSize(2);
                        assertThat(errors.get(0).detail()).isEqualTo("bad type");
                        assertThat(errors.get(0).source()).isInstanceOf(ErrorSources.Path.class);
                        assertThat(errors.get(1).detail()).isEqualTo("bad id");
                        assertThat(errors.get(1).source()).isInstanceOf(ErrorSources.Path.class);
                    });
        }
    }

    // --- Parameters validation ---

    @Nested
    class ParametersValidation {

        @Test
        void validate_filterValid_doesNotThrow() {
            when(request.getFilters()).thenReturn(Map.of("status", List.of("active")));

            assertThatCode(() ->
                    forRequest(request)
                            .parameters(params -> params
                                    .withFilterValidator("status", values -> {}))
                            .validate()
            ).doesNotThrowAnyException();
        }

        @Test
        void validate_filterInvalid_throwsWithParameterSource() {
            when(request.getFilters()).thenReturn(Map.of("status", List.of("invalid")));

            assertThatThrownBy(() ->
                    forRequest(request)
                            .parameters(params -> params
                                    .withFilterValidator("status", values -> {
                                        throw new JsonApiRequestValidationException("invalid filter");
                                    }))
                            .validate()
            )
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> {
                        var ex = (JsonApiRequestValidationException) e;
                        assertThat(ex.getSource()).isInstanceOf(ErrorSources.Parameter.class);
                        assertThat(ex.getDetail()).isEqualTo("invalid filter");
                    });
        }

        @Test
        void validate_multipleParametersFail_throwsComposite() {
            when(request.getFilters()).thenReturn(Map.of("status", List.of("bad")));
            when(request.getCursor()).thenReturn("bad-cursor");

            assertThatThrownBy(() ->
                    forRequest(request)
                            .parameters(params -> params
                                    .withFilterValidator("status", v -> {
                                        throw new JsonApiRequestValidationException("bad status");
                                    })
                                    .withCursorValidator(c -> {
                                        throw new JsonApiRequestValidationException("bad cursor");
                                    }))
                            .validate()
            )
                    .isInstanceOf(CompositeJsonApiRequestValidationException.class)
                    .satisfies(e -> {
                        var errors = ((CompositeJsonApiRequestValidationException) e).getValidationErrors();
                        assertThat(errors).hasSize(2);
                        assertThat(errors.get(0).detail()).isEqualTo("bad status");
                        assertThat(errors.get(1).detail()).isEqualTo("bad cursor");
                    });
        }

        @Test
        void validate_filtersValidatorReceivesWholeMap() {
            var filters = Map.of("a", List.of("1"), "b", List.of("2"));
            when(request.getFilters()).thenReturn(filters);

            assertThatThrownBy(() ->
                    forRequest(request)
                            .parameters(params -> params
                                    .withFiltersValidator(f -> {
                                        if (f.size() > 1) {
                                            throw new JsonApiRequestValidationException("too many filters");
                                        }
                                    }))
                            .validate()
            )
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> {
                        var ex = (JsonApiRequestValidationException) e;
                        assertThat(ex.getDetail()).isEqualTo("too many filters");
                    });
        }
    }

    // --- Headers validation ---

    @Nested
    class HeadersValidation {

        @Test
        void validate_headerValid_doesNotThrow() {
            when(request.getHeader("X-Tenant")).thenReturn("tenant-1");

            assertThatCode(() ->
                    forRequest(request)
                            .headers(h -> h.withHeaderValidator("X-Tenant", v -> {}))
                            .validate()
            ).doesNotThrowAnyException();
        }

        @Test
        void validate_headerInvalid_throwsWithHeaderSource() {
            when(request.getHeader("X-Tenant")).thenReturn(null);

            assertThatThrownBy(() ->
                    forRequest(request)
                            .headers(h -> h.withHeaderValidator("X-Tenant", v -> {
                                throw new JsonApiRequestValidationException("missing header");
                            }))
                            .validate()
            )
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> {
                        var ex = (JsonApiRequestValidationException) e;
                        assertThat(ex.getSource()).isInstanceOf(ErrorSources.Header.class);
                        assertThat(((ErrorSources.Header) ex.getSource()).header()).isEqualTo("X-Tenant");
                    });
        }
    }

    // --- Body: SingleResourceDoc validation ---

    @Nested
    class SingleResourceDocBodyValidation {

        @Test
        void validate_validBody_doesNotThrow() {
            mockSingleResourceDoc("123", "users", null, null);

            assertThatCode(() ->
                    forRequest(request)
                            .singleResourceBody(body -> body
                                    .withResourceTypeValidator(type -> {}))
                            .validate()
            ).doesNotThrowAnyException();
        }

        @Test
        void validate_invalidType_throwsWithPointerSource() {
            mockSingleResourceDoc(null, "wrong", null, null);

            assertThatThrownBy(() ->
                    forRequest(request)
                            .singleResourceBody(body -> body
                                    .withResourceTypeValidator(type -> {
                                        throw new JsonApiRequestValidationException("wrong type");
                                    }))
                            .validate()
            )
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> {
                        var ex = (JsonApiRequestValidationException) e;
                        assertThat(ex.getSource()).isInstanceOf(ErrorSources.JsonPointer.class);
                        assertThat(((ErrorSources.JsonPointer) ex.getSource()).pointer()).isEqualTo("/data/type");
                    });
        }

        @Test
        void validate_typeAndAttributesFail_throwsComposite() {
            mockSingleResourceDoc(null, "wrong", new LinkedHashMap<>(), null);

            assertThatThrownBy(() ->
                    forRequest(request)
                            .singleResourceBody(body -> body
                                    .withResourceTypeValidator(type -> {
                                        throw new JsonApiRequestValidationException("wrong type");
                                    })
                                    .withAttributesValidator(att -> {
                                        throw new JsonApiRequestValidationException(
                                                DefaultErrorCodes.VALUE_IS_ABSENT,
                                                "attrs invalid",
                                                ErrorSources.pointer().data().attributes()
                                        );
                                    }))
                            .validate()
            )
                    .isInstanceOf(CompositeJsonApiRequestValidationException.class)
                    .satisfies(e -> {
                        var errors = ((CompositeJsonApiRequestValidationException) e).getValidationErrors();
                        assertThat(errors).hasSize(2);
                        assertThat(errors.get(0).detail()).isEqualTo("wrong type");
                        assertThat(((ErrorSources.JsonPointer) errors.get(0).source()).pointer()).isEqualTo("/data/type");
                        assertThat(errors.get(1).detail()).isEqualTo("attrs invalid");
                        assertThat(((ErrorSources.JsonPointer) errors.get(1).source()).pointer()).isEqualTo("/data/attributes");
                    });
        }

        @Test
        void validate_dataValidatorRejectsNullData_skipsRemainingValidators() {
            when(request.getSingleResourceDocPayload()).thenReturn(new SingleResourceDoc<>(null));

            assertThatThrownBy(() ->
                    forRequest(request)
                            .singleResourceBody(body -> body
                                    .withDataValidator(data -> {
                                        if (data == null) {
                                            throw new JsonApiRequestValidationException("data is null");
                                        }
                                    })
                                    .withResourceTypeValidator(type -> {
                                        throw new JsonApiRequestValidationException("should not reach");
                                    }))
                            .validate()
            )
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> {
                        var ex = (JsonApiRequestValidationException) e;
                        assertThat(ex.getDetail()).isEqualTo("data is null");
                    });
        }

        @Test
        void validate_toOneRelationship_invalidType_throwsWithCorrectPointer() {
            var relData = resourceIdentifier("FI", "wrong");
            var rel = new ToOneRelationshipObject(relData, null, null);

            LinkedHashMap<String, RelationshipObject> rels = new LinkedHashMap<>();
            rels.put("placeOfBirth", rel);

            mockSingleResourceDoc(null, "users", null, rels);

            assertThatThrownBy(() ->
                    forRequest(request)
                            .singleResourceBody(body -> body
                                    .withToOneRelationship("placeOfBirth", r -> r
                                            .withResourceTypeValidator(type -> {
                                                throw new JsonApiRequestValidationException("wrong rel type");
                                            })))
                            .validate()
            )
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> {
                        var ex = (JsonApiRequestValidationException) e;
                        assertThat(((ErrorSources.JsonPointer) ex.getSource()).pointer())
                                .isEqualTo("/data/relationships/placeOfBirth/data/type");
                    });
        }

        @Test
        void validate_toManyRelationship_multipleElementsFail_collectsAll() {
            var ri0 = resourceIdentifier("US", "wrong1");
            var ri1 = resourceIdentifier("NO", "wrong2");
            var rel = new ToManyRelationshipObject(List.of(ri0, ri1), null, null);

            LinkedHashMap<String, RelationshipObject> rels = new LinkedHashMap<>();
            rels.put("citizenships", rel);

            mockSingleResourceDoc(null, "users", null, rels);

            assertThatThrownBy(() ->
                    forRequest(request)
                            .singleResourceBody(body -> body
                                    .withToManyRelationship("citizenships", r -> r
                                            .withResourceTypeValidator(type -> {
                                                throw new JsonApiRequestValidationException("bad type");
                                            })))
                            .validate()
            )
                    .isInstanceOf(CompositeJsonApiRequestValidationException.class)
                    .satisfies(e -> {
                        var errors = ((CompositeJsonApiRequestValidationException) e).getValidationErrors();
                        assertThat(errors).hasSize(2);
                        assertThat(((ErrorSources.JsonPointer) errors.get(0).source()).pointer())
                                .isEqualTo("/data/relationships/citizenships/data/0/type");
                        assertThat(((ErrorSources.JsonPointer) errors.get(1).source()).pointer())
                                .isEqualTo("/data/relationships/citizenships/data/1/type");
                    });
        }

        @Test
        void validate_attributesAndRelationshipFail_collectsAcrossSections() {
            var relData = resourceIdentifier("FI", "wrong");
            var rel = new ToOneRelationshipObject(relData, null, null);

            LinkedHashMap<String, RelationshipObject> rels = new LinkedHashMap<>();
            rels.put("placeOfBirth", rel);

            mockSingleResourceDoc(null, "users", new LinkedHashMap<>(), rels);

            assertThatThrownBy(() ->
                    forRequest(request)
                            .singleResourceBody(body -> body
                                    .withAttributesValidator(att -> {
                                        throw new JsonApiRequestValidationException("bad attrs");
                                    })
                                    .withToOneRelationship("placeOfBirth", r -> r
                                            .withResourceTypeValidator(type -> {
                                                throw new JsonApiRequestValidationException("bad rel type");
                                            })))
                            .validate()
            )
                    .isInstanceOf(CompositeJsonApiRequestValidationException.class)
                    .satisfies(e -> {
                        var errors = ((CompositeJsonApiRequestValidationException) e).getValidationErrors();
                        assertThat(errors).hasSize(2);
                        assertThat(errors.get(0).detail()).isEqualTo("bad attrs");
                        assertThat(errors.get(1).detail()).isEqualTo("bad rel type");
                    });
        }
    }

    // --- ToOneRelationshipBody ---

    @Nested
    class ToOneRelationshipBodyValidation {

        @Test
        void validate_validToOneBody_doesNotThrow() {
            var relData = resourceIdentifier("FI", "countries");
            var doc = new ToOneRelationshipDoc(relData, null, null);
            when(request.getToOneRelationshipDocPayload()).thenReturn(doc);

            assertThatCode(() ->
                    forRequest(request)
                            .toOneRelationshipBody(body -> body
                                    .withResourceTypeValidator(type -> {}))
                            .validate()
            ).doesNotThrowAnyException();
        }

        @Test
        void validate_nullData_doesNotThrow() {
            var doc = new ToOneRelationshipDoc(null, null, null);
            when(request.getToOneRelationshipDocPayload()).thenReturn(doc);

            assertThatCode(() ->
                    forRequest(request)
                            .toOneRelationshipBody(body -> body
                                    .withResourceTypeValidator(type -> {
                                        throw new JsonApiRequestValidationException("should not reach");
                                    }))
                            .validate()
            ).doesNotThrowAnyException();
        }
    }

    // --- ToManyRelationshipBody ---

    @Nested
    class ToManyRelationshipBodyValidation {

        @Test
        void validate_validToManyBody_doesNotThrow() {
            var ri = resourceIdentifier("US", "countries");
            var doc = new ToManyRelationshipsDoc(List.of(ri), null, null);
            when(request.getToManyRelationshipDocPayload()).thenReturn(doc);

            assertThatCode(() ->
                    forRequest(request)
                            .toManyRelationshipBody(body -> body
                                    .withResourceTypeValidator(type -> {}))
                            .validate()
            ).doesNotThrowAnyException();
        }

        @Test
        void validate_emptyData_doesNotThrow() {
            var doc = new ToManyRelationshipsDoc(List.of(), null, null);
            when(request.getToManyRelationshipDocPayload()).thenReturn(doc);

            assertThatCode(() ->
                    forRequest(request)
                            .toManyRelationshipBody(body -> body
                                    .withResourceTypeValidator(type -> {
                                        throw new JsonApiRequestValidationException("should not reach");
                                    }))
                            .validate()
            ).doesNotThrowAnyException();
        }
    }

    // --- Cross-section error collection ---

    @Nested
    class CrossSectionErrorCollection {

        @Test
        void validate_pathAndParametersFail_collectsErrorsFromBothSections() {
            when(request.getResourceId()).thenReturn("bad");
            when(request.getFilters()).thenReturn(Map.of("id", List.of("bad")));

            assertThatThrownBy(() ->
                    forRequest(request)
                            .path(path -> path
                                    .withResourceIdValidator(id -> {
                                        throw new JsonApiRequestValidationException("bad id");
                                    }))
                            .parameters(params -> params
                                    .withFilterValidator("id", v -> {
                                        throw new JsonApiRequestValidationException("bad filter");
                                    }))
                            .validate()
            )
                    .isInstanceOf(CompositeJsonApiRequestValidationException.class)
                    .satisfies(e -> {
                        var errors = ((CompositeJsonApiRequestValidationException) e).getValidationErrors();
                        assertThat(errors).hasSize(2);
                        assertThat(errors.get(0).detail()).isEqualTo("bad id");
                        assertThat(errors.get(0).source()).isInstanceOf(ErrorSources.Path.class);
                        assertThat(errors.get(1).detail()).isEqualTo("bad filter");
                        assertThat(errors.get(1).source()).isInstanceOf(ErrorSources.Parameter.class);
                    });
        }

        @Test
        void validate_pathHeadersAndBodyFail_collectsAllThreeSections() {
            when(request.getResourceId()).thenReturn("bad");
            when(request.getHeader("X-Tenant")).thenReturn(null);
            mockSingleResourceDoc(null, "wrong", null, null);

            assertThatThrownBy(() ->
                    forRequest(request)
                            .path(path -> path
                                    .withResourceIdValidator(id -> {
                                        throw new JsonApiRequestValidationException("bad id");
                                    }))
                            .headers(h -> h
                                    .withHeaderValidator("X-Tenant", v -> {
                                        throw new JsonApiRequestValidationException("missing tenant");
                                    }))
                            .singleResourceBody(body -> body
                                    .withResourceTypeValidator(type -> {
                                        throw new JsonApiRequestValidationException("wrong type");
                                    }))
                            .validate()
            )
                    .isInstanceOf(CompositeJsonApiRequestValidationException.class)
                    .satisfies(e -> {
                        var errors = ((CompositeJsonApiRequestValidationException) e).getValidationErrors();
                        assertThat(errors).hasSize(3);
                        assertThat(errors.get(0).detail()).isEqualTo("bad id");
                        assertThat(errors.get(1).detail()).isEqualTo("missing tenant");
                        assertThat(errors.get(2).detail()).isEqualTo("wrong type");
                    });
        }
    }

    // --- Helpers ---

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockSingleResourceDoc(String id, String type, Object attributes,
                                       LinkedHashMap<String, RelationshipObject> relationships) {
        var ro = new ResourceObject<>(id, null, type, attributes, relationships, null, null);
        var doc = new SingleResourceDoc(ro);
        when(request.getSingleResourceDocPayload()).thenReturn(doc);
    }

    private static ResourceIdentifierObject resourceIdentifier(String id, String type) {
        return new ResourceIdentifierObject(id, null, type, null);
    }

}
