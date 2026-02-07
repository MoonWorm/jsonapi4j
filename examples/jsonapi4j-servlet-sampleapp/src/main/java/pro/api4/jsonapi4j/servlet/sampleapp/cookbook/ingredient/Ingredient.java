package pro.api4.jsonapi4j.servlet.sampleapp.cookbook.ingredient;

import lombok.Data;

@Data
public class Ingredient {

    private String name;
    private String description;
    private boolean allergen;
}
