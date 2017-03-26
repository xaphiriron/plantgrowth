package com.xax.config;

import java.util.function.Predicate;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;

public class CrossReq {

    public final Block block;
    public final Predicate<Integer> variant;
    public final int quantity;
    
    public CrossReq (Block b) {
        this(b, new Predicate<Integer>() {
            public boolean test (Integer v) {
                return true;
            }
        }, 1);
    }

    public CrossReq (Block b, int q) {
        this(b, new Predicate<Integer>() {
            public boolean test (Integer v) {
                return true;
            }
        }, q);
    }
    
    public CrossReq (Block b, Predicate<Integer> v) {
        this(b, v, 1);
    }
    
    public CrossReq (Block b, Predicate<Integer> v, int q) {
        this.block = b;
        this.variant = v;
        this.quantity = q;
    }

    public boolean satisfied (IBlockState state) {
        return state.getBlock() == this.block
                && this.variant.test(state.getBlock().getMetaFromState(state));
    }

}
