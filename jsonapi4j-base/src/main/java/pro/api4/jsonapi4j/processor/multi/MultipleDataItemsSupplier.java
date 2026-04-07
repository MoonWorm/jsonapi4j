package pro.api4.jsonapi4j.processor.multi;

import pro.api4.jsonapi4j.response.PaginationAwareResponse;
import pro.api4.jsonapi4j.processor.exception.DataRetrievalException;

@FunctionalInterface
public interface MultipleDataItemsSupplier<REQUEST, DATA_ITEM_DTO> {

    PaginationAwareResponse<DATA_ITEM_DTO> get(REQUEST request) throws DataRetrievalException;

}
