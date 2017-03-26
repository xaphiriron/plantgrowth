package com.xax.config;

import java.util.ArrayList;
import java.util.List;

import com.xax.plantgrowth.MutableBush;
import com.xax.plantgrowth.util.Pair;
import com.xax.plantgrowth.util.TriPredicate;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;


public class CrossRecipe {

    public final Block base;
    public final int baseMeta;
    public final Block result;
    public final int resultMeta;
    private int size;
    private final int tally[];
    private ArrayList<CrossReq> requirements;
    public CrossPred test;

    // still need a way for the final block to be something other than the default (e.g., specific flower species)
    public CrossRecipe (Block base, List<CrossReq> requirements, final MutableBush result) {
        this(base, requirements, (Block)result, new CrossPred (new TriPredicate<World, BlockPos, IBlockState>() {
            public boolean test (World world, BlockPos pos, IBlockState state) {
                return result.canBlockStay(world, pos, state);
            }
        }));
    }

    public CrossRecipe (Block base, List<CrossReq> requiresNearby, Block result, CrossPred stayFunc) {
        this(new Pair<Block,Integer>(base,-1), requiresNearby, new Pair<Block,Integer>(result,-1), stayFunc);
    }

    public CrossRecipe (Pair<Block,Integer> base, List<CrossReq> requiresNearby, Pair<Block,Integer> result, CrossPred stayFunc) {
        this.base = base.left;
        this.baseMeta = base.right == -1
            ? base.left.getMetaFromState(base.left.getDefaultState())
            : base.right;
        this.result = result.left;
        this.resultMeta = result.right == -1
            ? result.left.getMetaFromState(result.left.getDefaultState())
            : result.right;
        this.size = requiresNearby.size();
        this.tally = new int[requiresNearby.size()];

        this.test = stayFunc;
        this.requirements = new ArrayList<CrossReq>();
        for (CrossReq r : requiresNearby) {
            this.requirements.add(r);
        }

        this.clearTally();
    }

    public boolean satisfied () {
        for (int i = 0; i < this.size; i++) {
            if (this.tally[i] < this.requirements.get(i).quantity) {
                return false;
            }
        }
        return true;
    }

    public void addTally (IBlockState state) {
        CrossReq req;
        for (int i = 0; i < this.size; i++) {
            req = this.requirements.get(i);
            if (req.satisfied(state)) {
                this.tally[i]++;
            }
        }
    }
    
    public void clearTally () {
        for (int i = 0; i < this.size; i++) {
            this.tally[i] = 0;
        }
    }
}
