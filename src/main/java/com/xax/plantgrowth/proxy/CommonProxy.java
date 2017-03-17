package com.xax.plantgrowth.proxy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.Logger;

import com.xax.hook.EventHook;
import com.xax.plantgrowth.Growth;
import com.xax.plantgrowth.INamed;
import com.xax.plantgrowth.MutableBush;
import com.xax.plantgrowth.util.QuadFunction;
import com.xax.plantgrowth.util.QuadPredicate;
import com.xax.plantgrowth.util.QuintConsumer;
//import com.xax.plantgrowth.MutableItem;
import com.xax.plantgrowth.util.TriConsumer;
import com.xax.plantgrowth.MutableBush.GrowthModifier;
import com.xax.plantgrowth.MutableBush.Witherable;
import com.xax.plantgrowth.MutableBush.WitheringAction;
import com.xax.plantgrowth.MutableFood;
import com.xax.plantgrowth.MutableItem;
import com.xax.plantgrowth.MutableSeed;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
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
    public static MutableSeed moonflowerSeed;
    public static MutableBush moonflowerPlant;
    public static MutableBush moonflowerBlossom;
    public static MutableItem moonflowerPetal;
    
    public void preInit(Logger log) {
        log.info("forge preinit");
        EventHook tickHandler = new EventHook();
        MinecraftForge.EVENT_BUS.register(tickHandler);

        lightningPod = new MutableFood("lightning_pod", 3, 0.6f, false);
        shockweed = new MutableBush("shockweed_block", 4, 0, Witherable.WITHERABLE);

        moonflowerPetal = new MutableItem("moonflower_petal");
        moonflowerPlant = new MutableBush ("moonflower", 6);
        moonflowerBlossom = new MutableBush ("moonflower_blossom", 4, 0, Witherable.WITHERABLE);
        moonflowerSeed = new MutableSeed("moonflower_seed", moonflowerPlant);


        // construction
        lightningPod
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
        });
        shockweed
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
            .setCreativeTab(CreativeTabs.DECORATIONS);
        moonflowerPetal
            .setCreativeTab(CreativeTabs.BREWING);
        moonflowerBlossom
            .setGrowthBlocks(Arrays.asList((Block)moonflowerPlant))
            .setBoundingBox(4, 4)
            .setGrowthHeight(Arrays.asList (4,6,8,8))
            .setCustomGrowth(new QuintConsumer<MutableBush, World, BlockPos, IBlockState, Random>() {
                public void accept (MutableBush this_, World world, BlockPos pos, IBlockState state, Random rand) {
                    System.out.println("in moonflower blossom update func (" + pos + ")");
                    long lunarCycle = 24000 * 8;
                    long phase = world.getWorldTime() % lunarCycle;
                    long fullmoonRise = 12000 + 1000; // night plus dusk

                    long ticksUntilFullMoonRise = (lunarCycle + fullmoonRise - phase) % lunarCycle;
                    boolean fullmoonOverhead = phase >= fullmoonRise && phase < fullmoonRise + 10000; // night minus dusk/dawn
                    System.out.println(fullmoonOverhead
                            // i think that's the right math but i'm not 100% sure
                            // (it's not right but seeing as it's not actually used for anything that's fine)
                            ? ("full moon overhead for the next " + (ticksUntilFullMoonRise % 24000) + " ticks")
                            : ("next full moon rise in " + ticksUntilFullMoonRise + " ticks"));
                    if (this_.isWithered(state)) {
                        this_.dropBlockAsItem(world, pos, state, 0);
                        world.setBlockToAir(pos);
                        return;
                    }
                    if (fullmoonOverhead) {
                        world.setBlockState(pos, this_.advanceGrowth(state));
                        // schedule growth ticks so that most blossoms will be opening around when the moon is directly overhead (~5000 ticks into the night i think)
                        world.scheduleUpdate(pos, this_, 1400 + world.rand.nextInt(400));
                    } else {
                        if (this_.getAge(state) == 0) {
                            // wait an eighth the time expected, and then check again. this is because i _think_ things like sleeping in beds and admins setting the time mess with the tick count? or at least the tick count as far as `scheduledTick` is concerned
                            // also wait at least 240 ticks, so as to not spam updates when night is arriving
                            world.scheduleUpdate(pos, this_, Math.max(240, (int)ticksUntilFullMoonRise / 8));
                        } else {
                            world.setBlockState(pos, this_.setWithered(state));
                            world.scheduleUpdate(pos, this_, 300 + world.rand.nextInt(300));
                        }
                    }
                    return;
                }
            })
            .setCustomItemDrop(new QuadFunction<MutableBush, IBlockState, Random, Integer, ArrayList<ItemStack>>() {
                public ArrayList<ItemStack> apply (MutableBush this_, IBlockState state, Random rand, Integer fortune) {
                    if (this_.isMaxAge(state)) {
                        ArrayList<ItemStack> drops = new ArrayList<ItemStack>();
                        if (this_.isWithered(state)) {
                            drops.add (new ItemStack(moonflowerSeed, 1 + rand.nextInt(2)));
                        } else {
                            drops.add (new ItemStack(moonflowerPetal, 2 + rand.nextInt(4)));
                        }
                        return drops;
                    }
                    return null;
                }
            });
        moonflowerPlant
            .setGrowthBlocks(Arrays.asList(Blocks.GRASS, Blocks.DIRT))
            .setGrowthHeight(Arrays.asList(1,2,3,4,6,8))
            .setGrowthModifiers(Arrays.asList(GrowthModifier.REQUIRES_OUTDOORS, GrowthModifier.PREFERS_DARK))
            .atFullGrowthTick(new QuintConsumer<MutableBush, World, BlockPos, IBlockState, Random>() {
                public void accept(MutableBush this_, World world, BlockPos pos, IBlockState state, Random rand) {
                    if (rand.nextInt(20) == 0 && world.isAirBlock(pos.up()) && world.getLightFromNeighbors(pos.up()) <= 5 && isMoonWaxing(world)) {
                        world.setBlockState(pos.up(), moonflowerBlossom.getDefaultState(), 2);
                    }
                }
                private boolean isMoonWaxing (World world) {
                    long lunarCycle = 24000 * 8;
                    long phase = world.getWorldTime() % lunarCycle;
                    long newMoonSet = 108000 + 12000;
                    long fullmoonRise = 12000 + 1000;
                    return phase > newMoonSet || phase < fullmoonRise;
                }
            });

        log.info("blocks created");
        
        this.register(lightningPod);
        this.register(shockweed);

        this.register(moonflowerSeed);
        this.register(moonflowerPetal);
        this.register(moonflowerPlant, NeedsItemBlock.STANDALONE);
        this.register(moonflowerBlossom, NeedsItemBlock.STANDALONE);
        
        log.info("blocks registered");

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

        BrewingRecipeRegistry.addRecipe
            ( PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM, 1), PotionTypes.AWKWARD)
            , new ItemStack (moonflowerPetal)
            , PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM, 1), PotionTypes.NIGHT_VISION)
            );
        
        log.info("recipes added");
    }

    private enum NeedsItemBlock {
        STANDALONE
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
    
    // register a block without registering an associated itemblock
    public <T extends Block & INamed> T register(T block, NeedsItemBlock v) {
        GameRegistry.register(block);
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
