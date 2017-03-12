package com.xax.plantgrowth.proxy;

import java.util.Arrays;
import java.util.Random;

import org.apache.logging.log4j.Logger;

import com.xax.plantgrowth.Growth;
import com.xax.plantgrowth.INamed;
import com.xax.plantgrowth.MutableBush;
//import com.xax.plantgrowth.MutableItem;
import com.xax.plantgrowth.util.TriConsumer;
import com.xax.plantgrowth.MutableBush.GrowthModifier;
import com.xax.plantgrowth.MutableFood;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;

public abstract class CommonProxy implements IProxy{
    
    public enum Side {
        CLIENT,
        SERVER
    }
    private final boolean runClientHooks;
    
    public CommonProxy(Side side) {
        this.runClientHooks = side == Side.CLIENT ? true : false;
    }
    
    public static MutableBush shockweed;
    public static MutableFood lightningPod;
    
    public void foo(Logger log) {

        lightningPod = this.register(new MutableFood("lightning_pod", 3, 0.6f, false)
            .setFoodCallback(new TriConsumer<ItemStack,World,EntityPlayer>() {
                public void accept(ItemStack itemStack, World worldIn, EntityPlayer player) {
                    Random rand = new Random();
                    if (worldIn.isRaining()
                            && worldIn.canSeeSky(new BlockPos(player.posX, player.posY, player.posZ))
                            && rand.nextInt(2) == 0) {
                        player.addPotionEffect(new PotionEffect(MobEffects.FIRE_RESISTANCE, 600)); // i have no clue how long that is
                        // is this the kind of thing we should do some server-side stuff with??? is the server-side stuff we need to do just a `worldIn.isRemote` check??
                        worldIn.addWeatherEffect(new EntityLightningBolt(worldIn, player.posX, player.posY, player.posZ, false));
                    }
                    return;
                }
            })
            .setCreativeTab(CreativeTabs.FOOD));

        shockweed = this.register(new MutableBush("shockweed_block", 4)
            .setGrowthBlocks(Arrays.asList(Blocks.GRASS, Blocks.DIRT, Blocks.SAND))
            .setGrowthHeight(Arrays.asList (2,4,8,8))
            .setGrowthModifiers(Arrays.asList (GrowthModifier.SPREADS))
            .setSpreadDimensions(2, 1)
            .setSpreadChance(2)
            .setCrop(lightningPod)
            .setCreativeTab(CreativeTabs.DECORATIONS));

    }

    public <T extends Block & INamed> T register(T block) {
        ItemBlock itemBlock = new ItemBlock(block);
        itemBlock.setRegistryName(block.getRegistryName());
        return register(block, itemBlock);
    }

    public <T extends Block & INamed> T register(T block, ItemBlock itemBlock) {
        GameRegistry.register(block);
        GameRegistry.register(itemBlock);

        if(this.runClientHooks) {
            this.registerItemRenderer(itemBlock, 0, block.getName());
        }

        return block;
    }
    
    public <T extends Item & INamed> T register (T item) {
        GameRegistry.register(item);
        if (this.runClientHooks) {
            this.registerItemRenderer(item, 0, item.getName());
        }
        return item;
    }

    public void registerItemRenderer(Item item, int meta, String id) {
        ModelLoader.setCustomModelResourceLocation(item, meta, new ModelResourceLocation(Growth.modId + ":" + id, "inventory"));
    }
}
