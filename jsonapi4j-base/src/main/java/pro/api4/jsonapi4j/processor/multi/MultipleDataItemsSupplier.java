package pro.api4.jsonapi4j.processor.multi;

import pro.api4.jsonapi4j.response.CursorPageableResponse;
import pro.api4.jsonapi4j.processor.exception.DataRetrievalException;

@FunctionalInterface
public interface MultipleDataItemsSupplier<REQUEST, DATA_ITEM_DTO> {

    CursorPageableResponse<DATA_ITEM_DTO> get(REQUEST request) throws DataRetrievalException;

}
