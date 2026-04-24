package pro.api4.jsonapi4j.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RelationshipName implements Comparable<RelationshipName> {

    private final String name;

    @Override
    public int compareTo(RelationshipName o) {
        return String.CASE_INSENSITIVE_ORDER.compare(this.name, o.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelationshipName that = (RelationshipName) o;
        return name != null ? name.equalsIgnoreCase(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.toLowerCase().hashCode() : 0;
    }

    @Override
    public String toString() {
        return name;
    }

}
