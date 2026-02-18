package pro.api4.jsonapi4j.servlet.sampleapp.cookbook.recipe;

import lombok.AllArgsConstructor;
import pro.api4.jsonapi4j.operation.ResourceOperations;
import pro.api4.jsonapi4j.operation.annotation.JsonApiResourceOperation;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo;
import pro.api4.jsonapi4j.response.CursorPageableResponse;
import pro.api4.jsonapi4j.request.JsonApiRequest;

import java.util.List;

@JsonApiResourceOperation(resource = DishRecipeResource.class)
@AllArgsConstructor
public class RecipeOperations implements ResourceOperations<DishRecipe> {

    private final List<DishRecipe> recipes;

    @OasOperationInfo
    @Override
    public DishRecipe readById(JsonApiRequest request) {
        return recipes.stream().filter(r -> r.getName().equals(request.getResourceId())).findFirst().orElse(null);
    }

    @OasOperationInfo
    @Override
    public CursorPageableResponse<DishRecipe> readPage(JsonApiRequest request) {
        return CursorPageableResponse.fromItemsNotPageable(recipes);
    }

    @Override
    public void validateReadById(JsonApiRequest request) {
        ResourceOperations.super.validateReadById(request);
    }

}
