package pro.api4.jsonapi4j.operation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OperationsRegistryTests {

    @Test
    public void empty_checkAllMethodsWorksAsExpected() {
        // given - when
        OperationsRegistry sut = OperationsRegistry.empty();

        // then
        assertThat(sut.getAllOperations()).isNotNull().isEmpty();
    }

}
