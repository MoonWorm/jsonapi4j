package pro.api4.jsonapi4j.operation.validation;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.exception.ConstraintViolationException;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JsonApi4jDefaultValidatorTests {

    private static final String PARAMETER_NAME = "testParam";

    private final JsonApi4jDefaultValidator validator = new JsonApi4jDefaultValidator();

    // --- validateNonNull ---

    @Test
    public void validateNonNull_objectIsNull_throwsException() {
        // when / then
        assertThatThrownBy(() -> validator.validateNonNull(null, PARAMETER_NAME))
                .isInstanceOf(ConstraintViolationException.class)
                .extracting("detail", "parameter")
                .containsExactly("value can't be null", PARAMETER_NAME);
    }

    @Test
    public void validateNonNull_objectIsNotNull_noException() {
        // given
        Object object = "non-null";

        // when / then
        assertThatNoException().isThrownBy(() -> validator.validateNonNull(object, PARAMETER_NAME));
    }

    // --- validateResourceId ---

    @Test
    public void validateResourceId_blankId_throwsException() {
        // when / then
        assertThatThrownBy(() -> validator.validateResourceId(""))
                .isInstanceOf(ConstraintViolationException.class)
                .extracting("detail")
                .isEqualTo("resource id can't be blank");
    }

    @Test
    public void validateResourceId_nullId_throwsException() {
        // when / then
        assertThatThrownBy(() -> validator.validateResourceId(null))
                .isInstanceOf(ConstraintViolationException.class)
                .extracting("detail")
                .isEqualTo("resource id can't be blank");
    }

    @Test
    public void validateResourceId_tooLongId_throwsException() {
        // given
        String tooLongId = "a".repeat(JsonApi4jDefaultValidator.RESOURCE_ID_MAX_LENGTH + 1);

        // when / then
        assertThatThrownBy(() -> validator.validateResourceId(tooLongId))
                .isInstanceOf(ConstraintViolationException.class)
                .extracting("detail")
                .isEqualTo("resource id length can't be more than " + JsonApi4jDefaultValidator.RESOURCE_ID_MAX_LENGTH);
    }

    @Test
    public void validateResourceId_validId_noException() {
        // when / then
        assertThatNoException().isThrownBy(() -> validator.validateResourceId("abc-123"));
    }

    @Test
    public void validateResourceId_maxLengthId_noException() {
        // given
        String maxLengthId = "a".repeat(JsonApi4jDefaultValidator.RESOURCE_ID_MAX_LENGTH);

        // when / then
        assertThatNoException().isThrownBy(() -> validator.validateResourceId(maxLengthId));
    }

    // --- validateFilterByIds ---

    @Test
    public void validateFilterByIds_nullList_noException() {
        // when / then
        assertThatNoException().isThrownBy(() -> validator.validateFilterByIds(null));
    }

    @Test
    public void validateFilterByIds_emptyList_noException() {
        // when / then
        assertThatNoException().isThrownBy(() -> validator.validateFilterByIds(List.of()));
    }

    @Test
    public void validateFilterByIds_oversizedList_throwsException() {
        // given
        List<String> oversizedList = IntStream.rangeClosed(1, JsonApi4jDefaultValidator.MAX_ELEMENTS_IN_FILTER_PARAM + 1)
                .mapToObj(i -> "id-" + i)
                .toList();

        // when / then
        assertThatThrownBy(() -> validator.validateFilterByIds(oversizedList))
                .isInstanceOf(ConstraintViolationException.class)
                .extracting("detail")
                .isEqualTo("max elements number exceeded: " + JsonApi4jDefaultValidator.MAX_ELEMENTS_IN_FILTER_PARAM);
    }

    @Test
    public void validateFilterByIds_listWithBlankId_throwsException() {
        // given
        List<String> listWithBlank = List.of("valid-id", "");

        // when / then
        assertThatThrownBy(() -> validator.validateFilterByIds(listWithBlank))
                .isInstanceOf(ConstraintViolationException.class)
                .extracting("detail")
                .isEqualTo("resource id can't be blank");
    }

    @Test
    public void validateFilterByIds_validList_noException() {
        // given
        List<String> validList = List.of("id1", "id2");

        // when / then
        assertThatNoException().isThrownBy(() -> validator.validateFilterByIds(validList));
    }

}
