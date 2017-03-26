package com.xax.config;

import java.lang.reflect.Type;
import java.util.function.Predicate;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.xax.plantgrowth.util.Pair;

import net.minecraft.block.Block;

public class CrossReqDeserializer implements JsonDeserializer<CrossReq> {

    public CrossReqDeserializer() {
    }

    public CrossReq deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        Block block;
        final int metadata;
        int count = 1;
        Predicate<Integer> metaFunc;

        Pair<Block, Integer> p;
        CrossReq req;

        if (json.isJsonObject()) {
            JsonObject e = json.getAsJsonObject();
            p = CrossUtils.getBlockFromString(e.get("block").getAsString());
            count = Integer.parseInt(e.get("count").getAsString());
            block = p.left;
            metadata = p.right;
        } else { // assume the json is a string
            p = CrossUtils.getBlockFromString(json.getAsString());
            block = p.left;
            metadata = p.right;
        }

        if (metadata != -1) {
            metaFunc = new Predicate<Integer>() {
                public boolean test(Integer meta) {
                    return meta == metadata;
                }
            };
            req = new CrossReq (block, metaFunc, count);
        } else {
            req = new CrossReq (block, count);
        }
        return req;
    }

}
