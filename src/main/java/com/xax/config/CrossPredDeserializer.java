package com.xax.config;

import java.lang.reflect.Type;
import java.util.HashSet;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.xax.plantgrowth.util.Pair;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public class CrossPredDeserializer implements JsonDeserializer<CrossPred>{

    /*
     * would be some token like:
    "TRANSMUTE minecraft:dirt"
    "TRANSMUTE minecraft:sand minecraft:gravel"
    "HAVINGLIGHTANDAIRABOVE 7"
    "REPLACESMATERIAL AIR WATER"
    "REPLACESMATERIAL WATER"
    "PLACEDON minecraft:dirt minecraft:sand minecraft:grass"
    "DEFAULT"
     */
    
    @Override
    public CrossPred deserialize(JsonElement json, Type typeOfT,
            JsonDeserializationContext context) throws JsonParseException {
        String raws[] = json.getAsString().split(" ");
        switch (raws[0]) {
        case "DEFAULT":
            if (raws.length == 1) {
                return new CrossPred (null); // this null is checked in CrossRecipeDeserializer, once we have the result block data and can actually figure out if default is valid / what it would be
            }
        case "TRANSMUTE":
            if (raws.length == 2) {
                Pair<Block,Integer> transmuted = CrossUtils.getBlockFromString(raws[1]);
                return new CrossPred (CrossUtils.transmute(transmuted.left, transmuted.right));
            } else if (raws.length > 2) {
                HashSet<Pair<Block,Integer>> transmutedBlocks = new HashSet<Pair<Block,Integer>>();
                raws[0] = "";
                for (String blockName : raws) {
                    if (blockName == "") {
                        continue;
                    }
                    transmutedBlocks.add(CrossUtils.getBlockFromString(blockName));
                }
                return new CrossPred (CrossUtils.transmute(transmutedBlocks));
            }
            break;
        case "REPLACESMATERIAL":
            if (raws.length == 2) {
                Material mat = CrossUtils.getMaterialFromString(raws[1]);
                return new CrossPred (CrossUtils.replacesMaterial(mat));
            } else if (raws.length > 2) {
                HashSet<Material> mats = new HashSet<Material>();
                raws[0] = "";
                for (String materialName : raws) {
                    if (materialName == "") {
                        continue;
                    }
                    mats.add(CrossUtils.getMaterialFromString(materialName));
                }
                return new CrossPred (CrossUtils.replacesMaterial(mats));
            }
        case "PLACEDON":
            if (raws.length == 2) {
                Pair<Block,Integer> pair = CrossUtils.getBlockFromString(raws[1]);
                return new CrossPred (CrossUtils.placedOn(pair.left, pair.right));
            } else if (raws.length > 2) {
                HashSet<Pair<Block,Integer>> supportingBlocks = new HashSet<Pair<Block,Integer>>();
                raws[0] = "";
                for (String blockName : raws) {
                    if (blockName == "") {
                        continue;
                    }
                    supportingBlocks.add(CrossUtils.getBlockFromString(blockName));
                }
                return new CrossPred (CrossUtils.placedOn(supportingBlocks));
            }
        case "PLACEDAGAINST": {
            if (raws.length == 2) {
                Pair<Block,Integer> pair = CrossUtils.getBlockFromString(raws[1]);
                return new CrossPred (CrossUtils.placedAgainst(pair.left, pair.right));
            } else if (raws.length > 2) {
                HashSet<Pair<Block,Integer>> adjacentBlocks = new HashSet<Pair<Block,Integer>>();
                raws[0] = "";
                for (String blockName : raws) {
                    if (blockName == "") {
                        continue;
                    }
                    adjacentBlocks.add(CrossUtils.getBlockFromString(blockName));
                }
                return new CrossPred (CrossUtils.placedAgainst(adjacentBlocks));
            }
        }
        case "HAVINGLIGHTANDAIRABOVE":
            if (raws.length == 2) {
                return new CrossPred (CrossUtils.havingLightAndAirAbove(Integer.parseInt(raws[1])));
            }
            break;
        default:
            throw new JsonParseException("unknown predicate (" + raws[0] + ") in plantgrowth config file");
        }
        throw new JsonParseException("incorrect number of args for " + raws[0] + " in plantgrowth config file");
    }

}

