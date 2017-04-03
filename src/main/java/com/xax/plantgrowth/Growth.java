package com.xax.plantgrowth;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.xax.config.*;
import com.xax.config.CrossReqDeserializer;
import com.xax.config.PlantGrowthConfig;
import com.xax.config.XConfig;
import com.xax.plantgrowth.proxy.CommonProxy;

import net.minecraftforge.common.config.Property;
//import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
//import net.minecraftforge.fml.common.TracingPrintStream;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

@Mod(modid = Growth.modId, version = Growth.version)
public class Growth {
    public static final String modId = "plantgrowth";
    public static final String version = "0.1";

    public static final Logger log = LogManager.getLogger(Growth.modId);
    
    @SidedProxy(modId = Growth.modId, clientSide = "com.xax.plantgrowth.proxy.ClientProxy", serverSide = "com.xax.plantgrowth.proxy.ServerProxy")
    public static CommonProxy proxy;

    private File crossbreedingConfigDir = null;
    
    @EventHandler
    public void PreInit(FMLPreInitializationEvent event){
        File configDir = new File (event.getModConfigurationDirectory(), "plantgrowth/");
        crossbreedingConfigDir = new File (configDir, "crossbreeding/");

        File forgeConfigFile = new File (configDir, "config.cfg");
        XConfig forgeConfig = new XConfig(forgeConfigFile, "0.1", false);
        forgeConfig.load();

        Property crossBlockTicksProp = forgeConfig.get("general", "crossbreeding ticks", 3, "determines the amount of random ticks allowed for crossbreeding checks. like `randomGrowthTicks`.");
        int crossBlocksPerCrossTick = crossBlockTicksProp.getInt(3);
        Property crossTicksProp = forgeConfig.get("general", "crossbreeding frequency", 8, "determines on which ticks the crossbreeding checks occur. 8 = on every 8th tick; 1 = on every tick; etc.");
        int crossTicksPerTick = crossTicksProp.getInt(8);

        if (forgeConfig.hasChanged()) {
            forgeConfig.save();
        }
        // in preinit:
        // 1. load the forge-standard config in *modid*/config.cfg

        // can't do the recipe loading now b/c blocks from other mods might not be loaded
        proxy.preInit(log, new PlantGrowthConfig (crossBlocksPerCrossTick, crossTicksPerTick));
    }

    @EventHandler
    public void load(FMLInitializationEvent event) throws FileNotFoundException {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(CrossReq.class, new CrossReqDeserializer())
                .registerTypeAdapter(CrossPred.class, new CrossPredDeserializer())
                .registerTypeAdapter(CrossRecipe.class, new CrossRecipeDeserializer())
                .registerTypeAdapter(CrossRecipeList.class, new CrossRecipeListDeserializer())
                .create();
        // in init:
        // 1. load all .json files in *modid*/crossbreeding/ and parse them into ArrayLists using CrossRecipeListDeserializer (which should work on the file's entire contents)
        // 2. register all recipe lists
        // make sure the config dir exists
        if (!crossbreedingConfigDir.exists()) {
            if (!crossbreedingConfigDir.mkdirs()) {
                log.error("plantgrowth `crossbreeding/` config dir doesn't exist and couldn't be automatically created!");
                throw new NullPointerException();
            }
        }
        JsonReader reader;
        CrossRecipeList data;
        ArrayList<CrossRecipe> crossRecipes = new ArrayList<CrossRecipe>();
        for (File recipeFile : crossbreedingConfigDir.listFiles()) {
            if (!recipeFile.getName().endsWith(".json")) {
                continue; // ignore files that aren't .json extension
            }
            // FileReader can throw FileNotFoundException, so i guess if the file is deleted after the .listFiles but before it's parsed?
            reader = new JsonReader(new FileReader(recipeFile));
            data = gson.fromJson(reader, CrossRecipeList.class);
            crossRecipes.addAll(data.recipes);
        }
        proxy.init(log, crossRecipes);
    }
    
    @EventHandler
    public void postInit(FMLPostInitializationEvent event){
        log.info("forge postinit");
    }
    
    @EventHandler
    public void onServerStart(FMLServerStartingEvent event){
        log.info("server starting");
    }
}
