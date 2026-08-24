package pro.api4.jsonapi4j.plugin.ac.copy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObjectCopierTests {

    @Nested
    class Records {

        @Test
        void copyWithout_recordWithBlankedComponent_copyHasItNullAndKeepsTheRest() {
            RecordAttributes sut = new RecordAttributes("John", "john@doe.com", "4111");

            RecordAttributes actualResult = ObjectCopier.copyWithout(sut, Set.of("creditCardNumber"));

            assertThat(actualResult.creditCardNumber()).isNull();
            assertThat(actualResult.fullName()).isEqualTo("John");
            assertThat(actualResult.email()).isEqualTo("john@doe.com");
        }

        @Test
        void copyWithout_record_sourceIsUnmodified() {
            RecordAttributes sut = new RecordAttributes("John", "john@doe.com", "4111");

            ObjectCopier.copyWithout(sut, Set.of("creditCardNumber"));

            assertThat(sut.creditCardNumber()).isEqualTo("4111");
        }

        @Test
        void isCopyable_record_isTrue() {
            assertThat(ObjectCopier.isCopyable(RecordAttributes.class)).isTrue();
        }

    }

    @Nested
    class AllArgsConstructorTypes {

        @Test
        void copyWithout_immutableClass_copyHasBlankedFieldNullAndKeepsTheRest() {
            ImmutableAttributes sut = new ImmutableAttributes("John", "john@doe.com", "4111");

            ImmutableAttributes actualResult = ObjectCopier.copyWithout(sut, Set.of("creditCardNumber"));

            assertThat(actualResult.getCreditCardNumber()).isNull();
            assertThat(actualResult.getFullName()).isEqualTo("John");
            assertThat(actualResult.getEmail()).isEqualTo("john@doe.com");
        }

        @Test
        void copyWithout_immutableClass_sourceIsUnmodified() {
            ImmutableAttributes sut = new ImmutableAttributes("John", "john@doe.com", "4111");

            ObjectCopier.copyWithout(sut, Set.of("creditCardNumber"));

            assertThat(sut.getCreditCardNumber()).isEqualTo("4111");
        }

        @Test
        void copyWithout_nothingBlanked_copyIsAFaithfulDistinctInstance() {
            ImmutableAttributes sut = new ImmutableAttributes("John", "john@doe.com", "4111");

            ImmutableAttributes actualResult = ObjectCopier.copyWithout(sut, Set.of());

            assertThat(actualResult).isNotSameAs(sut);
            assertThat(actualResult.getCreditCardNumber()).isEqualTo("4111");
        }

        @Test
        void copyWithout_subclassWhoseConstructorSkipsInheritedFields_carriesInheritedValueOver() {
            SubclassWithOwnFieldsConstructor sut = new SubclassWithOwnFieldsConstructor("own");
            sut.setId("42");

            SubclassWithOwnFieldsConstructor actualResult = ObjectCopier.copyWithout(sut, Set.of("own"));

            assertThat(actualResult.getId()).isEqualTo("42");
            assertThat(actualResult.getOwn()).isNull();
        }

        @Test
        void copyWithout_constructorThatReordersItsParameters_stillCopiesFaithfully() {
            ReorderingConstructorAttributes sut = new ReorderingConstructorAttributes("secret", "John");

            ReorderingConstructorAttributes actualResult = ObjectCopier.copyWithout(sut, Set.of("token"));

            assertThat(actualResult.getFullName()).isEqualTo("John");
            assertThat(actualResult.getToken()).isNull();
        }

        @Test
        void copyWithout_constructorThatRejectsNulls_canStillBlankThatField() {
            ValidatingAttributes sut = new ValidatingAttributes("john@doe.com", "4111");

            ValidatingAttributes actualResult = ObjectCopier.copyWithout(sut, Set.of("email"));

            assertThat(actualResult.getEmail()).isNull();
            assertThat(actualResult.getCreditCardNumber()).isEqualTo("4111");
        }

        @Test
        void copyWithout_constructorThatTransformsItsArguments_doesNotApplyTheTransformAgain() {
            SuffixingAttributes sut = new SuffixingAttributes("abc", "keep");

            SuffixingAttributes actualResult = ObjectCopier.copyWithout(sut, Set.of("other"));

            assertThat(actualResult.getCode()).isEqualTo(sut.getCode());
        }

        @Test
        void copyWithout_typeWithNoUsableConstructor_isStillCopyable() {
            NotCopyable sut = new NotCopyable("John");

            NotCopyable actualResult = ObjectCopier.copyWithout(sut, Set.of("fullName"));

            assertThat(actualResult.getFullName()).isNull();
        }

    }

    @Nested
    class MutableBeans {

        @Test
        void copyWithout_mutableBean_copyHasBlankedFieldNullAndKeepsTheRest() {
            MutableAttributes sut = new MutableAttributes();
            sut.setFullName("John");
            sut.setCreditCardNumber("4111");

            MutableAttributes actualResult = ObjectCopier.copyWithout(sut, Set.of("creditCardNumber"));

            assertThat(actualResult.getCreditCardNumber()).isNull();
            assertThat(actualResult.getFullName()).isEqualTo("John");
        }

        @Test
        void copyWithout_mutableBean_sourceIsUnmodified() {
            MutableAttributes sut = new MutableAttributes();
            sut.setCreditCardNumber("4111");

            ObjectCopier.copyWithout(sut, Set.of("creditCardNumber"));

            assertThat(sut.getCreditCardNumber()).isEqualTo("4111");
        }

        @Test
        void copyWithout_inheritedFields_areCarriedOverToTheCopy() {
            InheritingAttributes sut = new InheritingAttributes();
            sut.setId("42");
            sut.setCreditCardNumber("4111");

            InheritingAttributes actualResult = ObjectCopier.copyWithout(sut, Set.of("creditCardNumber"));

            assertThat(actualResult.getId()).isEqualTo("42");
            assertThat(actualResult.getCreditCardNumber()).isNull();
        }

        @Test
        void copyWithout_inheritedFieldBlanked_isNullInTheCopy() {
            InheritingAttributes sut = new InheritingAttributes();
            sut.setId("42");

            assertThat(ObjectCopier.copyWithout(sut, Set.of("id")).getId()).isNull();
        }

    }

    @Nested
    class UnsupportedTypes {

        @Test
        void isCopyable_interface_isFalse() {
            assertThat(ObjectCopier.isCopyable(Runnable.class)).isFalse();
        }

        @Test
        void isCopyable_abstractType_isFalse() {
            assertThat(ObjectCopier.isCopyable(AbstractAttributes.class)).isFalse();
        }

        @Test
        void copyWithout_primitiveFieldBlanked_throwsExplainingWhy() {
            MutableAttributes sut = new MutableAttributes();

            assertThatThrownBy(() -> ObjectCopier.copyWithout(sut, Set.of("age")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("primitive");
        }

        @Test
        void copyWithout_nullSource_returnsNull() {
            assertThat(ObjectCopier.copyWithout((Object) null, Set.of("anything"))).isNull();
        }

    }

    private record RecordAttributes(String fullName, String email, String creditCardNumber) {
    }

    private static class ImmutableAttributes {

        private final String fullName;
        private final String email;
        private final String creditCardNumber;

        ImmutableAttributes(String fullName, String email, String creditCardNumber) {
            this.fullName = fullName;
            this.email = email;
            this.creditCardNumber = creditCardNumber;
        }

        String getFullName() {
            return fullName;
        }

        String getEmail() {
            return email;
        }

        String getCreditCardNumber() {
            return creditCardNumber;
        }

    }

    private static class ReorderingConstructorAttributes {

        private final String fullName;
        private final String token;

        ReorderingConstructorAttributes(String secret, String name) {
            this.token = secret;
            this.fullName = name;
        }

        String getFullName() {
            return fullName;
        }

        String getToken() {
            return token;
        }

    }

    private static class SubclassWithOwnFieldsConstructor extends BaseAttributes {

        private final String own;

        SubclassWithOwnFieldsConstructor(String own) {
            this.own = own;
        }

        String getOwn() {
            return own;
        }

    }

    private abstract static class AbstractAttributes {

        private String whatever;

    }

    private static class ValidatingAttributes {

        private final String email;
        private final String creditCardNumber;

        ValidatingAttributes(String email, String creditCardNumber) {
            this.email = Objects.requireNonNull(email);
            this.creditCardNumber = creditCardNumber;
        }

        String getEmail() {
            return email;
        }

        String getCreditCardNumber() {
            return creditCardNumber;
        }

    }

    private static class SuffixingAttributes {

        private final String code;
        private final String other;

        SuffixingAttributes(String code, String other) {
            this.code = code + "-v2";
            this.other = other;
        }

        String getCode() {
            return code;
        }

        String getOther() {
            return other;
        }

    }

    private static class BaseAttributes {

        private String id;

        String getId() {
            return id;
        }

        void setId(String id) {
            this.id = id;
        }

    }

    private static class MutableAttributes {

        private String fullName;
        private String creditCardNumber;
        private int age;

        String getFullName() {
            return fullName;
        }

        void setFullName(String fullName) {
            this.fullName = fullName;
        }

        String getCreditCardNumber() {
            return creditCardNumber;
        }

        void setCreditCardNumber(String creditCardNumber) {
            this.creditCardNumber = creditCardNumber;
        }

        int getAge() {
            return age;
        }

    }

    private static class InheritingAttributes extends BaseAttributes {

        private String creditCardNumber;

        String getCreditCardNumber() {
            return creditCardNumber;
        }

        void setCreditCardNumber(String creditCardNumber) {
            this.creditCardNumber = creditCardNumber;
        }

    }

    private static class NotCopyable {

        private final String fullName;
        private final int age;

        NotCopyable(String fullName) {
            this.fullName = fullName;
            this.age = 0;
        }

        String getFullName() {
            return fullName;
        }

    }

}
