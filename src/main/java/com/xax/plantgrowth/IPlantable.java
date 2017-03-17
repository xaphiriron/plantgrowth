package com.xax.plantgrowth;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

// this is kind of a hack, especially since the only thing that implements this is MutableBush, so i might as well have just said MutableSeeds require a MutableBush crop
public interface IPlantable {
    public boolean canBlockStay(World world, BlockPos pos, IBlockState state);

    public IBlockState getDefaultState();
}
