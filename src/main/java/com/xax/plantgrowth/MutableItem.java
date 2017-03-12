package com.xax.plantgrowth;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public class MutableItem extends Item implements INamed {

	protected String name;

	public MutableItem(String name) {
		this.name = name;
		setUnlocalizedName(name);
		setRegistryName(name);
	}
	
	public String getName() {
		return this.name;
	}

	@Override
	public MutableItem setCreativeTab(CreativeTabs tab) {
		super.setCreativeTab(tab);
		return this;
	}
}
