package pro.api4.jsonapi4j.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.exception.CompositeJsonApiRequestValidationException;
import pro.api4.jsonapi4j.exception.JsonApiRequestValidationException;
import pro.api4.jsonapi4j.model.document.data.RelationshipObject;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import pro.api4.jsonapi4j.operation.validation.ValidationProperties;
import pro.api4.jsonapi4j.request.JsonApiRequest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultJsonApiBuildInRequestValidatorTests {

    private static final ResourceType USERS = new ResourceType("users");
    private static final RelationshipName CITIZENSHIPS = new RelationshipName("citizenships");

    @Mock
    private DomainRegistry domainRegistry;

    @Mock
    private ValidationProperties properties;

    @Mock
    private JsonApiRequest request;

    private DefaultJsonApiBuildInRequestValidator validator;

    @BeforeEach
    void setUp() {
        validator = DefaultJsonApiBuildInRequestValidator.builder()
                .domainRegistry(domainRegistry)
                .objectMapper(new ObjectMapper())
                .properties(properties)
                .build();
        setupDefaultProperties();
    }

    private void setupDefaultProperties() {
        lenient().when(properties.maxElementsInIncludeParam()).thenReturn(10);
        lenient().when(properties.maxElementsInSortByParam()).thenReturn(5);
        lenient().when(properties.maxNumberFilterParams()).thenReturn(5);
        lenient().when(properties.maxElementsInFilterParam()).thenReturn(20);
        lenient().when(properties.resourceIdMaxLength()).thenReturn(64);
        lenient().when(properties.limitMaxValue()).thenReturn(100L);
    }

    private void setupKnownResourceType() {
        when(domainRegistry.getResourceTypes()).thenReturn(Set.of(USERS));
    }

    private void setupKnownRelationships() {
        when(domainRegistry.getRelationshipNames(USERS)).thenReturn(Set.of(CITIZENSHIPS));
    }

    private void setupBasicPathRequest() {
        when(request.getTargetResourceType()).thenReturn(USERS);
        when(request.getResourceId()).thenReturn("1");
    }

    // --- validateReadResourceById ---

    @Nested
    class ReadResourceById {

        @Test
        void valid_doesNotThrow() {
            setupKnownResourceType();
            setupBasicPathRequest();
            when(request.getOriginalIncludes()).thenReturn(List.of());
            when(request.getFilters()).thenReturn(Map.of());
            when(request.getSortBy()).thenReturn(Map.of());

            assertThatCode(() -> validator.validateReadResourceById(request))
                    .doesNotThrowAnyException();
        }

        @Test
        void unknownResourceType_throws() {
            when(domainRegistry.getResourceTypes()).thenReturn(Set.of(new ResourceType("countries")));
            when(request.getTargetResourceType()).thenReturn(USERS);
            when(request.getResourceId()).thenReturn("1");
            when(request.getOriginalIncludes()).thenReturn(List.of());
            when(request.getFilters()).thenReturn(Map.of());
            when(request.getSortBy()).thenReturn(Map.of());

            assertThatThrownBy(() -> validator.validateReadResourceById(request))
                    .isInstanceOf(JsonApiRequestValidationException.class);
        }

        @Test
        void blankResourceId_throws() {
            setupKnownResourceType();
            when(request.getTargetResourceType()).thenReturn(USERS);
            when(request.getResourceId()).thenReturn("");

            assertThatThrownBy(() -> validator.validateReadResourceById(request))
                    .isInstanceOf(JsonApiRequestValidationException.class);
        }

        @Test
        void resourceIdTooLong_throws() {
            setupKnownResourceType();
            when(request.getTargetResourceType()).thenReturn(USERS);
            when(request.getResourceId()).thenReturn("x".repeat(65));

            assertThatThrownBy(() -> validator.validateReadResourceById(request))
                    .isInstanceOf(JsonApiRequestValidationException.class);
        }

        @Test
        void tooManyIncludes_throws() {
            setupKnownResourceType();
            setupBasicPathRequest();
            when(properties.maxElementsInIncludeParam()).thenReturn(2);
            when(request.getOriginalIncludes()).thenReturn(List.of("a", "b", "c"));

            assertThatThrownBy(() -> validator.validateReadResourceById(request))
                    .isInstanceOf(JsonApiRequestValidationException.class);
        }

        @Test
        void tooManyFilters_throws() {
            setupKnownResourceType();
            setupBasicPathRequest();
            when(request.getOriginalIncludes()).thenReturn(List.of());
            when(properties.maxNumberFilterParams()).thenReturn(1);
            when(request.getFilters()).thenReturn(Map.of("a", List.of("1"), "b", List.of("2")));

            assertThatThrownBy(() -> validator.validateReadResourceById(request))
                    .isInstanceOf(JsonApiRequestValidationException.class);
        }

        @Test
        void tooManySortFields_throws() {
            setupKnownResourceType();
            setupBasicPathRequest();
            when(request.getOriginalIncludes()).thenReturn(List.of());
            when(request.getFilters()).thenReturn(Map.of());
            when(properties.maxElementsInSortByParam()).thenReturn(0);
            when(request.getSortBy()).thenReturn(Map.of("name", pro.api4.jsonapi4j.request.SortAwareRequest.SortOrder.ASC));

            assertThatThrownBy(() -> validator.validateReadResourceById(request))
                    .isInstanceOf(JsonApiRequestValidationException.class);
        }
    }

    // --- validateReadMultipleResources ---

    @Nested
    class ReadMultipleResources {

        @Test
        void valid_doesNotThrow() {
            setupKnownResourceType();
            when(request.getTargetResourceType()).thenReturn(USERS);
            when(request.getOriginalIncludes()).thenReturn(List.of());
            when(request.getFilters()).thenReturn(Map.of());
            when(request.getLimit()).thenReturn(null);
            when(request.getSortBy()).thenReturn(Map.of());

            assertThatCode(() -> validator.validateReadMultipleResources(request))
                    .doesNotThrowAnyException();
        }

        @Test
        void limitTooHigh_throws() {
            setupKnownResourceType();
            when(request.getTargetResourceType()).thenReturn(USERS);
            when(request.getOriginalIncludes()).thenReturn(List.of());
            when(request.getFilters()).thenReturn(Map.of());
            when(request.getLimit()).thenReturn(101L);
            when(request.getSortBy()).thenReturn(Map.of());

            assertThatThrownBy(() -> validator.validateReadMultipleResources(request))
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> assertThat(e.getMessage()).contains("max allowed limit"));
        }
    }

    // --- validateCreateResource ---

    @Nested
    class CreateResource {

        @Test
        void valid_doesNotThrow() {
            setupKnownResourceType();
            when(request.getTargetResourceType()).thenReturn(USERS);
            mockSingleResourceDoc(null, "users", null, null);

            assertThatCode(() -> validator.validateCreateResource(request))
                    .doesNotThrowAnyException();
        }

        @Test
        void nullData_throws() {
            setupKnownResourceType();
            when(request.getTargetResourceType()).thenReturn(USERS);
            when(request.getSingleResourceDocPayload()).thenReturn(new SingleResourceDoc<>(null));

            assertThatThrownBy(() -> validator.validateCreateResource(request))
                    .isInstanceOf(JsonApiRequestValidationException.class);
        }

        @Test
        void resourceIdPresent_throws() {
            setupKnownResourceType();
            when(request.getTargetResourceType()).thenReturn(USERS);
            mockSingleResourceDoc("123", "users", null, null);

            assertThatThrownBy(() -> validator.validateCreateResource(request))
                    .isInstanceOf(JsonApiRequestValidationException.class);
        }

        @Test
        void typeMismatch_throws() {
            setupKnownResourceType();
            when(request.getTargetResourceType()).thenReturn(USERS);
            mockSingleResourceDoc(null, "countries", null, null);

            assertThatThrownBy(() -> validator.validateCreateResource(request))
                    .isInstanceOf(JsonApiRequestValidationException.class);
        }

        @Test
        void resourceIdPresentAndTypeMismatch_throwsComposite() {
            setupKnownResourceType();
            when(request.getTargetResourceType()).thenReturn(USERS);
            mockSingleResourceDoc("123", "countries", null, null);

            assertThatThrownBy(() -> validator.validateCreateResource(request))
                    .isInstanceOf(CompositeJsonApiRequestValidationException.class)
                    .satisfies(e -> {
                        var errors = ((CompositeJsonApiRequestValidationException) e).getValidationErrors();
                        assertThat(errors).hasSize(2);
                    });
        }
    }

    // --- validateUpdateResource ---

    @Nested
    class UpdateResource {

        @Test
        void valid_doesNotThrow() {
            setupKnownResourceType();
            setupBasicPathRequest();
            mockSingleResourceDoc("1", "users", null, null);

            assertThatCode(() -> validator.validateUpdateResource(request))
                    .doesNotThrowAnyException();
        }

        @Test
        void resourceIdMismatch_throws() {
            setupKnownResourceType();
            setupBasicPathRequest();
            mockSingleResourceDoc("999", "users", null, null);

            assertThatThrownBy(() -> validator.validateUpdateResource(request))
                    .isInstanceOf(JsonApiRequestValidationException.class);
        }

        @Test
        void blankResourceIdInBody_throws() {
            setupKnownResourceType();
            setupBasicPathRequest();
            mockSingleResourceDoc("", "users", null, null);

            assertThatThrownBy(() -> validator.validateUpdateResource(request))
                    .isInstanceOf(JsonApiRequestValidationException.class);
        }
    }

    // --- validateDeleteResource ---

    @Nested
    class DeleteResource {

        @Test
        void valid_doesNotThrow() {
            setupKnownResourceType();
            setupBasicPathRequest();

            assertThatCode(() -> validator.validateDeleteResource(request))
                    .doesNotThrowAnyException();
        }

        @Test
        void blankResourceId_throws() {
            setupKnownResourceType();
            when(request.getTargetResourceType()).thenReturn(USERS);
            when(request.getResourceId()).thenReturn("");

            assertThatThrownBy(() -> validator.validateDeleteResource(request))
                    .isInstanceOf(JsonApiRequestValidationException.class);
        }
    }

    // --- validateReadToOneRelationship ---

    @Nested
    class ReadToOneRelationship {

        @Test
        void valid_doesNotThrow() {
            setupKnownResourceType();
            setupKnownRelationships();
            setupBasicPathRequest();
            when(request.getTargetRelationshipName()).thenReturn(CITIZENSHIPS);
            when(request.getOriginalIncludes()).thenReturn(List.of());
            when(request.getFilters()).thenReturn(Map.of());
            when(request.getSortBy()).thenReturn(Map.of());

            assertThatCode(() -> validator.validateReadToOneRelationship(request))
                    .doesNotThrowAnyException();
        }

        @Test
        void unknownRelationshipName_throws() {
            setupKnownResourceType();
            setupKnownRelationships();
            setupBasicPathRequest();
            when(request.getTargetRelationshipName()).thenReturn(new RelationshipName("unknown"));

            assertThatThrownBy(() -> validator.validateReadToOneRelationship(request))
                    .isInstanceOf(JsonApiRequestValidationException.class);
        }
    }

    // --- validateUpdateToOneRelationship ---

    @Nested
    class UpdateToOneRelationship {

        @Test
        void valid_doesNotThrow() {
            setupKnownResourceType();
            setupKnownRelationships();
            setupBasicPathRequest();
            when(request.getTargetRelationshipName()).thenReturn(CITIZENSHIPS);
            var ri = new ResourceIdentifierObject("US", null, "countries", null);
            when(request.getToOneRelationshipDocPayload()).thenReturn(new ToOneRelationshipDoc(ri, null, null));

            assertThatCode(() -> validator.validateUpdateToOneRelationship(request))
                    .doesNotThrowAnyException();
        }

        @Test
        void blankResourceIdentifierId_throws() {
            setupKnownResourceType();
            setupKnownRelationships();
            setupBasicPathRequest();
            when(request.getTargetRelationshipName()).thenReturn(CITIZENSHIPS);
            var ri = new ResourceIdentifierObject("", null, "countries", null);
            when(request.getToOneRelationshipDocPayload()).thenReturn(new ToOneRelationshipDoc(ri, null, null));

            assertThatThrownBy(() -> validator.validateUpdateToOneRelationship(request))
                    .isInstanceOf(JsonApiRequestValidationException.class);
        }

        @Test
        void blankResourceIdentifierType_throws() {
            setupKnownResourceType();
            setupKnownRelationships();
            setupBasicPathRequest();
            when(request.getTargetRelationshipName()).thenReturn(CITIZENSHIPS);
            var ri = new ResourceIdentifierObject("US", null, "", null);
            when(request.getToOneRelationshipDocPayload()).thenReturn(new ToOneRelationshipDoc(ri, null, null));

            assertThatThrownBy(() -> validator.validateUpdateToOneRelationship(request))
                    .isInstanceOf(JsonApiRequestValidationException.class);
        }

        @Test
        void blankIdAndType_throwsComposite() {
            setupKnownResourceType();
            setupKnownRelationships();
            setupBasicPathRequest();
            when(request.getTargetRelationshipName()).thenReturn(CITIZENSHIPS);
            var ri = new ResourceIdentifierObject("", null, "", null);
            when(request.getToOneRelationshipDocPayload()).thenReturn(new ToOneRelationshipDoc(ri, null, null));

            assertThatThrownBy(() -> validator.validateUpdateToOneRelationship(request))
                    .isInstanceOf(CompositeJsonApiRequestValidationException.class)
                    .satisfies(e -> {
                        var errors = ((CompositeJsonApiRequestValidationException) e).getValidationErrors();
                        assertThat(errors).hasSize(2);
                    });
        }
    }

    // --- validateUpdateToManyRelationship ---

    @Nested
    class UpdateToManyRelationship {

        @Test
        void valid_doesNotThrow() {
            setupKnownResourceType();
            setupKnownRelationships();
            setupBasicPathRequest();
            when(request.getTargetRelationshipName()).thenReturn(CITIZENSHIPS);
            var ri = new ResourceIdentifierObject("US", null, "countries", null);
            when(request.getToManyRelationshipDocPayload()).thenReturn(new ToManyRelationshipsDoc(List.of(ri), null, null));

            assertThatCode(() -> validator.validateUpdateToManyRelationship(request))
                    .doesNotThrowAnyException();
        }

        @Test
        void multipleElementsWithBlankType_throwsComposite() {
            setupKnownResourceType();
            setupKnownRelationships();
            setupBasicPathRequest();
            when(request.getTargetRelationshipName()).thenReturn(CITIZENSHIPS);
            var ri0 = new ResourceIdentifierObject("US", null, "", null);
            var ri1 = new ResourceIdentifierObject("NO", null, "", null);
            when(request.getToManyRelationshipDocPayload()).thenReturn(new ToManyRelationshipsDoc(List.of(ri0, ri1), null, null));

            assertThatThrownBy(() -> validator.validateUpdateToManyRelationship(request))
                    .isInstanceOf(CompositeJsonApiRequestValidationException.class)
                    .satisfies(e -> {
                        var errors = ((CompositeJsonApiRequestValidationException) e).getValidationErrors();
                        assertThat(errors).hasSize(2);
                    });
        }
    }

    // --- validateReadToManyRelationship ---

    @Nested
    class ReadToManyRelationship {

        @Test
        void valid_doesNotThrow() {
            setupKnownResourceType();
            setupKnownRelationships();
            setupBasicPathRequest();
            when(request.getTargetRelationshipName()).thenReturn(CITIZENSHIPS);
            when(request.getOriginalIncludes()).thenReturn(List.of());
            when(request.getFilters()).thenReturn(Map.of());
            when(request.getLimit()).thenReturn(null);
            when(request.getSortBy()).thenReturn(Map.of());

            assertThatCode(() -> validator.validateReadToManyRelationship(request))
                    .doesNotThrowAnyException();
        }

        @Test
        void limitTooHigh_throws() {
            setupKnownResourceType();
            setupKnownRelationships();
            setupBasicPathRequest();
            when(request.getTargetRelationshipName()).thenReturn(CITIZENSHIPS);
            when(request.getOriginalIncludes()).thenReturn(List.of());
            when(request.getFilters()).thenReturn(Map.of());
            when(request.getLimit()).thenReturn(101L);
            when(request.getSortBy()).thenReturn(Map.of());

            assertThatThrownBy(() -> validator.validateReadToManyRelationship(request))
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> assertThat(e.getMessage()).contains("max allowed limit"));
        }
    }

    // --- Cross-section error collection ---

    @Nested
    class CrossSectionErrors {

        @Test
        void readResourceById_unknownTypeAndBlankId_collectsBoth() {
            when(domainRegistry.getResourceTypes()).thenReturn(Set.of(new ResourceType("countries")));
            when(request.getTargetResourceType()).thenReturn(USERS);
            when(request.getResourceId()).thenReturn("");

            assertThatThrownBy(() -> validator.validateReadResourceById(request))
                    .isInstanceOf(CompositeJsonApiRequestValidationException.class)
                    .satisfies(e -> {
                        var errors = ((CompositeJsonApiRequestValidationException) e).getValidationErrors();
                        assertThat(errors).hasSize(2);
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

}
