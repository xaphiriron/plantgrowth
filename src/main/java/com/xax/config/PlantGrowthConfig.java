package com.xax.config;

import java.util.ArrayList;

public class PlantGrowthConfig {

    public final int crossbreedingTicks;
    public final ArrayList<CrossRecipe> crossbreedingRecipes;
    public PlantGrowthConfig (int crossbreedingTicks) {
        this(crossbreedingTicks, null);
    }
    public PlantGrowthConfig (int crossbreedingTicks, ArrayList<CrossRecipe> crossbreedingRecipes) {
        this.crossbreedingTicks = crossbreedingTicks;
        this.crossbreedingRecipes = crossbreedingRecipes;
    }
}
