package com.xax.hook;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.Logger;

import com.xax.config.CrossRecipe;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class EventHook {
    BiConsumer<World,EntityLightningBolt> lightningHandler;
    ArrayList<CrossRecipe> crossbreedingRecipes;

    protected int updateLCG = (new Random()).nextInt();
    protected int crossTicks = 3;

    protected Logger log;

    public EventHook(Logger log) {
        this.log = log;
        this.lightningHandler = null;
        this.crossbreedingRecipes = new ArrayList<CrossRecipe>();
    }

    public void attachLightningStrikeHandler (BiConsumer<World, EntityLightningBolt> lightningHandler) {
        this.lightningHandler = lightningHandler;
    }

    public void setCrossTicks (int ticks) {
        this.crossTicks = ticks;
    }

    public void addCross (CrossRecipe rec) {
        this.crossbreedingRecipes.add(rec);
    }

    // there doesn't seem to be a forge lightning strike event (just an "entity struck by lightning" event), so this is a hack with some of the logic borrowed from the old pneumaticcraft lightning seeds implementation
    @SubscribeEvent
    public void tickEnd(TickEvent.WorldTickEvent event){
        if(event.phase == TickEvent.Phase.END) {
            this.checkLightning(event.world);
            
            this.doCrossbreedTicks(event.world);
        }
    }

    private void checkLightning (World world) {
        if(world.isRemote) {
            return;
        }
        for(Entity effect : world.weatherEffects) {
            if(effect.ticksExisted == 1 && effect instanceof EntityLightningBolt && this.lightningHandler != null) {
                this.lightningHandler.accept(world, (EntityLightningBolt)effect);
            }
        }
    }
    private void doCrossbreedTicks (World world) {
        // mimic growth ticks
        // okay so _apparently_ just b/c `world.isRemote == false` doesn't mean `world instanceOf WorldServer == true`. i don't know under what conditions this will really pass, and consequently under which conditions this will actually run.
        if (world.isRemote || !(world instanceof WorldServer)) {
            return;
        }
        int randomTicks = this.crossTicks;
        if (randomTicks <= 0 ) {
            return;
        }
        int xI;
        int zI;
        ArrayList<CrossRecipe> toCheck = new ArrayList<CrossRecipe>();
        for (Iterator<Chunk> chunks = ((WorldServer)world).getPlayerChunkMap().getChunkIterator(); chunks.hasNext();){
            Chunk chunk = chunks.next();
            xI = chunk.xPosition * 16;
            zI = chunk.zPosition * 16;
            for (ExtendedBlockStorage extendedblockstorage : chunk.getBlockStorageArray()) {
                if (extendedblockstorage == Chunk.NULL_BLOCK_STORAGE) {
                    continue;
                }
                for (int i = 0; i < randomTicks; ++i){
                    this.updateLCG = this.updateLCG * 3 + 1013904223;
                    int r = this.updateLCG >> 2;
                    int x = r & 15;
                    int z = r >> 8 & 15;
                    int y = r >> 16 & 15; // is each extendedblockstorage slice literally just 16 blocks tall??
                    IBlockState iblockstate = extendedblockstorage.get(x, y, z);
                    Block block = iblockstate.getBlock();
                    BlockPos pos = new BlockPos(xI + x, y + extendedblockstorage.getYLocation(), zI + z);
                    toCheck.clear();
                    for (CrossRecipe cross : this.crossbreedingRecipes) {
                        // check to see if this is an acceptable base block
                        if (block != cross.base) {
                            continue;
                        }
                        // add this recipe to the to-check list
                        toCheck.add(cross);
                    }
                    // if the to-check list is empty: continue (to next block)
                    if (toCheck.isEmpty()) {
                        continue;
                    }
                    //this.log.info("crossbreeding tick @ " + pos + " (" + block.getLocalizedName() + ").");
                    // get adjacent blocks (9*5*9 prism centered on this block)
                    for (BlockPos blockpos : BlockPos.getAllInBoxMutable(pos.add(-4, -2, -4), pos.add(4, 2, 4))) {
                        for (CrossRecipe cross : toCheck) {
                            cross.addTally(world.getBlockState(blockpos));
                        }
                    }
                    for (CrossRecipe cross : toCheck) {
                        if (cross.satisfied()) {
                            //this.log.info("recipe for " + cross.result.getLocalizedName() + " satisfied; trying locations");
                            BlockPos attempt = null;
                            int tries = 10;
                            while (tries > 0) {
                                attempt = pos.add
                                    (world.rand.nextInt(7) - 3
                                    ,world.rand.nextInt(3) - world.rand.nextInt(3) // [0,0,0,-1,-1,1,1-2,2]
                                    ,world.rand.nextInt(7) - 3);
                                //this.log.info("trying " + attempt);
                                if (cross.test.pred.test(world, attempt, world.getBlockState(attempt))) {
                                    break;
                                }
                                tries--;
                                attempt = null;
                            }
                            if (attempt != null) {
                                this.log.info("crossbreeding success! (after " + (10 - tries + 1) + " tries) placing block " + cross.result.getLocalizedName() + " at " + attempt);
                                world.setBlockState(attempt, cross.result.getDefaultState(), 2);
                                // also spawn some green sparkles when a crossbreeding action happens
                                world.playEvent(2005, attempt, 0);
                            }
                        }
                        cross.clearTally();
                    }
                }
            }
        }
    }
}
