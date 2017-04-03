package com.xax.config;

import java.util.ArrayList;

public class PlantGrowthConfig {

    public final int crossbreedingTicks;
    public final int crossbreedingFrequency;
    public final ArrayList<CrossRecipe> crossbreedingRecipes;
    public PlantGrowthConfig (int crossbreedingTicks, int crossFrequency) {
        this(crossbreedingTicks, crossFrequency, null);
    }
    public PlantGrowthConfig (int crossbreedingTicks, int crossFrequency, ArrayList<CrossRecipe> crossbreedingRecipes) {
        this.crossbreedingTicks = crossbreedingTicks;
        this.crossbreedingFrequency = crossFrequency;

        this.crossbreedingRecipes = crossbreedingRecipes;
    }
}
