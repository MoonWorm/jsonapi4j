package io.jsonapi4j.processor.multi;

import io.jsonapi4j.processor.CursorPageableResponse;
import io.jsonapi4j.processor.exception.DataRetrievalException;

@FunctionalInterface
public interface MultipleDataItemsSupplier<REQUEST, DATA_ITEM_DTO> {

    CursorPageableResponse<DATA_ITEM_DTO> get(REQUEST request) throws DataRetrievalException;

}
