package pro.api4.jsonapi4j.domain;

import lombok.Data;

@Data
public class RelationshipName implements Comparable<RelationshipName> {

    private final String name;

    @Override
    public int compareTo(RelationshipName o) {
        return this.name.compareTo(o.getName());
    }
}
