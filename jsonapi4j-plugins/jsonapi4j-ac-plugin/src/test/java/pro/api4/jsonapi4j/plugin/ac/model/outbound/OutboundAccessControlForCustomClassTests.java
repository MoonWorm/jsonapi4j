package pro.api4.jsonapi4j.plugin.ac.model.outbound;

import lombok.Data;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlScopes;
import pro.api4.jsonapi4j.plugin.ac.annotation.ScopesGroup;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OutboundAccessControlForCustomClassTests {

    @Test
    public void fromObjectClass_Annotations_checkDifferentScenarios() {
        // given - when
        TargetClass targetClass = new TargetClass();
        OutboundAccessControlForCustomClass actualResult
                = OutboundAccessControlForCustomClass.fromClassAnnotationsOf(targetClass);

        // then
        assertThat(actualResult).isNotNull();
        assertThat(actualResult.getClassLevel()).isNotNull();
        assertThat(actualResult.getClassLevel().getRequiredScopes()).isNotNull();
        assertThat(actualResult.getClassLevel().getRequiredScopes().getGroups().getFirst().getScopes()).isNotNull().isEqualTo(Set.of("TargetClass"));
        assertThat(actualResult.getFieldLevel()).isNotNull().isNotEmpty().hasSize(5);
        assertThat(actualResult.getFieldLevel().get("CONSTANT_1")).isNotNull();
        assertThat(actualResult.getFieldLevel().get("CONSTANT_1").getRequiredScopes()).isNotNull();
        assertThat(actualResult.getFieldLevel().get("CONSTANT_1").getRequiredScopes().getGroups().getFirst().getScopes()).isNotNull().isEqualTo(Set.of("CONSTANT_1"));
        assertThat(actualResult.getFieldLevel().get("CONSTANT_2")).isNotNull();
        assertThat(actualResult.getFieldLevel().get("CONSTANT_2").getRequiredScopes()).isNotNull();
        assertThat(actualResult.getFieldLevel().get("CONSTANT_2").getRequiredScopes().getGroups().getFirst().getScopes()).isNotNull().isEqualTo(Set.of("CONSTANT_2"));
        assertThat(actualResult.getFieldLevel().get("p1")).isNotNull();
        assertThat(actualResult.getFieldLevel().get("p1").getRequiredScopes()).isNotNull();
        assertThat(actualResult.getFieldLevel().get("p1").getRequiredScopes().getGroups().getFirst().getScopes()).isNotNull().isEqualTo(Set.of("p1"));
        assertThat(actualResult.getFieldLevel().get("t1")).isNotNull();
        assertThat(actualResult.getFieldLevel().get("t1").getRequiredScopes()).isNotNull();
        assertThat(actualResult.getFieldLevel().get("t1").getRequiredScopes().getGroups().getFirst().getScopes()).isNotNull().isEqualTo(Set.of("t1"));
        assertThat(actualResult.getFieldLevel().get("t2")).isNotNull();
        assertThat(actualResult.getFieldLevel().get("t2").getRequiredScopes()).isNotNull();
        assertThat(actualResult.getFieldLevel().get("t2").getRequiredScopes().getGroups().getFirst().getScopes()).isNotNull().isEqualTo(Set.of("t2"));
        assertThat(actualResult.getNested()).isNotNull().isNotEmpty().hasSize(2);
        assertThat(actualResult.getNested().get("CONSTANT_2")).isNotNull();
        assertThat(actualResult.getNested().get("CONSTANT_2").getClassLevel()).isNotNull();
        assertThat(actualResult.getNested().get("CONSTANT_2").getClassLevel().getRequiredScopes()).isNotNull();
        assertThat(actualResult.getNested().get("CONSTANT_2").getClassLevel().getRequiredScopes().getGroups().getFirst().getScopes()).isNotNull().isEqualTo(Set.of("NestedClassA"));
        assertThat(actualResult.getNested().get("CONSTANT_2").getFieldLevel()).isNotEmpty().hasSize(1);
        assertThat(actualResult.getNested().get("CONSTANT_2").getFieldLevel().get("a1")).isNotNull();
        assertThat(actualResult.getNested().get("CONSTANT_2").getFieldLevel().get("a1").getRequiredScopes()).isNotNull();
        assertThat(actualResult.getNested().get("CONSTANT_2").getFieldLevel().get("a1").getRequiredScopes().getGroups().getFirst().getScopes()).isNotNull().isEqualTo(Set.of("a1"));
        assertThat(actualResult.getNested().get("CONSTANT_2").getNested()).isEmpty();
        assertThat(actualResult.getNested().get("t2")).isNotNull();
        assertThat(actualResult.getNested().get("t2").getClassLevel()).isNotNull();
        assertThat(actualResult.getNested().get("t2").getClassLevel().getRequiredScopes()).isNotNull();
        assertThat(actualResult.getNested().get("t2").getClassLevel().getRequiredScopes().getGroups().getFirst().getScopes()).isNotNull().isEqualTo(Set.of("NestedClassB"));
        assertThat(actualResult.getNested().get("t2").getFieldLevel()).isNotEmpty().hasSize(1);
        assertThat(actualResult.getNested().get("t2").getFieldLevel().get("b1")).isNotNull();
        assertThat(actualResult.getNested().get("t2").getFieldLevel().get("b1").getRequiredScopes()).isNotNull();
        assertThat(actualResult.getNested().get("t2").getFieldLevel().get("b1").getRequiredScopes().getGroups().getFirst().getScopes()).isNotNull().isEqualTo(Set.of("b1"));
        assertThat(actualResult.getNested().get("t2").getNested()).isEmpty();
    }

    @Test
    public void merge_twoEqual_resultIsTheSame() {
        // given
        TargetClass targetClass1 = new TargetClass();
        OutboundAccessControlForCustomClass lowerPrecedence
                = OutboundAccessControlForCustomClass.fromClassAnnotationsOf(targetClass1);
        TargetClass targetClass2 = new TargetClass();
        OutboundAccessControlForCustomClass higherPrecedence
                = OutboundAccessControlForCustomClass.fromClassAnnotationsOf(targetClass2);

        // when
        OutboundAccessControlForCustomClass actualResult
                = OutboundAccessControlForCustomClass.merge(lowerPrecedence, higherPrecedence);

        // then
        assertThat(actualResult).isNotNull().isEqualTo(lowerPrecedence);
        assertThat(actualResult).isNotNull().isEqualTo(higherPrecedence);
    }

    @Test
    public void fromObjectClass_enumTypedField_doesNotRecurseIntoConstants_noStackOverflow() {
        // given
        ClassWithEnumField target = new ClassWithEnumField();

        // when - must not throw StackOverflowError from recursing into the enum's self-referential constants
        OutboundAccessControlForCustomClass actualResult
                = OutboundAccessControlForCustomClass.fromClassAnnotationsOf(target);

        // then - the enum's own class-level access control is captured, but it is treated as a leaf
        assertThat(actualResult).isNotNull();
        assertThat(actualResult.getNested()).containsKey("status");
        OutboundAccessControlForCustomClass statusAc = actualResult.getNested().get("status");
        assertThat(statusAc.getClassLevel()).isNotNull();
        assertThat(statusAc.getClassLevel().getRequiredScopes().getGroups().getFirst().getScopes()).isEqualTo(Set.of("Status"));
        assertThat(statusAc.getNested()).isEmpty();
    }

    @Test
    public void fromObjectClass_selfReferentialField_doesNotRecurseInfinitely_noStackOverflow() {
        // given
        SelfReferential target = new SelfReferential();

        // when - a class referencing its own type must not blow the stack
        OutboundAccessControlForCustomClass actualResult
                = OutboundAccessControlForCustomClass.fromClassAnnotationsOf(target);

        // then
        assertThat(actualResult).isNotNull();
        assertThat(actualResult.getNested().get("child")).isNotNull();
    }

    @Nested
    class Caching {

        @Test
        void fromClassAnnotationsOf_sameClassTwice_returnsTheSameInstance() {
            OutboundAccessControlForCustomClass first
                    = OutboundAccessControlForCustomClass.fromClassAnnotationsOf(new TargetClass());
            OutboundAccessControlForCustomClass second
                    = OutboundAccessControlForCustomClass.fromClassAnnotationsOf(new TargetClass());

            assertThat(second).isSameAs(first);
        }

        @Test
        void fromClassAnnotationsOf_sameResourceClassDifferentAttributesClass_returnsDifferentInstances() {
            // given - one ResourceObject class carrying two different attributes types
            OutboundAccessControlForCustomClass withTarget
                    = OutboundAccessControlForCustomClass.fromClassAnnotationsOf(resourceObjectOf(new TargetClass()));
            OutboundAccessControlForCustomClass withNestedB
                    = OutboundAccessControlForCustomClass.fromClassAnnotationsOf(resourceObjectOf(new NestedClassB()));

            // then - attributes requirements must not leak from one attributes type to the other
            assertThat(withNestedB).isNotSameAs(withTarget);
            assertThat(withTarget.getNested().get(ResourceObject.ATTRIBUTES_FIELD).getClassLevel()
                    .getRequiredScopes().getGroups().getFirst().getScopes()).isEqualTo(Set.of("TargetClass"));
            assertThat(withNestedB.getNested().get(ResourceObject.ATTRIBUTES_FIELD).getClassLevel()
                    .getRequiredScopes().getGroups().getFirst().getScopes()).isEqualTo(Set.of("NestedClassB"));
        }

        @Test
        void fromClassAnnotationsOf_resourceObjectWithoutAttributes_doesNotThrow() {
            OutboundAccessControlForCustomClass actualResult
                    = OutboundAccessControlForCustomClass.fromClassAnnotationsOf(resourceObjectOf(null));

            assertThat(actualResult).isNotNull();
            assertThat(actualResult.getNested()).doesNotContainKey(ResourceObject.ATTRIBUTES_FIELD);
        }

        @Test
        void fromClassAnnotationsOf_nullObject_returnsNull() {
            assertThat(OutboundAccessControlForCustomClass.fromClassAnnotationsOf(null)).isNull();
        }

        @Test
        void fromClassAnnotationsOf_cachedResult_nestedMapsRejectMutation() {
            OutboundAccessControlForCustomClass actualResult
                    = OutboundAccessControlForCustomClass.fromClassAnnotationsOf(new TargetClass());

            assertThatThrownBy(() -> actualResult.getNested().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> actualResult.getNested().get("t2").getNested().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        private ResourceObject<Object, Object> resourceObjectOf(Object attributes) {
            return new ResourceObject<>("1", null, "things", attributes, null, null, null);
        }

    }

    @AccessControl(scopes = @AccessControlScopes(@ScopesGroup("Status")))
    private enum Status {
        ACTIVE, INACTIVE
    }

    @Data
    private static class ClassWithEnumField {
        @AccessControl(scopes = @AccessControlScopes(@ScopesGroup("status")))
        private Status status;
    }

    @AccessControl(scopes = @AccessControlScopes(@ScopesGroup("SelfReferential")))
    @Data
    private static class SelfReferential {
        @AccessControl(scopes = @AccessControlScopes(@ScopesGroup("child")))
        private SelfReferential child;
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
