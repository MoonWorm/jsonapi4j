package pro.api4.jsonapi4j.processor.util;

import pro.api4.jsonapi4j.operation.exception.OperationNotFoundException;
import pro.api4.jsonapi4j.processor.exception.DataRetrievalException;
import pro.api4.jsonapi4j.processor.exception.InvalidCursorException;
import pro.api4.jsonapi4j.processor.exception.InvalidPayloadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public final class DataRetrievalUtil {

    private static final Logger log = LoggerFactory.getLogger(DataRetrievalUtil.class);

    private DataRetrievalUtil() {

    }

    public static <RESULT> RESULT retrieveDataNullable(Supplier<RESULT> supplier) {
        RESULT result;
        try {
            result = supplier.get();
        } catch (DataRetrievalException | InvalidCursorException | InvalidPayloadException | OperationNotFoundException e) {
            throw e;
        } catch (RuntimeException re) {
            OperationNotFoundException operationNotFoundException = findCause(re, OperationNotFoundException.class);
            if (operationNotFoundException != null) {
                throw operationNotFoundException;
            }

            InvalidCursorException invalidCursorException = findCause(re, InvalidCursorException.class);
            if (invalidCursorException != null) {
                throw invalidCursorException;
            }

            InvalidPayloadException invalidPayloadException = findCause(re, InvalidPayloadException.class);
            if (invalidPayloadException != null) {
                throw invalidPayloadException;
            }

            DataRetrievalException dataRetrievalException = findCause(re, DataRetrievalException.class);
            if (dataRetrievalException != null) {
                throw dataRetrievalException;
            }

            String errMsg = "Unknown error sending/retrieving data from the data source";
            log.error("{}. Error message: {}", errMsg, re.getMessage());
            throw new DataRetrievalException(errMsg, re);
        }
        return result;
    }

    public static <RESULT> RESULT retrieveDataStrict(Supplier<RESULT> supplier) {
        RESULT result = retrieveDataNullable(supplier);
        if (result == null) {
            throw new DataRetrievalException("Data provider returned null");
        }
        return result;
    }

    private static <T extends Throwable> T findCause(Throwable throwable,
                                                     Class<T> targetType) {
        Throwable current = throwable;
        while (current != null) {
            if (targetType.isInstance(current)) {
                return targetType.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }

}
