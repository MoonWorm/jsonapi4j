package pro.api4.jsonapi4j.servlet.sampleapp.cookbook;

import lombok.AllArgsConstructor;
import lombok.Getter;
import pro.api4.jsonapi4j.domain.RelationshipName;

@Getter
@AllArgsConstructor
public enum CookbookResourceRelationshipNames implements RelationshipName {

    RECIPE_INGREDIENTS("recipe-ingredients");

    private final String name;
}
