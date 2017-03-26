package com.xax.config;

import java.util.ArrayList;

/**
 * this only exists because java deals very poorly with getting the .class
 * values of generic types. this turns `ArrayList<CrossRecipe>` into a
 * concrete type with a .class, so that CrossRecipeListDeserializer can decode
 * things into lists of CropRecipes without making a huge deal about it.
 * @author xax
 *
 */
public class CrossRecipeList {
    public final ArrayList<CrossRecipe> recipes;
    public CrossRecipeList (ArrayList<CrossRecipe> recipes) {
        this.recipes = recipes;
    }
}
