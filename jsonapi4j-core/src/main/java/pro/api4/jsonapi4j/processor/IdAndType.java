package pro.api4.jsonapi4j.processor;

import pro.api4.jsonapi4j.domain.ResourceType;
import lombok.Data;

@Data
public class IdAndType {

    private final String id;
    private final ResourceType type;

}
