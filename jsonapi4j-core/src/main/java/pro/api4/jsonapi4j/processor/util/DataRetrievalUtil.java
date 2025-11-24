package pro.api4.jsonapi4j.processor.util;

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
        } catch (DataRetrievalException | InvalidCursorException | InvalidPayloadException e) {
            throw e;
        } catch (RuntimeException re) {
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

}
