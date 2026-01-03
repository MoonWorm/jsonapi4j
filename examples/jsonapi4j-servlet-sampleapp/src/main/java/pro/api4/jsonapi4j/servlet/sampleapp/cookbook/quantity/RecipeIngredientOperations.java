package pro.api4.jsonapi4j.servlet.sampleapp.cookbook.quantity;

import pro.api4.jsonapi4j.operation.ReadToManyRelationshipOperation;
import pro.api4.jsonapi4j.operation.annotation.JsonApiRelationshipOperation;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.servlet.sampleapp.cookbook.ingredient.Ingredient;
import pro.api4.jsonapi4j.servlet.sampleapp.cookbook.recipe.DishRecipe;
import pro.api4.jsonapi4j.servlet.sampleapp.cookbook.recipe.DishRecipeResource;

import java.util.List;
import java.util.Map;

@JsonApiRelationshipOperation(resource = DishRecipeResource.class, relationship = RecipeIngredientRelationship.class)
public class RecipeIngredientOperations
        implements ReadToManyRelationshipOperation<DishRecipe, RecipeIngredientRelationship> {

    private Map<String, List<Ingredient>> ingredients;

    @Override
    public CursorPageableResponse<RecipeIngredientRelationship> readMany(JsonApiRequest relationshipRequest) {
        return null;
    }

}
