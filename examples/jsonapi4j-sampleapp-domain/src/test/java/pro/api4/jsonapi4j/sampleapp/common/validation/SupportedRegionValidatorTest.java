package pro.api4.jsonapi4j.sampleapp.common.validation;

import pro.api4.jsonapi4j.sampleapp.operations.country.validation.SupportedRegionValidator;
import pro.api4.jsonapi4j.sampleapp.domain.country.Region;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class SupportedRegionValidatorTest {

    private SupportedRegionValidator sut = new SupportedRegionValidator();

    private static Stream<Arguments> input() {
        return Stream.of(
                Arguments.of(Region.europe.name(), true),
                Arguments.of(Region.asia.name(), true),
                Arguments.of("non-existing", false),
                Arguments.of(null, true)
        );
    }

    @ParameterizedTest
    @MethodSource("input")
    public void testValidator(String region, boolean expectedResult) {
        assertThat(sut.isValid(region, null)).isEqualTo(expectedResult);
    }

}
