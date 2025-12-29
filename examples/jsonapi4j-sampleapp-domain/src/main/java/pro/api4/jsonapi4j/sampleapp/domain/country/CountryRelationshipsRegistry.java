package pro.api4.jsonapi4j.sampleapp.domain.country;

import pro.api4.jsonapi4j.domain.RelationshipName;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum CountryRelationshipsRegistry implements RelationshipName {

    COUNTRY_CURRENCIES("currencies");

    private final String relationship;

    @Override
    public String getName() {
        return relationship;
    }

}
