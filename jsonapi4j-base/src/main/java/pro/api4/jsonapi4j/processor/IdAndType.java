package pro.api4.jsonapi4j.processor;

import pro.api4.jsonapi4j.domain.ResourceType;
import lombok.Data;

/**
 * Immutable value object pairing a JSON:API resource {@code "id"} with its {@code "type"}.
 * <p>
 * Used internally by the processing engine to identify relationship targets without
 * carrying the full resource DTO, and as a natural-ordering key (by type then id) for
 * deterministic sorting of included resources in compound documents.
 */
@Data
public class IdAndType implements Comparable<IdAndType> {

    /** The JSON:API {@code "id"} of the resource. */
    private final String id;

    /** The JSON:API {@code "type"} of the resource. */
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
