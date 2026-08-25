package pro.api4.jsonapi4j.plugin.ac.model.outbound;

import lombok.Data;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlScopes;
import pro.api4.jsonapi4j.plugin.ac.annotation.ScopesGroup;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboundAccessControlForCustomClassTests {

    @Nested
    class Building {

        @Test
        void fromClassAnnotationsOf_classLevelRequirement_isCaptured() {
            OutboundAccessControlForCustomClass actualResult
                    = OutboundAccessControlForCustomClass.fromClassAnnotationsOf(new TargetClass());

            assertThat(actualResult.getClassLevel().getRequiredScopes().getGroups().getFirst().getScopes())
                    .isEqualTo(Set.of("TargetClass"));
        }

        @Test
        void fromClassAnnotationsOf_fieldLevelRequirements_captureDeclaredAndInheritedFields() {
            OutboundAccessControlForCustomClass actualResult
                    = OutboundAccessControlForCustomClass.fromClassAnnotationsOf(new TargetClass());

            assertThat(actualResult.getFieldLevel()).containsKeys("t1", "t2", "p1", "CONSTANT_1", "CONSTANT_2");
        }

        @Test
        void fromClassAnnotationsOf_fieldRequirement_carriesItsScopes() {
            OutboundAccessControlForCustomClass actualResult
                    = OutboundAccessControlForCustomClass.fromClassAnnotationsOf(new TargetClass());

            assertThat(actualResult.getFieldLevel().get("t1").getRequiredScopes()
                    .getGroups().getFirst().getScopes()).isEqualTo(Set.of("t1"));
        }

        @Test
        void fromClassAnnotationsOf_classDeclaringNothing_returnsNull() {
            assertThat(OutboundAccessControlForCustomClass.fromClassAnnotationsOf(new Unannotated())).isNull();
        }

        @Test
        void fromClassAnnotationsOf_nullObject_returnsNull() {
            assertThat(OutboundAccessControlForCustomClass.fromClassAnnotationsOf(null)).isNull();
        }

        @Test
        void forClass_nullClass_returnsNull() {
            assertThat(OutboundAccessControlForCustomClass.forClass(null)).isNull();
        }

    }

    @Nested
    class Merging {

        @Test
        void merge_twoEqual_resultIsTheSame() {
            OutboundAccessControlForCustomClass lowerPrecedence
                    = OutboundAccessControlForCustomClass.fromClassAnnotationsOf(new TargetClass());
            OutboundAccessControlForCustomClass higherPrecedence
                    = OutboundAccessControlForCustomClass.fromClassAnnotationsOf(new TargetClass());

            OutboundAccessControlForCustomClass actualResult
                    = OutboundAccessControlForCustomClass.merge(lowerPrecedence, higherPrecedence);

            assertThat(actualResult).isEqualTo(lowerPrecedence).isEqualTo(higherPrecedence);
        }

        @Test
        void merge_bothNull_returnsNull() {
            assertThat(OutboundAccessControlForCustomClass.merge(null, null)).isNull();
        }

        @Test
        void merge_onlyOneSidePresent_keepsIt() {
            OutboundAccessControlForCustomClass only
                    = OutboundAccessControlForCustomClass.fromClassAnnotationsOf(new TargetClass());

            assertThat(OutboundAccessControlForCustomClass.merge(null, only).getClassLevel())
                    .isEqualTo(only.getClassLevel());
        }

    }

    @Nested
    class Caching {

        @Test
        void fromClassAnnotationsOf_sameClassTwice_returnsTheSameInstance() {
            OutboundAccessControlForCustomClass first
                    = OutboundAccessControlForCustomClass.fromClassAnnotationsOf(new TargetClass());

            assertThat(OutboundAccessControlForCustomClass.fromClassAnnotationsOf(new TargetClass()))
                    .isSameAs(first);
        }

        @Test
        void fromClassAnnotationsOf_differentClasses_returnDifferentInstances() {
            assertThat(OutboundAccessControlForCustomClass.fromClassAnnotationsOf(new TargetClass()))
                    .isNotSameAs(OutboundAccessControlForCustomClass.fromClassAnnotationsOf(new NestedClassB()));
        }

        @Test
        void fromClassAnnotationsOf_cachedResult_fieldLevelMapRejectsMutation() {
            OutboundAccessControlForCustomClass actualResult
                    = OutboundAccessControlForCustomClass.fromClassAnnotationsOf(new TargetClass());

            assertThatThrownBy(() -> actualResult.getFieldLevel().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }

    }

    @Data
    private static class Unannotated {
        private String plain;
    }

    @AccessControl(scopes = @AccessControlScopes(@ScopesGroup("TargetClass")))
    @Data
    private static class TargetClass extends ParentClass {

        @AccessControl(scopes = @AccessControlScopes(@ScopesGroup("CONSTANT_1")))
        private static final int CONSTANT_1 = 0;

        @AccessControl(scopes = @AccessControlScopes(@ScopesGroup("CONSTANT_2")))
        private static final NestedClassA CONSTANT_2 = new NestedClassA("foo");

        @AccessControl(scopes = @AccessControlScopes(@ScopesGroup("t1")))
        private String t1;

        @AccessControl(scopes = @AccessControlScopes(@ScopesGroup("t2")))
        private NestedClassB t2;

        @AccessControl(scopes = @AccessControlScopes(@ScopesGroup("NestedClassA")))
        @Data
        private static class NestedClassA {
            @AccessControl(scopes = @AccessControlScopes(@ScopesGroup("a1")))
            private final String a1;
        }

    }

    @AccessControl(scopes = @AccessControlScopes(@ScopesGroup("ParentClass")))
    @Data
    private static class ParentClass {

        @AccessControl(scopes = @AccessControlScopes(@ScopesGroup("p1")))
        private String p1;

    }

    @AccessControl(scopes = @AccessControlScopes(@ScopesGroup("NestedClassB")))
    @Data
    private static class NestedClassB {
        @AccessControl(scopes = @AccessControlScopes(@ScopesGroup("b1")))
        private String b1;
    }

}
