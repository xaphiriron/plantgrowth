package com.xax.config;

import com.xax.plantgrowth.util.TriPredicate;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CrossPred {
    public final TriPredicate<World,BlockPos,IBlockState> pred;
    public CrossPred (TriPredicate<World,BlockPos,IBlockState> pred) {
        this.pred = pred;
    }
}
