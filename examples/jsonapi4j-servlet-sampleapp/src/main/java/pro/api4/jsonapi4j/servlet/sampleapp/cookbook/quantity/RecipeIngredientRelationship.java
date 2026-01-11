package pro.api4.jsonapi4j.servlet.sampleapp.cookbook.quantity;

import pro.api4.jsonapi4j.domain.ToManyRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.servlet.sampleapp.cookbook.ingredient.Ingredient;
import pro.api4.jsonapi4j.servlet.sampleapp.cookbook.recipe.DishRecipeResource;

@JsonApiRelationship(relationshipName = "recipe-ingredients", parentResource = DishRecipeResource.class)
public class RecipeIngredientRelationship implements ToManyRelationship<Ingredient> {

    @Override
    public String resolveResourceIdentifierType(Ingredient ingredient) {
        return "ingredients";
    }

    @Override
    public String resolveResourceIdentifierId(Ingredient ingredient) {
        return ingredient.getName();
    }

    @Override
    public IngredientQuantity resolveResourceIdentifierMeta(JsonApiRequest relationshipRequest, Ingredient ingredient) {
        return new IngredientQuantity("1 spoon");
    }
}
