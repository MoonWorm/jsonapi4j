package pro.api4.jsonapi4j.processor.util;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.operation.exception.OperationNotFoundException;
import pro.api4.jsonapi4j.processor.exception.DataRetrievalException;
import pro.api4.jsonapi4j.processor.exception.InvalidCursorException;
import pro.api4.jsonapi4j.processor.exception.InvalidPayloadException;

import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DataRetrievalUtilTests {

    @Test
    public void operationNotFound_directThrow_isRethrown() {
        OperationNotFoundException expected = new OperationNotFoundException("Operation not found");

        assertThatThrownBy(() -> DataRetrievalUtil.retrieveDataNullable(() -> {
            throw expected;
        })).isSameAs(expected);
    }

    @Test
    public void operationNotFound_nestedCause_isRethrown() {
        OperationNotFoundException expected = new OperationNotFoundException("Operation not found");

        assertThatThrownBy(() -> DataRetrievalUtil.retrieveDataNullable(() -> {
            throw new CompletionException(expected);
        })).isSameAs(expected);
    }

    @Test
    public void invalidCursor_nestedCause_isRethrown() {
        InvalidCursorException expected = new InvalidCursorException("cursor");

        assertThatThrownBy(() -> DataRetrievalUtil.retrieveDataNullable(() -> {
            throw new RuntimeException(new RuntimeException(expected));
        })).isSameAs(expected);
    }

    @Test
    public void invalidPayload_nestedCause_isRethrown() {
        InvalidPayloadException expected = new InvalidPayloadException("bad payload");

        assertThatThrownBy(() -> DataRetrievalUtil.retrieveDataNullable(() -> {
            throw new RuntimeException(expected);
        })).isSameAs(expected);
    }

    @Test
    public void unknownRuntimeException_isWrappedToDataRetrievalException() {
        assertThatThrownBy(() -> DataRetrievalUtil.retrieveDataNullable(() -> {
            throw new RuntimeException("boom");
        })).isInstanceOf(DataRetrievalException.class)
                .hasMessageContaining("Unknown error sending/retrieving data from the data source");
    }
}
