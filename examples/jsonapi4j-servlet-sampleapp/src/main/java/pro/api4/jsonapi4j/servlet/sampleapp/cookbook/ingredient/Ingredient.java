package pro.api4.jsonapi4j.servlet.sampleapp.cookbook.ingredient;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Data
public class Ingredient {

    @Schema(description = "Unique name of an ingredient", example = "Rye bread", requiredMode = REQUIRED)
    private String name;

    @Schema(description = "Description of an ingredient", example = "Bread made from rye", requiredMode = NOT_REQUIRED)
    private String description;

    @Schema(description = "Is ingredient a known allergen?", example = "true", requiredMode = REQUIRED)
    private boolean allergen;
}
