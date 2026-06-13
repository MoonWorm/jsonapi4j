package pro.api4.jsonapi4j.plugin.ac.model.outbound;

import lombok.Data;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlScopes;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(actualResult.getClassLevel().getRequiredScopes().getRequiredScopes()).isNotNull().isEqualTo(Set.of("TargetClass"));
        assertThat(actualResult.getFieldLevel()).isNotNull().isNotEmpty().hasSize(5);
        assertThat(actualResult.getFieldLevel().get("CONSTANT_1")).isNotNull();
        assertThat(actualResult.getFieldLevel().get("CONSTANT_1").getRequiredScopes()).isNotNull();
        assertThat(actualResult.getFieldLevel().get("CONSTANT_1").getRequiredScopes().getRequiredScopes()).isNotNull().isEqualTo(Set.of("CONSTANT_1"));
        assertThat(actualResult.getFieldLevel().get("CONSTANT_2")).isNotNull();
        assertThat(actualResult.getFieldLevel().get("CONSTANT_2").getRequiredScopes()).isNotNull();
        assertThat(actualResult.getFieldLevel().get("CONSTANT_2").getRequiredScopes().getRequiredScopes()).isNotNull().isEqualTo(Set.of("CONSTANT_2"));
        assertThat(actualResult.getFieldLevel().get("p1")).isNotNull();
        assertThat(actualResult.getFieldLevel().get("p1").getRequiredScopes()).isNotNull();
        assertThat(actualResult.getFieldLevel().get("p1").getRequiredScopes().getRequiredScopes()).isNotNull().isEqualTo(Set.of("p1"));
        assertThat(actualResult.getFieldLevel().get("t1")).isNotNull();
        assertThat(actualResult.getFieldLevel().get("t1").getRequiredScopes()).isNotNull();
        assertThat(actualResult.getFieldLevel().get("t1").getRequiredScopes().getRequiredScopes()).isNotNull().isEqualTo(Set.of("t1"));
        assertThat(actualResult.getFieldLevel().get("t2")).isNotNull();
        assertThat(actualResult.getFieldLevel().get("t2").getRequiredScopes()).isNotNull();
        assertThat(actualResult.getFieldLevel().get("t2").getRequiredScopes().getRequiredScopes()).isNotNull().isEqualTo(Set.of("t2"));
        assertThat(actualResult.getNested()).isNotNull().isNotEmpty().hasSize(2);
        assertThat(actualResult.getNested().get("CONSTANT_2")).isNotNull();
        assertThat(actualResult.getNested().get("CONSTANT_2").getClassLevel()).isNotNull();
        assertThat(actualResult.getNested().get("CONSTANT_2").getClassLevel().getRequiredScopes()).isNotNull();
        assertThat(actualResult.getNested().get("CONSTANT_2").getClassLevel().getRequiredScopes().getRequiredScopes()).isNotNull().isEqualTo(Set.of("NestedClassA"));
        assertThat(actualResult.getNested().get("CONSTANT_2").getFieldLevel()).isNotEmpty().hasSize(1);
        assertThat(actualResult.getNested().get("CONSTANT_2").getFieldLevel().get("a1")).isNotNull();
        assertThat(actualResult.getNested().get("CONSTANT_2").getFieldLevel().get("a1").getRequiredScopes()).isNotNull();
        assertThat(actualResult.getNested().get("CONSTANT_2").getFieldLevel().get("a1").getRequiredScopes().getRequiredScopes()).isNotNull().isEqualTo(Set.of("a1"));
        assertThat(actualResult.getNested().get("CONSTANT_2").getNested()).isEmpty();
        assertThat(actualResult.getNested().get("t2")).isNotNull();
        assertThat(actualResult.getNested().get("t2").getClassLevel()).isNotNull();
        assertThat(actualResult.getNested().get("t2").getClassLevel().getRequiredScopes()).isNotNull();
        assertThat(actualResult.getNested().get("t2").getClassLevel().getRequiredScopes().getRequiredScopes()).isNotNull().isEqualTo(Set.of("NestedClassB"));
        assertThat(actualResult.getNested().get("t2").getFieldLevel()).isNotEmpty().hasSize(1);
        assertThat(actualResult.getNested().get("t2").getFieldLevel().get("b1")).isNotNull();
        assertThat(actualResult.getNested().get("t2").getFieldLevel().get("b1").getRequiredScopes()).isNotNull();
        assertThat(actualResult.getNested().get("t2").getFieldLevel().get("b1").getRequiredScopes().getRequiredScopes()).isNotNull().isEqualTo(Set.of("b1"));
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
        assertThat(statusAc.getClassLevel().getRequiredScopes().getRequiredScopes()).isEqualTo(Set.of("Status"));
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

    @AccessControl(scopes = @AccessControlScopes(requiredScopes = "Status"))
    private enum Status {
        ACTIVE, INACTIVE
    }

    @Data
    private static class ClassWithEnumField {
        @AccessControl(scopes = @AccessControlScopes(requiredScopes = "status"))
        private Status status;
    }

    @AccessControl(scopes = @AccessControlScopes(requiredScopes = "SelfReferential"))
    @Data
    private static class SelfReferential {
        @AccessControl(scopes = @AccessControlScopes(requiredScopes = "child"))
        private SelfReferential child;
    }

    @AccessControl(scopes = @AccessControlScopes(requiredScopes = "TargetClass"))
    @Data
    private static class TargetClass extends ParentClass {

        @AccessControl(scopes = @AccessControlScopes(requiredScopes = "CONSTANT_1"))
        private static final int CONSTANT_1 = 0;

        @AccessControl(scopes = @AccessControlScopes(requiredScopes = "CONSTANT_2"))
        private static final NestedClassA CONSTANT_2 = new NestedClassA("foo");

        @AccessControl(scopes = @AccessControlScopes(requiredScopes = "t1"))
        private String t1;

        @AccessControl(scopes = @AccessControlScopes(requiredScopes = "t2"))
        private NestedClassB t2;

        @AccessControl(scopes = @AccessControlScopes(requiredScopes = "NestedClassA"))
        @Data
        private static class NestedClassA {
            @AccessControl(scopes = @AccessControlScopes(requiredScopes = "a1"))
            private final String a1;
        }

    }

    @AccessControl(scopes = @AccessControlScopes(requiredScopes = "ParentClass"))
    @Data
    private static class ParentClass {

        @AccessControl(scopes = @AccessControlScopes(requiredScopes = "p1"))
        private String p1;

    }

    @AccessControl(scopes = @AccessControlScopes(requiredScopes = "NestedClassB"))
    @Data
    private static class NestedClassB {
        @AccessControl(scopes = @AccessControlScopes(requiredScopes = "b1"))
        private String b1;
    }

}
