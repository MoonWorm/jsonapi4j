package io.jsonapi4j.processor.single;

import io.jsonapi4j.processor.exception.DataRetrievalException;

@FunctionalInterface
public interface SingleDataItemSupplier<REQUEST, DATA_ITEM_DTO> {

    DATA_ITEM_DTO get(REQUEST request) throws DataRetrievalException;

}
