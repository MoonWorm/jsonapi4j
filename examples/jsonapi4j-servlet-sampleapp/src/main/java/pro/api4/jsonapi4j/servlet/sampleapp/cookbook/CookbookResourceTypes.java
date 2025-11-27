package pro.api4.jsonapi4j.servlet.sampleapp.cookbook;

import lombok.AllArgsConstructor;
import lombok.Getter;
import pro.api4.jsonapi4j.domain.ResourceType;

@Getter
@AllArgsConstructor
public enum CookbookResourceTypes implements ResourceType {

    INGREDIENT("ingredients"),
    RECIPE("recipes");

    private final String type;
}
