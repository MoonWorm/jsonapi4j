package pro.api4.jsonapi4j.domain;

import lombok.Data;

@Data
public class ResourceType implements Comparable<ResourceType> {

    private final String type;

    @Override
    public int compareTo(ResourceType o) {
        return this.type.compareTo(o.getType());
    }
}
