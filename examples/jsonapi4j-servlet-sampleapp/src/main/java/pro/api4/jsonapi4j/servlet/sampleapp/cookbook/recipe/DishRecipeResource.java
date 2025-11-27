package pro.api4.jsonapi4j.servlet.sampleapp.cookbook.recipe;

import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.servlet.sampleapp.cookbook.CookbookResourceTypes;

public class DishRecipeResource implements Resource<DishRecipe> {

    @Override
    public String resolveResourceId(DishRecipe r) {
        return r.getName();
    }

    @Override
    public ResourceType resourceType() {
        return CookbookResourceTypes.RECIPE;
    }

    @Override
    public DishRecipe resolveAttributes(DishRecipe dataSourceDto) {
        return dataSourceDto;
    }
}
