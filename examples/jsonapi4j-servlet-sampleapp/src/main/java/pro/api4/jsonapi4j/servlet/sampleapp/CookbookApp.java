package pro.api4.jsonapi4j.servlet.sampleapp;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.servlet.sampleapp.cookbook.ingredient.IngredientResource;
import pro.api4.jsonapi4j.servlet.sampleapp.cookbook.recipe.DishRecipe;
import pro.api4.jsonapi4j.servlet.sampleapp.cookbook.recipe.DishRecipeResource;
import pro.api4.jsonapi4j.servlet.sampleapp.cookbook.recipe.RecipeOperations;

import java.util.List;
import java.util.Set;

public class CookbookApp {

    private static final Logger log = LoggerFactory.getLogger(CookbookApp.class);

    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.setInitParameter("jsonapi4j.config", "/jsonapi4j.yaml");

        var recipes = List.of(
            new DishRecipe(
                "cheese-sandwich",
                List.of("Spread evenly butter on bread", "Put slice of cheese on top")
            ),
            new DishRecipe(
                "pelmeni",
                List.of(
                    "Boil water in a pot",
                    "Add pelmeni into boiling water",
                    "Add salt",
                    "Cook for 10 minutes",
                    "Serve pelmeni with butter or smetana"
                )
            )
        );
        var domainRegistry = DomainRegistry.builder()
                .resource(new IngredientResource())
                .resource(new DishRecipeResource())
                .build();

        var recipeOperations = new RecipeOperations(recipes);

        var operationsRegistry = OperationsRegistry.builder().operation(recipeOperations).build();
        context.setAttribute(JsonApi4jServletContainerInitializer.DOMAIN_REGISTRY_ATT_NAME, domainRegistry);
        context.setAttribute(JsonApi4jServletContainerInitializer.OPERATION_REGISTRY_ATT_NAME, operationsRegistry);

        var initializer = new JsonApi4jServletContainerInitializer();
        initializer.onStartup(null, context.getServletContext());

        server.setHandler(context);
        server.start();
        server.join();
    }
}
