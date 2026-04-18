package pro.api4.jsonapi4j.processor;

import pro.api4.jsonapi4j.domain.ResourceType;
import lombok.Data;

@Data
public class IdAndType implements Comparable<IdAndType> {

    private final String id;
    private final ResourceType type;

    @Override
    public String toString() {
        return String.format("[type=%s, id=%s]", type, id);
    }

    @Override
    public int compareTo(IdAndType o) {
        int typeCompare = this.type.getType().compareTo(o.type.getType());
        if (typeCompare != 0) {
            return typeCompare;
        }
        return this.id.compareTo(o.id);
    }
}
