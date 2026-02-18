package pro.api4.jsonapi4j.servlet.sampleapp;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.plugin.oas.init.JsonApiOasServletContainerInitializer;
import pro.api4.jsonapi4j.servlet.sampleapp.cookbook.ingredient.IngredientResource;
import pro.api4.jsonapi4j.servlet.sampleapp.cookbook.recipe.DishRecipe;
import pro.api4.jsonapi4j.servlet.sampleapp.cookbook.recipe.DishRecipeResource;
import pro.api4.jsonapi4j.servlet.sampleapp.cookbook.recipe.RecipeOperations;

import java.util.List;
import java.util.concurrent.Executors;

public class CookbookApp {

    private static final List<DishRecipe> recipes = List.of(
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

    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.setInitParameter("jsonapi4j.config", "/jsonapi4j.yaml");

        List<JsonApi4jPlugin> plugins = List.of(
                new JsonApiOasPlugin()
        );

        var domainRegistry = DomainRegistry.builder(plugins)
                .resource(new IngredientResource())
                .resource(new DishRecipeResource())
                .build();

        var operationsRegistry = OperationsRegistry.builder(plugins)
                .operations(new RecipeOperations(recipes))
                .build();

        var jsonApi4j = JsonApi4j.builder()
                .domainRegistry(domainRegistry)
                .operationsRegistry(operationsRegistry)
                .plugins(plugins)
                .executor(Executors.newCachedThreadPool())
                .build();

        context.setAttribute(JsonApi4jServletContainerInitializer.JSONAPI4J_ATT_NAME, jsonApi4j);

        var jsonApi4jInitializer = new JsonApi4jServletContainerInitializer();
        jsonApi4jInitializer.onStartup(null, context.getServletContext());

        var oasPluginInitializer = new JsonApiOasServletContainerInitializer();
        oasPluginInitializer.onStartup(null, context.getServletContext());

        server.setHandler(context);
        server.start();
        server.join();
    }
}
