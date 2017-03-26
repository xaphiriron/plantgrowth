package com.xax.config;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;

import com.google.gson.*;

public class CrossRecipeListDeserializer implements JsonDeserializer<CrossRecipeList> {

    @Override
    public CrossRecipeList deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        ArrayList<CrossRecipe> recipes = new ArrayList<CrossRecipe>();
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(CrossRecipe.class, new CrossRecipeDeserializer())
            .create();

        JsonArray jsonRecipes = json.getAsJsonObject().get("crossbreeding recipes").getAsJsonArray();
        Iterator<JsonElement> iterator = jsonRecipes.iterator();
        while (iterator.hasNext()) {
            recipes.add(gson.fromJson(iterator.next(), CrossRecipe.class));
        }

        return new CrossRecipeList (recipes);
    }

}
