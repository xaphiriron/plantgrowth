package com.xax.plantgrowth.proxy;

import java.util.Arrays;
import java.util.Random;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.Logger;

import com.xax.hook.EventHook;
import com.xax.plantgrowth.Growth;
import com.xax.plantgrowth.INamed;
import com.xax.plantgrowth.MutableBush;
import com.xax.plantgrowth.util.QuadPredicate;
//import com.xax.plantgrowth.MutableItem;
import com.xax.plantgrowth.util.TriConsumer;
import com.xax.plantgrowth.MutableBush.GrowthModifier;
import com.xax.plantgrowth.MutableBush.Witherable;
import com.xax.plantgrowth.MutableBush.WitheringAction;
import com.xax.plantgrowth.MutableFood;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
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
import net.minecraftforge.common.MinecraftForge;
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
    
    public void preInit(Logger log) {
        log.info("forge preinit");
        EventHook tickHandler = new EventHook();
        MinecraftForge.EVENT_BUS.register(tickHandler);

        lightningPod = this.register(new MutableFood("lightning_pod", 3, 0.6f, false)
            .setFoodCallback(new TriConsumer<ItemStack,World,EntityPlayer>() {
                public void accept(ItemStack itemStack, World world, EntityPlayer player) {
                    player.addPotionEffect(new PotionEffect(MobEffects.FIRE_RESISTANCE, 600)); // i think this is 30 seconds
                    if (world.isRaining()
                            && world.canSeeSky(new BlockPos(player.posX, player.posY, player.posZ))
                            && world.rand.nextInt(2) == 0) {
                        // is this the kind of thing we should do some server-side stuff with??? is the server-side stuff we need to do just a `worldIn.isRemote` check??
                        world.addWeatherEffect(new EntityLightningBolt(world, player.posX, player.posY, player.posZ, false));
                    }
                    return;
                }
            })
            .setCreativeTab(CreativeTabs.FOOD));

        shockweed = this.register(new MutableBush("shockweed_block", 4, 0, Witherable.WITHERABLE)
            .setGrowthBlocks(Arrays.asList(Blocks.GRASS, Blocks.DIRT, Blocks.SAND))
            .setGrowthHeight(Arrays.asList (2,4,8,8))
            .setGrowthModifiers(Arrays.asList (GrowthModifier.SPREADS, GrowthModifier.LIKES_RAIN))
            .setSpreadDimensions(2, 1)
            .setSpreadChance(2)
            .withersWhen(new QuadPredicate<World, BlockPos, IBlockState, Random>(){
                public boolean test(World world, BlockPos pos, IBlockState state, Random rand) {
                    return !world.isRainingAt(pos);
                }
            })
            .witherAction (WitheringAction.REVERSE_GROWTH)
            .setCrop(lightningPod)
            .setCreativeTab(CreativeTabs.DECORATIONS));

        log.info("blocks created and registered");

        // try to plant shockweed on lightning strikes, at 25% chance
        tickHandler.attachLightningStrikeHandler (new BiConsumer<World,EntityLightningBolt>() {
            public void accept(World world, EntityLightningBolt bolt) {
                //System.out.println("in lightning handler");
                if(world.rand.nextInt(4) == 0) {
                    BlockPos hit = new BlockPos(bolt.posX, bolt.posY, bolt.posZ);
                    BlockPos supporting = hit.down();
                    //System.out.println("hit: " + hit + " (" + world.getBlockState(hit).getBlock() + "); supporting: " + supporting + " (" + world.getBlockState(supporting).getBlock() + ")");
                    //System.out.println("shockweed can stay?: " + (shockweed.canBlockStay(world, supporting, world.getBlockState (supporting))) + "; hit block is air?: " + (world.isAirBlock(hit)));
                    if(shockweed.canBlockStay(world, supporting, world.getBlockState (supporting))
                            && (world.isAirBlock(hit) || world.getBlockState(hit).getBlock() == Blocks.FIRE)) {
                        world.setBlockState(hit, shockweed.getDefaultState(), 2);
                    }
                }
            }
        });
        
        log.info("event handlers attached");
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

    private void registerItemRenderer(Item item, int meta, String name) {
        ModelLoader.setCustomModelResourceLocation(item, meta, new ModelResourceLocation(Growth.modId + ":" + name, "inventory"));
    }
}
