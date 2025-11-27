package pro.api4.jsonapi4j.servlet.sampleapp.cookbook.quantity;

import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.domain.ToManyRelationship;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.servlet.sampleapp.cookbook.CookbookResourceRelationshipNames;
import pro.api4.jsonapi4j.servlet.sampleapp.cookbook.CookbookResourceTypes;
import pro.api4.jsonapi4j.servlet.sampleapp.cookbook.ingredient.Ingredient;
import pro.api4.jsonapi4j.servlet.sampleapp.cookbook.recipe.DishRecipe;

public class RecipeIngredientRelationship implements ToManyRelationship<DishRecipe, Ingredient> {

    @Override
    public RelationshipName relationshipName() {
        return CookbookResourceRelationshipNames.RECIPE_INGREDIENTS;
    }

    @Override
    public ResourceType parentResourceType() {
        return CookbookResourceTypes.RECIPE;
    }

    @Override
    public ResourceType resolveResourceIdentifierType(Ingredient ingredient) {
        return CookbookResourceTypes.INGREDIENT;
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
