package pro.api4.jsonapi4j.operation.validation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.exception.JsonApiRequestValidationException;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidateTests {

    private static final ErrorSources.Source EMAIL_SOURCE = ErrorSources.pointer().data().attributes("email");

    // --- Object assertions ---

    @Nested
    class ObjectAssertions {

        @Test
        void isNotNull_passes() {
            assertThatCode(() -> Validate.assertThat("value").isNotNull()).doesNotThrowAnyException();
        }

        @Test
        void isNotNull_fails() {
            assertThatThrownBy(() -> Validate.assertThat((Object) null).isNotNull())
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> {
                        var ex = (JsonApiRequestValidationException) e;
                        assertThat(ex.getErrorCode()).isEqualTo(DefaultErrorCodes.VALUE_IS_ABSENT);
                    });
        }

        @Test
        void isNull_passes() {
            assertThatCode(() -> Validate.assertThat((Object) null).isNull()).doesNotThrowAnyException();
        }

        @Test
        void isNull_fails() {
            assertThatThrownBy(() -> Validate.assertThat("value").isNull())
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> assertThat(((JsonApiRequestValidationException) e).getErrorCode())
                            .isEqualTo(DefaultErrorCodes.VALUE_IS_NOT_ABSENT));
        }

        @Test
        void isEqualTo_passes() {
            assertThatCode(() -> Validate.assertThat("abc").isEqualTo("abc")).doesNotThrowAnyException();
        }

        @Test
        void isEqualTo_fails() {
            assertThatThrownBy(() -> Validate.assertThat("abc").isEqualTo("xyz"))
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> assertThat(((JsonApiRequestValidationException) e).getErrorCode())
                            .isEqualTo(DefaultErrorCodes.VALUE_IS_NOT_EQUAL_TO));
        }

        @Test
        void isIn_passes() {
            assertThatCode(() -> Validate.assertThat("a").isIn("a", "b", "c")).doesNotThrowAnyException();
        }

        @Test
        void isIn_fails() {
            assertThatThrownBy(() -> Validate.assertThat("x").isIn("a", "b"))
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> assertThat(((JsonApiRequestValidationException) e).getErrorCode())
                            .isEqualTo(DefaultErrorCodes.INVALID_ENUM_VALUE));
        }

        @Test
        void isInstanceOf_passes() {
            assertThatCode(() -> Validate.assertThat("text").isInstanceOf(String.class)).doesNotThrowAnyException();
        }

        @Test
        void isInstanceOf_fails() {
            assertThatThrownBy(() -> Validate.assertThat("text").isInstanceOf(Integer.class))
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> assertThat(((JsonApiRequestValidationException) e).getErrorCode())
                            .isEqualTo(DefaultErrorCodes.VALUE_INVALID_TYPE));
        }

        @Test
        void sourceInheritance() {
            assertThatThrownBy(() -> Validate.assertThat((Object) null).withSource(EMAIL_SOURCE).isNotNull())
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> assertThat(((JsonApiRequestValidationException) e).getSource())
                            .isEqualTo(EMAIL_SOURCE));
        }

        @Test
        void withSourceOverride() {
            var overrideSource = ErrorSources.pointer().data().attributes("name");
            assertThatThrownBy(() -> Validate.assertThat((Object) null).withSource(overrideSource).isNotNull())
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> assertThat(((JsonApiRequestValidationException) e).getSource())
                            .isEqualTo(overrideSource));
        }

        @Test
        void withErrorCodeOverride() {
            assertThatThrownBy(() -> Validate.assertThat((Object) null).withErrorCode(DefaultErrorCodes.MISSING_REQUIRED_PARAMETER).isNotNull())
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> assertThat(((JsonApiRequestValidationException) e).getErrorCode())
                            .isEqualTo(DefaultErrorCodes.MISSING_REQUIRED_PARAMETER));
        }

        @Test
        void withDetailOverride() {
            assertThatThrownBy(() -> Validate.assertThat((Object) null).withDetail("custom message").isNotNull())
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> assertThat(((JsonApiRequestValidationException) e).getDetail())
                            .isEqualTo("custom message"));
        }

        @Test
        void withSourceIsPersistent() {
            // withSource sets the source for all subsequent assertions in the chain
            assertThatThrownBy(() -> Validate.assertThat("value").withSource(EMAIL_SOURCE)
                    .isNotNull()  // passes, source persists
                    .isNull())    // fails, should still use EMAIL_SOURCE
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> assertThat(((JsonApiRequestValidationException) e).getSource())
                            .isEqualTo(EMAIL_SOURCE));
        }

        @Test
        void withSourceCanBeOverridden() {
            var newSource = ErrorSources.pointer().data().attributes("name");
            assertThatThrownBy(() -> Validate.assertThat("value").withSource(EMAIL_SOURCE)
                    .isNotNull()                // passes, source = EMAIL_SOURCE
                    .withSource(newSource)       // override to new source
                    .isNull())                   // fails with new source
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> assertThat(((JsonApiRequestValidationException) e).getSource())
                            .isEqualTo(newSource));
        }

        @Test
        void satisfies_passes() {
            assertThatCode(() -> Validate.assertThat("abc").satisfies(v -> {
                if (v == null) throw new JsonApiRequestValidationException("null");
            })).doesNotThrowAnyException();
        }
    }

    // --- String assertions ---

    @Nested
    class StringAssertions {

        @Test
        void isNotBlank_passes() {
            assertThatCode(() -> Validate.assertThat("hello").isNotBlank()).doesNotThrowAnyException();
        }

        @Test
        void isNotBlank_fails() {
            assertThatThrownBy(() -> Validate.assertThat("  ").isNotBlank())
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> assertThat(((JsonApiRequestValidationException) e).getErrorCode())
                            .isEqualTo(DefaultErrorCodes.VALUE_EMPTY));
        }

        @Test
        void hasLengthLessThanOrEqualTo_passes() {
            assertThatCode(() -> Validate.assertThat("abc").hasLengthLessThanOrEqualTo(5)).doesNotThrowAnyException();
        }

        @Test
        void hasLengthLessThanOrEqualTo_fails() {
            assertThatThrownBy(() -> Validate.assertThat("abcdef").hasLengthLessThanOrEqualTo(3))
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> assertThat(((JsonApiRequestValidationException) e).getErrorCode())
                            .isEqualTo(DefaultErrorCodes.VALUE_TOO_LONG));
        }

        @Test
        void hasLengthGreaterThanOrEqualTo_fails() {
            assertThatThrownBy(() -> Validate.assertThat("ab").hasLengthGreaterThanOrEqualTo(5))
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> assertThat(((JsonApiRequestValidationException) e).getErrorCode())
                            .isEqualTo(DefaultErrorCodes.VALUE_TOO_SHORT));
        }

        @Test
        void matches_passes() {
            assertThatCode(() -> Validate.assertThat("abc123").matches("[a-z0-9]+")).doesNotThrowAnyException();
        }

        @Test
        void matches_fails() {
            assertThatThrownBy(() -> Validate.assertThat("abc 123").matches("[a-z0-9]+"))
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> assertThat(((JsonApiRequestValidationException) e).getErrorCode())
                            .isEqualTo(DefaultErrorCodes.VALUE_INVALID_FORMAT));
        }

        @Test
        void isEmail_passes() {
            assertThatCode(() -> Validate.assertThat("test@example.com").isEmail()).doesNotThrowAnyException();
        }

        @Test
        void isEmail_fails() {
            assertThatThrownBy(() -> Validate.assertThat("not-an-email").isEmail())
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> assertThat(((JsonApiRequestValidationException) e).getErrorCode())
                            .isEqualTo(DefaultErrorCodes.VALUE_INVALID_FORMAT));
        }

        @Test
        void isUUID_passes() {
            assertThatCode(() -> Validate.assertThat("550e8400-e29b-41d4-a716-446655440000").isUUID())
                    .doesNotThrowAnyException();
        }

        @Test
        void isUUID_fails() {
            assertThatThrownBy(() -> Validate.assertThat("not-a-uuid").isUUID())
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> assertThat(((JsonApiRequestValidationException) e).getErrorCode())
                            .isEqualTo(DefaultErrorCodes.VALUE_INVALID_FORMAT));
        }

        @Test
        void isOneOf_passes() {
            assertThatCode(() -> Validate.assertThat("users").isOneOf("users", "countries"))
                    .doesNotThrowAnyException();
        }

        @Test
        void isOneOf_caseInsensitive() {
            assertThatCode(() -> Validate.assertThat("USERS").isOneOf("users", "countries"))
                    .doesNotThrowAnyException();
        }

        @Test
        void isOneOf_fails() {
            assertThatThrownBy(() -> Validate.assertThat("wrong").isOneOf("users", "countries"))
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> assertThat(((JsonApiRequestValidationException) e).getErrorCode())
                            .isEqualTo(DefaultErrorCodes.INVALID_ENUM_VALUE));
        }

        @Test
        void contains_passes() {
            assertThatCode(() -> Validate.assertThat("hello world").contains("world")).doesNotThrowAnyException();
        }

        @Test
        void contains_fails() {
            assertThatThrownBy(() -> Validate.assertThat("hello").contains("world"))
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> assertThat(((JsonApiRequestValidationException) e).getErrorCode())
                            .isEqualTo(DefaultErrorCodes.VALUE_INVALID_FORMAT));
        }

        @Test
        void startsWith_passes() {
            assertThatCode(() -> Validate.assertThat("hello").startsWith("hel")).doesNotThrowAnyException();
        }

        @Test
        void chaining_failsOnFirst() {
            assertThatThrownBy(() -> Validate.assertThat("").isNotBlank().isEmail())
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> assertThat(((JsonApiRequestValidationException) e).getErrorCode())
                            .isEqualTo(DefaultErrorCodes.VALUE_EMPTY));
        }

        @Test
        void chaining_passes() {
            assertThatCode(() -> Validate.assertThat("test@example.com")
                    .isNotBlank()
                    .isEmail()
                    .hasLengthLessThanOrEqualTo(100))
                    .doesNotThrowAnyException();
        }
    }

    // --- Number assertions ---

    @Nested
    class NumberAssertions {

        @Test
        void isPositive_passes() {
            assertThatCode(() -> Validate.assertThat(5).isPositive()).doesNotThrowAnyException();
        }

        @Test
        void isPositive_fails() {
            assertThatThrownBy(() -> Validate.assertThat(-1).isPositive())
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> assertThat(((JsonApiRequestValidationException) e).getErrorCode())
                            .isEqualTo(DefaultErrorCodes.VALUE_TOO_LOW));
        }

        @Test
        void isLessThan_passes() {
            assertThatCode(() -> Validate.assertThat(5).isLessThan(10)).doesNotThrowAnyException();
        }

        @Test
        void isLessThan_fails() {
            assertThatThrownBy(() -> Validate.assertThat(10).isLessThan(5))
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> assertThat(((JsonApiRequestValidationException) e).getErrorCode())
                            .isEqualTo(DefaultErrorCodes.VALUE_TOO_HIGH));
        }

        @Test
        void isBetween_passes() {
            assertThatCode(() -> Validate.assertThat(5).isBetween(1, 10)).doesNotThrowAnyException();
        }

        @Test
        void isBetween_fails() {
            assertThatThrownBy(() -> Validate.assertThat(15).isBetween(1, 10))
                    .isInstanceOf(JsonApiRequestValidationException.class);
        }

        @Test
        void longValues_work() {
            assertThatCode(() -> Validate.assertThat(100L).isGreaterThan(50L).isLessThan(200L))
                    .doesNotThrowAnyException();
        }
    }

    // --- Collection assertions ---

    @Nested
    class CollectionAssertions {

        @Test
        void isNotEmpty_passes() {
            assertThatCode(() -> Validate.assertThat(List.of("a")).isNotEmpty()).doesNotThrowAnyException();
        }

        @Test
        void isNotEmpty_fails() {
            assertThatThrownBy(() -> Validate.assertThat(List.of()).isNotEmpty())
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> assertThat(((JsonApiRequestValidationException) e).getErrorCode())
                            .isEqualTo(DefaultErrorCodes.ARRAY_LENGTH_TOO_SHORT));
        }

        @Test
        void hasSizeLessThanOrEqualTo_passes() {
            assertThatCode(() -> Validate.assertThat(List.of("a", "b")).hasSizeLessThanOrEqualTo(5))
                    .doesNotThrowAnyException();
        }

        @Test
        void hasSizeLessThanOrEqualTo_fails() {
            assertThatThrownBy(() -> Validate.assertThat(List.of("a", "b", "c")).hasSizeLessThanOrEqualTo(2))
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> assertThat(((JsonApiRequestValidationException) e).getErrorCode())
                            .isEqualTo(DefaultErrorCodes.ARRAY_LENGTH_TOO_LONG));
        }

        @Test
        void doesNotHaveDuplicates_passes() {
            assertThatCode(() -> Validate.assertThat(List.of("a", "b", "c")).doesNotHaveDuplicates())
                    .doesNotThrowAnyException();
        }

        @Test
        void doesNotHaveDuplicates_fails() {
            assertThatThrownBy(() -> Validate.assertThat(List.of("a", "b", "a")).doesNotHaveDuplicates())
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> assertThat(((JsonApiRequestValidationException) e).getErrorCode())
                            .isEqualTo(DefaultErrorCodes.ARRAY_CONTAINS_DUPLICATES));
        }

        @Test
        void contains_passes() {
            assertThatCode(() -> Validate.assertThat(List.of("a", "b")).contains("a"))
                    .doesNotThrowAnyException();
        }

        @Test
        void contains_fails() {
            assertThatThrownBy(() -> Validate.assertThat(List.of("a", "b")).contains("x"))
                    .isInstanceOf(JsonApiRequestValidationException.class);
        }
    }

    // --- Map assertions ---

    @Nested
    class MapAssertions {

        @Test
        void containsKey_passes() {
            assertThatCode(() -> Validate.assertThat(Map.of("a", "1")).containsKey("a"))
                    .doesNotThrowAnyException();
        }

        @Test
        void containsKey_fails() {
            assertThatThrownBy(() -> Validate.assertThat(Map.of("a", "1")).containsKey("b"))
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> assertThat(((JsonApiRequestValidationException) e).getErrorCode())
                            .isEqualTo(DefaultErrorCodes.MISSING_REQUIRED_PARAMETER));
        }

        @Test
        void doesNotContainKey_passes() {
            assertThatCode(() -> Validate.assertThat(Map.of("a", "1")).doesNotContainKey("b"))
                    .doesNotThrowAnyException();
        }

        @Test
        void doesNotContainKey_fails() {
            assertThatThrownBy(() -> Validate.assertThat(Map.of("a", "1")).doesNotContainKey("a"))
                    .isInstanceOf(JsonApiRequestValidationException.class)
                    .satisfies(e -> assertThat(((JsonApiRequestValidationException) e).getErrorCode())
                            .isEqualTo(DefaultErrorCodes.UNEXPECTED_PARAMETER));
        }

        @Test
        void isNotEmpty_passes() {
            assertThatCode(() -> Validate.assertThat(Map.of("a", "1")).isNotEmpty())
                    .doesNotThrowAnyException();
        }

        @Test
        void isNotEmpty_fails() {
            assertThatThrownBy(() -> Validate.assertThat(Map.of()).isNotEmpty())
                    .isInstanceOf(JsonApiRequestValidationException.class);
        }

        @Test
        void containsEntry_passes() {
            assertThatCode(() -> Validate.assertThat(Map.of("a", "1")).containsEntry("a", "1"))
                    .doesNotThrowAnyException();
        }
    }

}
