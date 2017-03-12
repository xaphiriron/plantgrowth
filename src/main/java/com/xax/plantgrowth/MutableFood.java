package com.xax.plantgrowth;

import com.xax.plantgrowth.util.TriConsumer;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class MutableFood extends ItemFood implements INamed {

    protected String name;
    
    private TriConsumer<ItemStack,World,EntityPlayer> foodCallback;

    public MutableFood(String name, int heal, float saturation, boolean isWolfFood) {
        super(heal, saturation, isWolfFood);
        this.name = name;
        setUnlocalizedName(name);
        setRegistryName(name);
        
        this.foodCallback = null;
    }
    
    public MutableFood setFoodCallback (TriConsumer<ItemStack,World,EntityPlayer> foodCallback) {
        this.foodCallback = foodCallback;
        return this;
    }
    public String getName() {
        return this.name;
    }

    @Override
    protected void onFoodEaten(ItemStack stack, World worldIn, EntityPlayer player) {
        super.onFoodEaten(stack, worldIn, player);
        if (this.foodCallback != null) {
            this.foodCallback.accept (stack, worldIn, player);
        }
    }

    @Override
    public MutableFood setCreativeTab(CreativeTabs tab) {
        super.setCreativeTab(tab);
        return this;
    }
}
