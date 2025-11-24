package pro.api4.jsonapi4j.ac.model.outbound;

import lombok.Data;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.ac.annotation.AccessControlScopes;

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
