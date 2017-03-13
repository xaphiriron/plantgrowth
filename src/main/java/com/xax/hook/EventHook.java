package com.xax.hook;

import java.util.function.BiConsumer;

import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class EventHook {
    BiConsumer<World,EntityLightningBolt> lightningHandler;
    
    public EventHook() {
        this.lightningHandler = null;
    }

    public void attachLightningStrikeHandler (BiConsumer<World, EntityLightningBolt> lightningHandler) {
        this.lightningHandler = lightningHandler;
    }
    

    // there doesn't seem to be a forge lightning strike event (just an "entity struck by lightning" event), so this is a hack with some of the logic borrowed from the old pneumaticcraft mod lightning seeds implementation
    @SubscribeEvent
    public void tickEnd(TickEvent.WorldTickEvent event){
        if(event.phase == TickEvent.Phase.END) {
            this.checkLightning(event.world);
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
}
