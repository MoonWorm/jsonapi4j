package pro.api4.jsonapi4j.sampleapp.domain.user;

import pro.api4.jsonapi4j.domain.RelationshipName;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserRelationshipsRegistry implements RelationshipName {

    USER_CITIZENSHIPS("citizenships");

    private final String relationship;

    @Override
    public String getName() {
        return relationship;
    }

}
