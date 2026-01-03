package pro.api4.jsonapi4j.servlet.sampleapp.cookbook.recipe;

import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;

@JsonApiResource(resourceType = "recipes")
public class DishRecipeResource implements Resource<DishRecipe> {

    @Override
    public String resolveResourceId(DishRecipe r) {
        return r.getName();
    }

    @Override
    public DishRecipe resolveAttributes(DishRecipe dataSourceDto) {
        return dataSourceDto;
    }
}
