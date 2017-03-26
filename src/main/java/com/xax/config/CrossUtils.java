package com.xax.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

import com.xax.plantgrowth.MutableBush;
import com.xax.plantgrowth.util.Pair;
import com.xax.plantgrowth.util.TriPredicate;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockReed;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CrossUtils {

    public static final Pair<Block,Integer> getBlockFromString (String name) {
        String modname;
        String blockname;
        int metadata;
        String[] splits = name.split(":");
        if (splits.length == 1) {
            modname = "minecraft";
            blockname = splits[0];
            metadata = -1;
        } else if (splits.length == 2) {
            try {
                // is this something like "sapling:5"?
                metadata = Integer.parseInt(splits[1]);
                blockname = splits[0];
                modname = "minecraft";
            } catch (NumberFormatException e) {
                // nope it's something like "thaumcraft:crystal"
                modname = splits[0];
                blockname = splits[1];
                metadata = -1;
            }
        } else {
            // something like "bop:grass:12"
            modname = splits[0];
            blockname = splits[1];
            metadata = Integer.parseInt(splits[2]);
        }
        Block block = Block.REGISTRY.getObject(new ResourceLocation (modname, blockname));
        if (metadata < -1 || metadata > 15) {
            throw new RuntimeException("block metadata outside the range of 0-15 not allowed");
        }
        return new Pair<Block,Integer> (block, metadata);
    }
    /**
     * note that there are several system materials here (barrier, portal, structure_void, etc) that you REALLY would not want to mess with. they're provided for completeness' sake, not because it's ever a good idea to use them.
     * @param name
     * @return
     */
    public static final Material getMaterialFromString (String name) {
        switch (name.toLowerCase(Locale.ROOT)) {
            case "air": return Material.AIR;
            case "grass": return Material.GRASS;
            case "ground": return Material.GROUND;
            case "wood": return Material.WOOD;
            case "rock": return Material.ROCK;
            case "iron": return Material.IRON;
            case "anvil": return Material.ANVIL;
            case "water": return Material.WATER;
            case "lava": return Material.LAVA;
            case "leaves": return Material.LEAVES;
            case "plants": return Material.PLANTS;
            case "vine": return Material.VINE;
            case "sponge": return Material.SPONGE;
            case "cloth": return Material.CLOTH;
            case "fire": return Material.FIRE;
            case "sand": return Material.SAND;
            case "circuits": return Material.CIRCUITS;
            case "carpet": return Material.CARPET;
            case "glass": return Material.GLASS;
            case "redstone_light": return Material.REDSTONE_LIGHT;
            case "tnt": return Material.TNT;
            case "coral": return Material.CORAL;
            case "ice": return Material.ICE;
            case "packed_ice": return Material.PACKED_ICE;
            case "snow": return Material.SNOW;
            case "crafted_snow": return Material.CRAFTED_SNOW;
            case "cactus": return Material.CACTUS;
            case "clay": return Material.CLAY;
            case "gourd": return Material.GOURD;
            case "dragon_egg": return Material.DRAGON_EGG;
            case "portal": return Material.PORTAL;
            case "cake": return Material.CAKE;
            case "web": return Material.WEB;
            case "piston": return Material.PISTON;
            case "barrier": return Material.BARRIER;
            case "structure_void": return Material.STRUCTURE_VOID;
            default: return null;
        }
    }

    public static final <T extends Comparable<T>, V extends T> Predicate<Integer> allowVariant (final Block b, final IProperty<T> iprop, final V val) {
        return new Predicate<Integer>() {
            public boolean test (Integer meta) {
                return meta == b.getMetaFromState(b.getDefaultState().withProperty(iprop, val));
            }
        };
    }
    public static final <T extends Comparable<T>, V extends T> Predicate<Integer> allowVariants (final Block b, final IProperty<T> iprop, final List<V> vals) {
        return new Predicate<Integer>() {
            public boolean test (Integer meta) {
                for (V val : vals) {
                    if (meta == b.getMetaFromState(b.getDefaultState().withProperty(iprop, val))) {
                        return true;
                    }
                }
                return false;
            }
        };
    }
    
    /**
     * for turning one block into another (dirt into grass, vines into honeysuckle, etc)
     * @param b
     * @return
     */
    public static final TriPredicate<World, BlockPos, IBlockState> transmute (final Block b, final int meta) {
        return new TriPredicate<World, BlockPos, IBlockState>() {
            public boolean test (World w, BlockPos pos, IBlockState state) {
                return state.getBlock() == b
                    && (meta == -1 || state.getBlock().getMetaFromState(state) == meta);
            }
        };
    }
    
    private static final HashMap<Block,List<Integer>> makeBlockLookup (final Set<Pair<Block,Integer>> blocks) {
        final HashMap<Block,List<Integer>> check = new HashMap<Block,List<Integer>>();
        for (Pair<Block,Integer> block : blocks) {
            List<Integer> metas = check.get(block.left);
            if (metas == null) {
                metas = new ArrayList<Integer>(1);
                metas.add(block.right);
                check.put(block.left, metas);
            } else {
                // i think due to object action at a distance this updates the value in the hashmap also
                // FIXME: if somebody's got something like TRANSMUTE sapling:0 sapling:0 sapling:0 then we end up in a weird state. not a _bad_ weird state, just a weird one.
                metas.add(block.right);
            }
        }
        return check;
    }

    public static final TriPredicate<World, BlockPos, IBlockState> transmute (final Set<Pair<Block,Integer>> blocks) {
        // i hate that `final` is required here for mutation safety but also we can happily mutate the hashmap.
        final HashMap<Block,List<Integer>> check = CrossUtils.makeBlockLookup (blocks);
        System.out.println("made block lookup for transmute: " + check);

        return new TriPredicate<World, BlockPos, IBlockState>() {
            public boolean test (World w, BlockPos pos, IBlockState state) {
                Block block = state.getBlock();
                List<Integer> metas = check.get(block);
                //System.out.println("in transmute pred, w/ check " + check + "; looking up block " + block);
                //System.out.println("got " + metas);
                if (metas == null) {
                    return false;
                }
                return metas.contains(-1) || metas.contains(block.getMetaFromState(state));
            }
        };
    }
    /**
     * for replacing a kind of block (i.e, AIR, WATER, etc)
     * @param m
     * @return
     */
    public static final TriPredicate<World, BlockPos, IBlockState> replacesMaterial (final Material m) {
        return new TriPredicate<World, BlockPos, IBlockState>() {
            public boolean test (World w, BlockPos pos, IBlockState state) {
                return state.getMaterial() == m;
            }
        };
    }
    public static final TriPredicate<World, BlockPos, IBlockState> replacesMaterial (final Set<Material> ms) {
        return new TriPredicate<World, BlockPos, IBlockState>() {
            public boolean test (World w, BlockPos pos, IBlockState state) {
                return ms.contains(state.getMaterial());
            }
        };
    }
    /**
     * for placing one block on top of another one. note there's no "what block are we replacing" check;
     * @param b
     * @return
     */
    public static final TriPredicate<World, BlockPos, IBlockState> placedOn (final Block b, final int meta) {
        return new TriPredicate<World, BlockPos, IBlockState>() {
            public boolean test (World w, BlockPos pos, IBlockState state) {
                IBlockState down = w.getBlockState(pos.down());
                Block block = down.getBlock();
                return block == b
                    && (meta == -1 || block.getMetaFromState(down) == meta);
            }
        };
    }
    public static final TriPredicate<World, BlockPos, IBlockState> placedOn (final Set<Pair<Block,Integer>> blocks) {
        final HashMap<Block,List<Integer>> check = CrossUtils.makeBlockLookup (blocks);
        return new TriPredicate<World, BlockPos, IBlockState>() {
            public boolean test (World w, BlockPos pos, IBlockState state) {
                IBlockState down = w.getBlockState(pos.down());
                Block block = down.getBlock();
                List<Integer> metas = check.get(block);
                if (metas == null) {
                    return false;
                }
                return metas.contains(-1) || metas.contains(block.getMetaFromState(down));
            }
        };
    }

    /**
     * for placing one block against another adjacent one (e.g., making vines grow on leaves)
     * @param b
     * @return
     */
    public static final TriPredicate<World, BlockPos, IBlockState> placedAgainst (final Block b, final int meta) {
        return new TriPredicate<World, BlockPos, IBlockState>() {
            public boolean test (World w, BlockPos pos, IBlockState state) {
                IBlockState[] adjs =
                    { w.getBlockState(pos.north())
                    , w.getBlockState(pos.south())
                    , w.getBlockState(pos.east())
                    , w.getBlockState(pos.west())
                    };
                for (IBlockState adj : adjs) {
                    if (adj.getBlock() == b
                        && (meta == -1 || adj.getBlock().getMetaFromState(adj) == meta)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }
    public static final TriPredicate<World, BlockPos, IBlockState> placedAgainst (final Set<Pair<Block,Integer>> blocks) {
        final HashMap<Block,List<Integer>> check = CrossUtils.makeBlockLookup (blocks);
        return new TriPredicate<World, BlockPos, IBlockState>() {
            public boolean test (World w, BlockPos pos, IBlockState state) {
                IBlockState[] adjs =
                    { w.getBlockState(pos.north())
                    , w.getBlockState(pos.south())
                    , w.getBlockState(pos.east())
                    , w.getBlockState(pos.west())
                    };
                for (IBlockState adj : adjs) {
                    Block block = adj.getBlock();
                    List<Integer> metas = check.get(block);
                    if (metas.contains(-1) || metas.contains(block.getMetaFromState(adj))) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    public static final TriPredicate<World, BlockPos, IBlockState> havingLightAndAirAbove (final int light) {
        return new TriPredicate<World, BlockPos, IBlockState>() {
            public boolean test (World w, BlockPos pos, IBlockState state) {
                return w.isAirBlock(pos.up()) && w.getLight(pos.up()) >= light;
            }
        };
    }

    public static final TriPredicate<World, BlockPos, IBlockState> default_ (final BlockCactus b) {
        return new TriPredicate<World, BlockPos, IBlockState>() {
            public boolean test (World world, BlockPos pos, IBlockState state) {
                return b.canBlockStay(world, pos);
            }
        };
    }

    public static final TriPredicate<World, BlockPos, IBlockState> default_ (final BlockReed b) {
        return new TriPredicate<World, BlockPos, IBlockState>() {
            public boolean test (World world, BlockPos pos, IBlockState state) {
                return b.canBlockStay(world, pos);
            }
        };
    }

    public static final TriPredicate<World, BlockPos, IBlockState> default_ (final MutableBush b) {
        return new TriPredicate<World, BlockPos, IBlockState>() {
            public boolean test (World world, BlockPos pos, IBlockState state) {
                return b.canBlockStay(world, pos, state);
            }
        };
    }

    public static final TriPredicate<World, BlockPos, IBlockState> default_ (final BlockBush b) {
        return new TriPredicate<World, BlockPos, IBlockState>() {
            public boolean test (World world, BlockPos pos, IBlockState state) {
                return b.canBlockStay(world, pos, state);
            }
        };
    }

    public static final CrossPred and (final List<TriPredicate<World,BlockPos,IBlockState>> ands) {
        return new CrossPred (new TriPredicate <World, BlockPos, IBlockState>() {
            public boolean test (World a, BlockPos b, IBlockState c) {
                for (TriPredicate<World,BlockPos,IBlockState> and : ands) {
                    if (!and.test(a, b, c)) {
                        return false;
                    }
                }
                return true;
            }
        });
    }
}
