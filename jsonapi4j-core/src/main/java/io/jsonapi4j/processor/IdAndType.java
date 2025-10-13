package io.jsonapi4j.processor;

import io.jsonapi4j.domain.ResourceType;
import lombok.Data;

@Data
public class IdAndType {

    private final String id;
    private final ResourceType type;

}
