package pro.api4.jsonapi4j.servlet.sampleapp.cookbook.recipe;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Data
@AllArgsConstructor
public class DishRecipe {

    @Schema(description = "Name of a dish", example = "Sandwich with cheese", requiredMode = REQUIRED)
    private String name;

    @Schema(description = "Recipe's instructions",
        example = "Take a bread slice and put a butter on it and then put on top a slice of any cheese",
        requiredMode = REQUIRED)
    private List<String> instructions;
}
