package pro.api4.jsonapi4j.processor;

import lombok.Data;
import pro.api4.jsonapi4j.plugin.ac.impl.AnonymizationResult;

@Data
public class ProcessingItem<DATA_SOURCE_DTO, RESOURCE> {
    private final DATA_SOURCE_DTO resourceDto;
    private final RESOURCE resource;
    private AnonymizationResult<RESOURCE> anonymizationResult;
}
