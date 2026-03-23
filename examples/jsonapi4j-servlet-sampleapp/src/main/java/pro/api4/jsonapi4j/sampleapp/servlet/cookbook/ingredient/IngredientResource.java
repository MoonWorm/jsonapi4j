package pro.api4.jsonapi4j.sampleapp.servlet.cookbook.ingredient;

import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;

@JsonApiResource(resourceType = "ingredients")
public class IngredientResource implements Resource<Ingredient> {

    @Override
    public String resolveResourceId(Ingredient i) {
        return i.getName();
    }

}
