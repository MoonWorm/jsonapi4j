package pro.api4.jsonapi4j.sampleapp.domain.user;

import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import lombok.Data;

@Data
public class UserRelationships {

    private ToManyRelationshipsDoc citizenships;

    public UserRelationships(ToManyRelationshipsDoc citizenships) {
        this.citizenships = citizenships;
    }
}
