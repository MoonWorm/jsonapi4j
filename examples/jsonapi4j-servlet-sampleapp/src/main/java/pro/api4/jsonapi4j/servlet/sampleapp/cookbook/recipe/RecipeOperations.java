package pro.api4.jsonapi4j.servlet.sampleapp.cookbook.recipe;

import lombok.AllArgsConstructor;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.operation.ResourceRepository;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.servlet.sampleapp.cookbook.CookbookResourceTypes;

import java.util.List;

@AllArgsConstructor
public class RecipeOperations implements ResourceRepository<DishRecipe>
{

    private final List<DishRecipe> recipes;

    @Override
    public DishRecipe readById(JsonApiRequest request) {
        return recipes.stream().filter(r -> r.getName().equals(request.getResourceId())).findFirst().orElse(null);
    }

    @Override
    public CursorPageableResponse<DishRecipe> readPage(JsonApiRequest request) {
        return CursorPageableResponse.fromItemsNotPageable(recipes);
    }

    @Override
    public void validateReadById(JsonApiRequest request) {
        ResourceRepository.super.validateReadById(request);
    }

    @Override
    public ResourceType resourceType() {
        return CookbookResourceTypes.RECIPE;
    }

}
