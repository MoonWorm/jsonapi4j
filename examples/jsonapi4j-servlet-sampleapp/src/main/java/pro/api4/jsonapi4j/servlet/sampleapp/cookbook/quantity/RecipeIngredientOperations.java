package pro.api4.jsonapi4j.servlet.sampleapp.cookbook.quantity;

import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.operation.ReadToManyRelationshipOperation;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.servlet.sampleapp.cookbook.CookbookResourceRelationshipNames;
import pro.api4.jsonapi4j.servlet.sampleapp.cookbook.CookbookResourceTypes;
import pro.api4.jsonapi4j.servlet.sampleapp.cookbook.ingredient.Ingredient;
import pro.api4.jsonapi4j.servlet.sampleapp.cookbook.recipe.DishRecipe;

import java.util.List;
import java.util.Map;

public class RecipeIngredientOperations
    implements ReadToManyRelationshipOperation<DishRecipe, RecipeIngredientRelationship>
{

    private Map<String, List<Ingredient>> ingredients;

    @Override
    public CursorPageableResponse<RecipeIngredientRelationship> readMany(JsonApiRequest relationshipRequest) {
        return null;
    }

    @Override
    public RelationshipName relationshipName() {
        return CookbookResourceRelationshipNames.RECIPE_INGREDIENTS;
    }

    @Override
    public ResourceType resourceType() {
        return CookbookResourceTypes.RECIPE;
    }
}
