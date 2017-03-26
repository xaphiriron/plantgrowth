package com.xax.config;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.xax.plantgrowth.MutableBush;
import com.xax.plantgrowth.util.Pair;
import com.xax.plantgrowth.util.TriPredicate;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockReed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CrossRecipeDeserializer implements JsonDeserializer<CrossRecipe> {

    @Override
    public CrossRecipe deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(CrossReq.class, new CrossReqDeserializer())
            .registerTypeAdapter(CrossPred.class, new CrossPredDeserializer())
            .registerTypeAdapter(CrossRecipe.class, new CrossRecipeDeserializer())
            .create();

        JsonObject e = json.getAsJsonObject();
        JsonArray part;
        Iterator<JsonElement> iterator;

        // decode the base block
        Pair<Block,Integer> base = CrossUtils.getBlockFromString(e.get("base").getAsString());

        // decode the result block
        Pair<Block,Integer> result = CrossUtils.getBlockFromString(e.get("result").getAsString());

        // decode the required nearby blocks
        CrossPred canStayAt = null;
        ArrayList<CrossReq> requiresNearby = new ArrayList<CrossReq>();
        part = e.get("nearby").getAsJsonArray();
        iterator = part.iterator();
        while (iterator.hasNext()) {
            CrossReq bar = gson.fromJson(iterator.next(), CrossReq.class);
            if (bar == null) { // null == DEFAULT
                // if DEFAULT isn't the first item:
                if (requiresNearby.size() != 0) {
                    throw new JsonParseException ("can't (well, i don't want to let you) mix DEFAULT predicate with anything else");
                }
                if (result.left instanceof MutableBush) {
                    canStayAt = new CrossPred (CrossUtils.default_((MutableBush)result.left));
                } else if (result.left instanceof BlockBush) {
                    canStayAt = new CrossPred (CrossUtils.default_((BlockBush)result.left));
                } else if (result.left instanceof BlockCactus) {
                    canStayAt = new CrossPred (CrossUtils.default_((BlockCactus)result.left));
                } else if (result.left instanceof BlockReed) {
                    canStayAt = new CrossPred (CrossUtils.default_((BlockReed)result.left));
                } else {
                    throw new JsonParseException ("block " + e.get("result").getAsString() + " doesn't have a default placement predicate! (only values descending from minecraft-default BlockBush, BlockCactus, or BlockReed classes, or the plantgrowth MutableBush class have those)");
                }
                break;
            }
            requiresNearby.add(bar);
        }
        // if DEFAULT wasn't the last item: 
        if (iterator.hasNext()) {
            throw new JsonParseException ("can't (well, i don't want to let you) mix DEFAULT predicate with anything else");
        }
        
        // no defaults so we have to actually do this
        if (canStayAt == null) {
            // decode the stay predicates
            ArrayList<TriPredicate<World,BlockPos,IBlockState>> stayParts = new ArrayList<TriPredicate<World,BlockPos,IBlockState>>();
            iterator = e.get("placementRequires").getAsJsonArray().iterator();
            while (iterator.hasNext()) {
                stayParts.add(gson.fromJson(iterator.next(), CrossPred.class).pred);
            }
            canStayAt = CrossUtils.and(stayParts);
        }
        
        return new CrossRecipe (base, requiresNearby, result, canStayAt);
    }
    /*
     * 
     * 
    { "base":"minecraft:dirt"
    , "nearby":["minecraft:tallgrass"]
    OR
     -   -   - [{block: "minecraft:tallgrass", count: 12}]
    
    , "result":"minecraft:grass"
    , "placementRequires":
        [ TRANSMUTE "minecraft:dirt"
        , HAVINGLIGHTANDAIRABOVE 7
        ]
    }
    */
}
