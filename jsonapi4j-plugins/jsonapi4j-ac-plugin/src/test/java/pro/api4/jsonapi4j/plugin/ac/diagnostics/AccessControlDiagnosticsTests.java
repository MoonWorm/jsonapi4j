package pro.api4.jsonapi4j.plugin.ac.diagnostics;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlScopes;
import pro.api4.jsonapi4j.plugin.ac.annotation.Authenticated;
import pro.api4.jsonapi4j.plugin.ac.annotation.ScopesGroup;
import pro.api4.jsonapi4j.plugin.ac.exception.AccessControlMisconfigurationException;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.util.ReflectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AccessControlDiagnosticsTests {

    @Nested
    class UnenforceableElementRequirements {

        @Test
        void unenforceableElementRequirements_listOfAnnotatedElements_reportsTheElementType() {
            assertThat(unenforceableFor("addresses")).containsExactly(AnnotatedElement.class);
        }

        @Test
        void unenforceableElementRequirements_arrayOfAnnotatedElements_reportsTheElementType() {
            assertThat(unenforceableFor("arr")).containsExactly(AnnotatedElement.class);
        }

        @Test
        void unenforceableElementRequirements_mapOfAnnotatedValues_reportsTheValueType() {
            assertThat(unenforceableFor("byName")).containsExactly(AnnotatedElement.class);
        }

        @Test
        void unenforceableElementRequirements_containerOfPlainElements_reportsNothing() {
            assertThat(unenforceableFor("plain")).isEmpty();
        }

        @Test
        void unenforceableElementRequirements_annotatedElementHeldDirectly_reportsNothing() {
            assertThat(unenforceableFor("primary")).isEmpty();
        }

        private Set<Class<?>> unenforceableFor(String fieldName) {
            return AccessControlDiagnostics.unenforceableElementRequirements(
                    ReflectionUtils.fetchFieldTypes(ContainerHolder.class).get(fieldName),
                    ReflectionUtils.fetchFields(ContainerHolder.class).get(fieldName).getGenericType());
        }

    }

    @Nested
    class UnhideableFields {

        @Test
        void primitiveFieldsAmong_primitiveField_isReported() {
            assertThat(AccessControlDiagnostics
                    .primitiveFieldsAmong(UnhideableHolder.class, Set.of("count", "name")))
                    .containsExactly("count");
        }

        @Test
        void primitiveFieldsAmong_boxedField_isNotReported() {
            assertThat(AccessControlDiagnostics
                    .primitiveFieldsAmong(UnhideableHolder.class, Set.of("boxedCount")))
                    .isEmpty();
        }

        @Test
        void staticFieldsAmong_staticField_isReported() {
            assertThat(AccessControlDiagnostics
                    .staticFieldsAmong(UnhideableHolder.class, Set.of("SHARED", "name")))
                    .containsExactly("SHARED");
        }

        @Test
        void staticFieldsAmong_instanceField_isNotReported() {
            assertThat(AccessControlDiagnostics
                    .staticFieldsAmong(UnhideableHolder.class, Set.of("name", "count")))
                    .isEmpty();
        }

    }

    @Nested
    class RequirementlessAccessControl {

        @Test
        void declaresNoRequirements_bareAnnotation_isTrue() {
            assertThat(AccessControlModel.fromClassAnnotation(BareAnnotationResource.class)
                    .declaresNoRequirements()).isTrue();
        }

        @Test
        void declaresNoRequirements_annotationDeclaringARequirement_isFalse() {
            assertThat(AccessControlModel.fromClassAnnotation(AuthenticatedResource.class)
                    .declaresNoRequirements()).isFalse();
        }

    }

    @Nested
    class Rejections {

        @Test
        void emptyEntitlementsGroup_namesWhatWasDeclared() {
            assertThat(AccessControlDiagnostics.emptyEntitlementsGroup(new String[]{"", " "}))
                    .isInstanceOf(AccessControlMisconfigurationException.class)
                    .hasMessageContaining("at least one non-blank entitlement");
        }

        @Test
        void emptyScopesGroup_namesWhatWasDeclared() {
            assertThat(AccessControlDiagnostics.emptyScopesGroup(new String[]{}))
                    .isInstanceOf(AccessControlMisconfigurationException.class)
                    .hasMessageContaining("at least one non-blank scope");
        }

        @Test
        void uninstantiablePolicy_namesThePolicyType() {
            assertThat(AccessControlDiagnostics.uninstantiablePolicy(String.class, new RuntimeException()))
                    .isInstanceOf(AccessControlMisconfigurationException.class)
                    .hasMessageContaining("no-argument constructor")
                    .hasMessageContaining(String.class.getName());
        }

        @Test
        void unredactable_namesTheTypeAndFields() {
            assertThat(AccessControlDiagnostics.unredactable(
                    String.class, Set.of("secret"), new RuntimeException()))
                    .isInstanceOf(AccessControlMisconfigurationException.class)
                    .hasMessageContaining("secret")
                    .hasMessageContaining(String.class.getName());
        }

    }

    @AccessControl(scopes = @AccessControlScopes(@ScopesGroup("elements.read")))
    private static class AnnotatedElement {
    }

    private static class PlainElement {
    }

    private static class ContainerHolder {

        private List<AnnotatedElement> addresses;
        private AnnotatedElement[] arr;
        private Map<String, AnnotatedElement> byName;
        private List<PlainElement> plain;
        private AnnotatedElement primary;

    }

    private static class UnhideableHolder {

        private static String SHARED;

        private int count;
        private Integer boxedCount;
        private String name;

    }

    @AccessControl
    private static class BareAnnotationResource {
    }

    @AccessControl(authenticated = Authenticated.AUTHENTICATED)
    private static class AuthenticatedResource {
    }

}
