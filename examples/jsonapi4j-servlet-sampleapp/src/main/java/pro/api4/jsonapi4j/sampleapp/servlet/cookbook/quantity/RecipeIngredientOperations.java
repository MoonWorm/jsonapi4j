package pro.api4.jsonapi4j.sampleapp.servlet.cookbook.quantity;

import pro.api4.jsonapi4j.operation.ReadToManyRelationshipOperation;
import pro.api4.jsonapi4j.operation.annotation.JsonApiRelationshipOperation;
import pro.api4.jsonapi4j.response.CursorPageableResponse;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.sampleapp.servlet.cookbook.ingredient.Ingredient;
import pro.api4.jsonapi4j.sampleapp.servlet.cookbook.recipe.DishRecipe;

import java.util.List;
import java.util.Map;

@JsonApiRelationshipOperation(relationship = RecipeIngredientRelationship.class)
public class RecipeIngredientOperations
        implements ReadToManyRelationshipOperation<DishRecipe, RecipeIngredientRelationship> {

    private Map<String, List<Ingredient>> ingredients;

    @Override
    public CursorPageableResponse<RecipeIngredientRelationship> readMany(JsonApiRequest relationshipRequest) {
        return null;
    }

}
