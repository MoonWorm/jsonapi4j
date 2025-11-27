package pro.api4.jsonapi4j.servlet.sampleapp.cookbook.ingredient;

import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.servlet.sampleapp.cookbook.CookbookResourceTypes;

public class IngredientResource implements Resource<Ingredient> {

    @Override
    public String resolveResourceId(Ingredient i) {
        return i.getName();
    }

    @Override
    public ResourceType resourceType() {
        return CookbookResourceTypes.INGREDIENT;
    }
}
