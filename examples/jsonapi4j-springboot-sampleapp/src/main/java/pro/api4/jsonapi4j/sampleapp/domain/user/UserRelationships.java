package pro.api4.jsonapi4j.sampleapp.domain.user;

import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import lombok.Data;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;

@Data
public class UserRelationships {

    private ToManyRelationshipsDoc citizenships;
    private ToOneRelationshipDoc placeOfBirth;
    private ToManyRelationshipsDoc relatives;

    public UserRelationships(ToManyRelationshipsDoc citizenships,
                             ToOneRelationshipDoc placeOfBirth,
                             ToManyRelationshipsDoc relatives) {
        this.citizenships = citizenships;
        this.placeOfBirth = placeOfBirth;
        this.relatives = relatives;
    }

}
