package pro.api4.jsonapi4j.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ResourceType implements Comparable<ResourceType> {

    private final String type;

    @Override
    public int compareTo(ResourceType o) {
        return String.CASE_INSENSITIVE_ORDER.compare(this.type, o.getType());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceType that = (ResourceType) o;
        return type != null ? type.equalsIgnoreCase(that.type) : that.type == null;
    }

    @Override
    public int hashCode() {
        return type != null ? type.toLowerCase().hashCode() : 0;
    }

    @Override
    public String toString() {
        return type;
    }

}
