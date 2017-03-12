package com.xax.plantgrowth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.xax.plantgrowth.proxy.CommonProxy;

//import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
//import net.minecraftforge.fml.common.TracingPrintStream;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

@Mod(modid = "plantgrowth")
public class Growth {
	public static final String modId = "plantgrowth";
	public static final Logger log = LogManager.getLogger("plantgrowth");
	
    @SidedProxy(modId = "plantgrowth", clientSide = "com.xax.plantgrowth.proxy.ClientProxy", serverSide = "com.xax.plantgrowth.proxy.ServerProxy")
    public static CommonProxy proxy;

    @EventHandler
    public void PreInit(FMLPreInitializationEvent event){
        log.info("forge preinit");
        proxy.foo(log);
        log.info("done loading blocks");
    }
    
    @EventHandler
    public void load(FMLInitializationEvent event){
        log.info("forge init/load");
    }
    
    @EventHandler
    public void postInit(FMLPostInitializationEvent event){
        log.info("forge postinit");
    }
    
    @EventHandler
    public void onServerStart(FMLServerStartingEvent event){
        log.info("server starting");
    }
}
