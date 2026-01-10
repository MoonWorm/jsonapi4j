package pro.api4.jsonapi4j.sampleapp.config.datasource.model.user;


import lombok.Data;

@Data
public class UserRelationshipInfo {

    private final String relativeUserId;
    private final RelationshipType relationshipType;

    public enum RelationshipType {
        HUSBAND, WIFE, SON, DAUGHTER, MOTHER, FATHER, BROTHER;
    }
}
