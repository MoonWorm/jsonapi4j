package pro.api4.jsonapi4j.servlet.sampleapp.cookbook.recipe;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DishRecipe {

    private String name;

    private List<String> instructions;
}
