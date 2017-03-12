package com.xax.plantgrowth.proxy;

import org.apache.logging.log4j.Logger;

import net.minecraft.item.Item;

public interface IProxy {
	
    public void foo(Logger log);

    public void registerItemRenderer(Item item, int meta, String id);
}