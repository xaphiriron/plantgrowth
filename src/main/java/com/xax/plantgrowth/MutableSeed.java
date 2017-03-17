package com.xax.plantgrowth;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MutableSeed extends MutableItem implements INamed {

    private IPlantable crops;

    public MutableSeed (String name, IPlantable crops) {
        super(name);

        this.crops = crops;

        this.setCreativeTab(CreativeTabs.MATERIALS);
    }

    @Override
    public EnumActionResult onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        // FIXME: this is hardcoded to only allow planting on the tops of blocks; maybe see if that can be tweaked
        BlockPos planted = pos.up();
        if (facing == EnumFacing.UP
                && player.canPlayerEdit(pos.offset(facing), facing, stack)
                && this.crops.canBlockStay(world, planted, this.crops.getDefaultState())
                && world.isAirBlock(planted))
        {
            world.setBlockState(planted, this.crops.getDefaultState());
            --stack.stackSize;
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.FAIL;
    }
}
