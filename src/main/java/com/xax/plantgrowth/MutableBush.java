package com.xax.plantgrowth;

import java.lang.reflect.Field;
//import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.xax.enumerable.BitField;
import com.xax.plantgrowth.util.QuadPredicate;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockFarmland;
//import net.minecraft.block.BlockFarmland;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
//import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class MutableBush extends BlockBush implements IGrowable, INamed {
    private ArrayList<Block> growsOn;
    private GrowthType growthType;
    private String name;
    private int lowLight;
    private int highLight;
    private int lowElevation;
    private int highElevation;

    private int maxAge;
    private BitField<PropertyInteger> age;
    private ArrayList<AxisAlignedBB> aabbs = new ArrayList<AxisAlignedBB>();
    
    private boolean witherable;
    private int witherVal; // the yield data value that means "this plant is withered"
    private QuadPredicate<World, BlockPos, IBlockState, Random> witheringCallback;
    private WitheringAction witheringAction;
    
    private int maxYield;
    private BitField<PropertyInteger> yield;
    
    private HashSet<GrowthModifier> growthModifiers;
    
    private int spreadHoriz;
    private int spreadVert;
    private int spreadChance;
    
    private Item seed;
    private Item crop;
    
    private boolean inConstructor;
    
    protected enum GrowthType {
        SOLID,
        SELECT,
        COMPLICATED
    }
    
    public enum Witherable {
        WITHERABLE
    }
    
    public enum WitheringAction {
        DIES_INSTANTLY,
        REVERSE_GROWTH
    }

    public enum GrowthModifier {
        LIKES_MOISTURE, // grows faster on wet farmland or when its soil is adjacent to a water block (but w/ no bonus for both)
        DEEP_SOIL,      // grows faster when the 3 blocks beneath it all pass `canSustainBush`
        BROAD_SOIL,     // grows faster when the 3x3 soil blocks around its base pass `canSustainBush`
            // while also checking LIKES_MOISTURE and DEEP_SOIL for those blocks too, at 25% strength
            // not yet implemented
        LINE_GROWTH,    // grows slower when there's adjacent crops of the same type in anything other than a straight line
        PREFERS_LIGHT,  // grows slower at lower light levels
        PREFERS_DARK,   // grows slower at higher light levels
        LIKES_RAIN,     // grows (considerably) faster when it's raining
        SPREADS;        // spreads like mushrooms. if it has growth stages, only spreads at full growth
    }

    public MutableBush(String name) {
        this.inConstructor = true;
        this.init(name);
        
        this.inConstructor = false;
    }
    public MutableBush(String name, int growthStages) {
        this.inConstructor = true;
        this.init(name);
        this.canGrowToStage (growthStages <= 0 ? 0 : growthStages-1);

        this.rebuildBlockState();
        this.inConstructor = false;
    }
    public MutableBush(String name, int growthStages, int yieldStages) {
        this.inConstructor = true;
        this.init(name);
        this.canGrowToStage (growthStages <= 0 ? 0 : growthStages-1);
        this.hasYieldStages (yieldStages <= 0 ? 0 : yieldStages-1);

        this.rebuildBlockState();
        this.inConstructor = false;
    }
    public MutableBush(String name, int growthStages, int yieldStages, Witherable w) {
        this.inConstructor = true;
        this.init(name);
        this.setWitherable();
        this.canGrowToStage (growthStages <= 0 ? 0 : growthStages-1);
        this.hasYieldStages(yieldStages <= 0 ? 0 : yieldStages-1);
        
        this.rebuildBlockState();
        this.inConstructor = false;
    }
    private void init(String name) {
        this.name = name;
        setUnlocalizedName(name);
        setRegistryName(name);
        
        this
            .setGrowthSolid()
            .allowAnyLight()
            .allowAnyElevation();
        this.maxAge = 0;
        this.maxYield = 0;
        
        this.witherable = false;
        this.witheringCallback = null;
        
        this.growthModifiers = new HashSet<GrowthModifier>();

        this.spreadHoriz = 1;
        this.spreadVert = 1;
        this.spreadChance = 25;
        
        this.crop = null;
        this.seed = null;

        this.setHardness(0.0F);
        this.setSoundType(SoundType.PLANT);
        this.disableStats(); // i guess plants aren't counted for mined/placed stats
    }
    
    public String getName() {
        return this.name;
    }
    
    /**
     * @param blocks
     * @return
     * Takes a list of blocks (of any metadata/variant) that the bush can be planted on 
     */
    public MutableBush setGrowthBlocks(List<Block> blocks){
        this.growthType = GrowthType.SELECT;
        this.growsOn = new ArrayList<Block>(blocks.size());
        for (Block b : blocks) {
            this.growsOn.add(b);
        }
        return this;
    }
    public MutableBush setGrowthSolid() {
        this.growthType = GrowthType.SOLID;
        this.growsOn = new ArrayList<Block>();
        return this;
    }
    
    public MutableBush setGrowthModifiers(List<GrowthModifier> mods) {
        this.growthModifiers.clear();
        for (GrowthModifier mod : mods) {
            this.growthModifiers.add(mod);
        }
        return this;
    }
    
    public MutableBush setSpreadDimensions(int h, int v) {
        if (h <= 0 || v <= 0 || h > 8 || v > 8) {
            throw new RuntimeException ("invalid crop spread dimensions; h: " + h + "; v: " + v + ". (8 is the max allowed)");
        }
        this.spreadHoriz = h;
        this.spreadVert = h;
        return this;
    }
    public MutableBush setSpreadChance(int c) {
        if (c <= 0) {
            throw new RuntimeException ("invalid crop spread chance; got " + c + "; limits are 1 (fast) to infinity (slow)");
        }
        this.spreadChance = c;
        return this;
    }
    
    public MutableBush setCrop(Item crop) {
        this.crop = crop;
        return this;
    }
    
    // set allowed light levels. allowed range from 0 to 15; low must be <= high
    public MutableBush setAllowedLight(int low, int high) {
        if (low >= 0 && high < 16 && low <= high) {
            this.lowLight = low;
            this.highLight = high;
        } else {
            throw new RuntimeException ("invalid light levels; low:" + low + "; high:" + high + ".");
        }
        return this;
    }
    public MutableBush allowAnyLight() {
        this.lowLight = 0;
        this.highLight = 15;
        return this;
    }
    
    // set allowed elevation. allowed range from 0 to 255; low must be <= high
    public MutableBush setAllowedElevation(int low, int high) {
        if (low >= 0 && high < 256 && low <= high) {
            this.lowElevation = low;
            this.highElevation = high;
        } else {
            throw new RuntimeException ("invalid height levels; low:" + low + "; high:" + high + ".");
        }
        return this;
    }
    public MutableBush allowAnyElevation() {
        this.lowElevation = 0;
        this.highElevation = 255;
        return this;
    }
    
    
    public MutableBush withersWhen (QuadPredicate<World, BlockPos, IBlockState, Random> witherCallback) {
        this.witheringCallback = witherCallback;
        return this;
    }
    
    public MutableBush witherAction (WitheringAction witherAction) {
        this.witheringAction = witherAction;
        return this;
    }
    
    /*
     * probably-should-be-internal state-setting functions
     */
    public MutableBush canGrowToStage(int maxAge) {
        ArrayList<Integer> baseHeights = new ArrayList<Integer>(maxAge + 1);
        this.maxAge = maxAge;
        for (int i = maxAge + 1; i > 0; i--) {
            baseHeights.add(8);
        }
        this.setGrowthHeight(baseHeights);

        this.setTickRandomly(true); // growing plants should receive ticks
        return this;
    }
    
    public MutableBush hasYieldStages(int maxYield) {
        this.maxYield = maxYield;
        return this;
    }
    
    public MutableBush setWitherable() {
        this.witherable = true;
        this.witheringAction = WitheringAction.DIES_INSTANTLY; // this is the default
        this.setTickRandomly(true); // withering plants should receive ticks
        return this;
    }
    
    /**
     * takes a list of integer values that determine how tall the block is, in eighths of a block (so 8 is one block tall).
     * 
     * if there aren't enough values, it pads with the final value. if there are too many values it throws a `RuntimeException`.
     * @param heights
     * @return itself
     */
    public MutableBush setGrowthHeight(List<Integer> heights) {
        int extraNeeded = this.maxAge - heights.size() + 1;
        int last = 8;
        if (extraNeeded < 0) {
            throw new RuntimeException("too many height values (" + heights.size() + ") for the number of growth stages (" + (this.maxAge+1) + ")");
        }
        this.aabbs.clear();
        for (Integer h : heights) {
            if (h < 1 || h > 8) {
                throw new RuntimeException("height value outside range (1-8 allowed; got " + h + ")");
            }
            this.aabbs.add(new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, (double)h / 8.0D, 1.0D));
            last = h.intValue();
        }
        for (int i = extraNeeded; i > 0 ; i--) {
            this.aabbs.add(new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, (double)last / 8.0D, 1.0D));
        }
        return this;
    }

    /**
     * .createBlockState() is called as part of the `Block` constructor, so it will already
     * exist with no properties. the `.blockState` field is additionally set `final`, so it
     * can't be overwritten. i THINK `.blockState`s aren't actually used until _at least_
     * the blocks are registered in the block index, so as long as no edits are made
     * after-the-fact this should be fine.
     * but, you know, no promises.
     */
    private void rebuildBlockState() {
        if (!this.inConstructor) {
            //log.warn("after-the-fact growth edit; blockState hack might leave the object in an indeterminate state.");
        }
        IBlockState defaultState;
        int ageBits = 0;
        int yieldBits = 0;
        
        // first, set the props
        if (this.hasAge()) {
            this.age = new BitField<PropertyInteger>(PropertyInteger.create("age", 0, this.maxAge), this.maxAge);
            ageBits = this.age.getUsedBits();
        }
        if (this.hasYield()) {
            int actualMaxYield = this.maxYield + (this.witherable ? 1 : 0);
            if (this.witherable) {
                this.witherVal = this.maxYield + 1;
            }
            this.yield = new BitField<PropertyInteger>(PropertyInteger.create("yield", 0, actualMaxYield), actualMaxYield, ageBits);
            yieldBits = this.yield.getUsedBits();
        }
        // then hack the state (since it needs the prop values to be set)
        this.hackBlockState();
        // then construct the default state (since it needs a blockState value + props set)
        defaultState = this.blockState.getBaseState();
        if (this.hasAge()) {
            defaultState = defaultState.withProperty(this.age.getVal(), 0);
        }
        if (this.hasYield()) {
            defaultState = defaultState.withProperty(this.yield.getVal(), 0);
        }
        // then check to see if the state is actually coherent wrt metadata space
        if (ageBits + yieldBits > 4) {
            throw new RuntimeException("too many metadata bits for plant \"" + this.name
                    + "\"! age uses "+ ageBits +" bits (" + this.maxAge
                    + "); yield uses " + yieldBits + " bits (" + this.maxYield + "+"
                    + (this.witherable ? 1 : 0) + "); blocks only have 4! THIS BLOCK WILL NOT WORK CORRECTLY AT ALL");
        }
        // then set the default state!
        this.setDefaultState(defaultState);
    }
    
    private void hackBlockState() {
        try {
            Field state = null;
            // .getField only finds public fields; blockState is protected and so can't be found.
            // but .getDeclaredFields returns everything!
            for (Field f : Block.class.getDeclaredFields()) {
                if (f.getName() == "blockState") {
                    state = f;
                    break;
                }
            }
            if (state == null) {
                throw new NoSuchFieldException("blockState");
            }
            state.setAccessible(true);
            state.set(this, this.createBlockState());
        } catch (NoSuchFieldException e) {
            System.err.println("programmer error hacking blockstate; no such field " + e.getMessage());
        } catch (IllegalAccessException e) {
            System.err.println("blockState hack prevented; everything is about to go horribly wrong. (" + e.getMessage() + ")");
        }
    }
    
    @Override
    public MutableBush setCreativeTab(CreativeTabs tab) {
        super.setCreativeTab(tab);
        return this;
    }
    
    private boolean canGrowInLight (int light) {
        return light >= this.lowLight && light <= this.highLight;
    }
    private boolean canGrowAtElevation (int height) {
        return height >= this.lowElevation && height <= this.highElevation;
    }

    
    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        if (this.hasAge()) {
            return this.aabbs.get(state.getValue(this.getAgeProperty()));
        } else {
            return this.aabbs.get(0);
        }
    }

    @Override
    protected boolean canSustainBush(IBlockState state) {
        switch (this.growthType) {
        case SOLID:
            return state.isFullBlock();
        case SELECT:
            return this.growsOn.contains (state.getBlock());
        case COMPLICATED:
            // not yet implemented
            return false;
        default:
            throw new RuntimeException("invalid MutableBush growth value");
        }
    }

    // the pos and state are the state of the bush block itself, not the block it's on.
    @Override
    public boolean canBlockStay(World worldIn, BlockPos pos, IBlockState state) {
        IBlockState supporting = worldIn.getBlockState(pos.down());
        // this determines under what conditions the plant is destroyed (e.g., mushrooms on too-bright tiles)
        return this.canSustainBush (supporting) && this.canGrowInLight (worldIn.getLight(pos)) && this.canGrowAtElevation (pos.getY());
    }


    /**
     * IGrowable interface
     */
    // can be right-clicked with bonemeal
    public boolean canGrow(World worldIn, BlockPos pos, IBlockState state, boolean isClient) {
        return false;
        //return !this.isMaxAge(state);
    }

    // does right-clicking with bonemeal do anything (frequently a random true/false for things like mushrooms and saplings)
    public boolean canUseBonemeal(World worldIn, Random rand, BlockPos pos, IBlockState state) {
        return false;
    }

    // this gets called when bonemeal is used and canUseBonemeal returns true
    public void grow(World worldIn, Random rand, BlockPos pos, IBlockState state) {

    }

    /*
     * reimplementing the BlockCrop interface, basically
     */
    protected int getMaxAge() {
        return this.maxAge;
    }

    protected PropertyInteger getAgeProperty() {
        return this.age.getVal();
    }

    protected IBlockState withAge(int age) {
        return this.getDefaultState().withProperty(this.getAgeProperty(), Integer.valueOf(age));
    }

    protected int getAge(IBlockState state) {
        if (!this.hasAge()) {
            throw new RuntimeException ("tried to get age on a plant block without an age property.");
        } else {
            return state.getValue(this.age.getVal());
        }
    }
    protected int getYield(IBlockState state) {
        if (this.maxYield == 0) {
            throw new RuntimeException ("tried to get yield on a plant block without a yield property.");
        } else {
            return state.getValue(this.yield.getVal());
        }
    }
    protected boolean getWithered(IBlockState state) {
        int witheredValue;
        if (!this.witherable) {
            throw new RuntimeException ("tried to get withered on a plant block without a withered property.");
        } else {
            // sorry for the bit-twiddling here, but the idea is the withered value should be whatever yield value is all 1s
            // tbqh this could and probably should be `this.maxYield+1` instead
            /*witheredValue = this.maxYield == 0
                    ? 1
                    : (Integer.highestOneBit(this.maxYield) << 1) - 1;
            */
            witheredValue = this.maxYield + 1;
            return state.getValue(this.yield.getVal()) == witheredValue;
        }
    }
    
    public boolean hasAge() {
        return this.maxAge != 0;
    }
    public boolean hasWithered() {
        return this.witherable;
    }
    public boolean hasYield() {
        return this.maxYield > 0 || this.witherable;
    }

    public boolean isMaxAge(IBlockState state) {
        if (this.maxAge == 0) {
            return true;
        } else {
            return state.getValue(this.age.getVal()).intValue() >= this.maxAge;
        }
    }

    // not sure under what conditions these are called
    // this is called when the block is destroyed? i guess?
    @Override
    @Nullable
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        if (this.hasAge() && this.isMaxAge(state) && this.crop != null) {
            return this.crop;
        }
        return this.seed;
    }
    // no clue about this
    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        if (this.seed != null) {
            return new ItemStack(this.seed);
        }
        return null;
    }
    

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        super.updateTick(worldIn, pos, state, rand);
        boolean resetYield = false;
        
        if (this.witherable) {
            if (this.witheringCallback != null && this.witheringCallback.test(worldIn, pos, state, rand)) {
                switch (this.witheringAction) {
                case DIES_INSTANTLY:
                    worldIn.setBlockToAir(pos);
                    return;
                case REVERSE_GROWTH:
                    int age = 0;
                    // if it has age left to reverse, reverse it. if not, die.
                    if (this.hasAge() && (age = this.getAge(state)) != 0) {
                        worldIn.setBlockState(pos, state
                            .withProperty(this.age.getVal(), age - 1)
                            .withProperty(this.yield.getVal(), this.witherVal), 2);
                    } else {
                        worldIn.setBlockToAir(pos);
                    }
                    return;
                default:
                    throw new RuntimeException ("withering crop with no wither action set!");
                }
            }
            if (this.getWithered(state)) {
                // this plant withered at some point, but now it's fine, so we should set the yield value to 0
                resetYield = true;
                state = state.withProperty(this.yield.getVal(), 0);
            }
        }

        if (this.hasAge() && this.canGrowInLight (worldIn.getLightFromNeighbors(pos.up()))) {
            int age = this.getAge(state);
            if (age < this.getMaxAge()) {
                float f = getGrowthChance(this, worldIn, pos);
                if (rand.nextInt((int)(25.0F / f) + 1) == 0) {
                    worldIn.setBlockState(pos, state.withProperty(this.age.getVal(), age + 1), 2);
                }
            }
        } else if (resetYield) {
            // reset wither state even if this can't grow / didn't grow this tick
            worldIn.setBlockState(pos, state, 2);
        }
        if (this.growthModifiers.contains(GrowthModifier.SPREADS)
            && this.isMaxAge(state)
            && rand.nextInt(this.spreadChance) == 0
            && !this.fullySpread(worldIn, pos)) {
            BlockPos attempt = null;
            int tries = 4;
            int hSpan = this.spreadHoriz * 2 + 1;
            int vSpan = this.spreadVert * 2 + 1;
            // try a few times to find a suitable position in the spreadHoriz*spreadVert*spreadHoriz adjacent cells
            while (tries > 0) {
                attempt = pos.add
                    (rand.nextInt(hSpan) - this.spreadHoriz
                    ,rand.nextInt(vSpan) - this.spreadVert
                    ,rand.nextInt(hSpan) - this.spreadHoriz);
                if (worldIn.isAirBlock(attempt) && this.canBlockStay(worldIn, attempt, this.getDefaultState())) {
                    break;
                }
                tries--;
                attempt = null;
            }
            // if we got a working cell, plant a new thing
            if (attempt != null) {
                worldIn.setBlockState(attempt, this.getDefaultState(), 2);
            }
        }
    }

    // this is basically the mushroom-spreading check, only with some values based on the spreading amount
    private boolean fullySpread (World worldIn, BlockPos pos) {
        int range = this.spreadHoriz * 2 + 1;
        int maxAllowed = (range * range) / 2 + 1; // i.e., 5 for 3x3, 13 for 5x5, etc. however, (like the mushroom check) it checks more than the spreading range
        // we want to check further out from the spreading limit, just to avoid having plants at the 'edges' constantly growing further out, but we don't want to check _that much_ further out just for efficiency reasons.
        for (BlockPos nearby : BlockPos.getAllInBoxMutable
                ( pos.add(-(this.spreadHoriz+2), -(this.spreadVert+1), -(this.spreadHoriz+2))
                , pos.add(this.spreadHoriz+2, this.spreadVert+1, this.spreadHoriz+2))) {
            if (worldIn.getBlockState(nearby).getBlock() == this) {
                --maxAllowed;
                if (maxAllowed <= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    // this should probably go into a utility class
    // check a prism surrounding a block to see if any blocks match the predicate
    protected boolean adjacentBlockMatches (World worldIn, BlockPos base, int nsSpan, int ewSpan, int ySpan, Predicate<IBlockState> test) {
        IBlockState state;
        // i don't actually know if z is north/south and x is east/west, but look there's no reason to make non-square checks so it should be fine.
        for (int z = -nsSpan; z <= nsSpan; ++z) {
            for (int x = -ewSpan; x <= ewSpan; ++x) {
                for (int y = -ySpan; y <= ySpan; ++y) {
                    if (x == 0 && y == 0 && z == 0)
                        continue;
                    state = worldIn.getBlockState(base.add(x, y, z));
                    if (test.test(state)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected float blockGrowthRating(World worldIn, BlockPos pos) {
        float f = 0;
        // you get one point for being in suitable ground. that gets increased to three points if yr in wet soil / adjacent to water (and you care about that kind of thing)
        IBlockState state = worldIn.getBlockState(pos);
        if (canSustainBush(state)) {
            f = 1.0F;
        }
        if (this.growthModifiers.contains(GrowthModifier.LIKES_MOISTURE)) {
            if (((Integer)state.getValue(BlockFarmland.MOISTURE)).intValue() > 0) {
                f = 3.0F;
            } else if (adjacentBlockMatches (worldIn, pos, 1, 1, 0,
                // what a monster anonymous class instances are. (this would be a lambda but that's java 1.8+ only and idk if that's okay with forge)
                new Predicate<IBlockState>() {
                    public boolean test(IBlockState adj) { return adj.getMaterial() == Material.WATER; }
                })) {
                f = 3.0F;
            }
        }
        return f;
    }

    // returns a number that has to be between 0 and 25, which is used to generate a number between 0 and (25 / n) + 1. i think.
    protected float getGrowthChance(Block blockIn, World worldIn, BlockPos pos) {
        float f = 1.0F;
        float plus = 0.0F;

        // handles the basic LIKES_MOISTURE check
        plus = this.blockGrowthRating (worldIn, pos.down());
        f += plus;

        /*
        if (this.growthModifiers.contains(GrowthModifier.BROAD_SOIL)) {
            for (int i = -1; i <= 1; ++i) {
                for (int j = -1; j <= 1; ++j) {
                    if (i == 0 && j == 0) {
                        continue;
                    }
                    plus = 0.0F;
                    supportingState = worldIn.getBlockState(supporting.add(i, 0, j));
                    if (canSustainBush(supportingState)) {
                        plus = 0.25F;
                        if (((Integer)supportingState.getValue(BlockFarmland.MOISTURE)).intValue() > 0) {
                            plus = 0.75F;
                        }
                    }
                    f += plus;
                }
            }
        }
        */

        // halves growth chance if it's any more tightly-packed than being grown in lines
        if (this.growthModifiers.contains(GrowthModifier.LINE_GROWTH)) {
            BlockPos north = pos.north();
            BlockPos south = pos.south();
            BlockPos west = pos.west();
            BlockPos east = pos.east();
            boolean neighboringEWCrops = blockIn == worldIn.getBlockState(west).getBlock()
                    || blockIn == worldIn.getBlockState(east).getBlock();
            boolean neighboringNSCrops = blockIn == worldIn.getBlockState(north).getBlock()
                    || blockIn == worldIn.getBlockState(south).getBlock();

            if (neighboringEWCrops && neighboringNSCrops) {
                f /= 2.0F;
            } else {
                boolean diagonalCrops = blockIn == worldIn.getBlockState(west.north()).getBlock()
                        || blockIn == worldIn.getBlockState(east.north()).getBlock()
                        || blockIn == worldIn.getBlockState(east.south()).getBlock()
                        || blockIn == worldIn.getBlockState(west.south()).getBlock();
                if (diagonalCrops) {
                    f /= 2.0F;
                }
            }
        }

        // halves growth chance at full darkness; doubles it at full light. on a sine curve.
        double light = worldIn.getLightFromNeighbors(pos.up()) / 15.0F;
        if (this.growthModifiers.contains(GrowthModifier.PREFERS_LIGHT)) {
            f *= Math.sin(light * Math.PI) * 1.5 + 0.5;
        }

        // halves growth chance at full light; doubles it at full darkness. on a sine curve.
        if (this.growthModifiers.contains(GrowthModifier.PREFERS_DARK)) {
            f *= Math.sin((1 - light) * Math.PI) * 1.5 + 0.5;
        }
        
        // a large constant boost to growth % when it's raining (in a biome that gets rain) and outdoors
        if (this.growthModifiers.contains(GrowthModifier.LIKES_RAIN)) {
            if (worldIn.isRainingAt(pos)) {
                f += 5.0F;
            }
        }

        return f;
    }
    
    /**
     * overriding the basic block data representation
     */
    @Override
    protected BlockStateContainer createBlockState() {
        final IProperty<?> props[];
        // ugh i'm sure there's a better way to do this but i don't know it.
        if (this.hasAge() && this.hasYield()) {
            props = new IProperty[] {this.age.getVal(), this.yield.getVal()};
        } else if (this.hasAge()) {
            props = new IProperty[] {this.age.getVal()};
        } else if (this.hasYield()) {
            props = new IProperty[] {this.yield.getVal()};
        } else {
            props = new IProperty[0];
        }
        return new BlockStateContainer(this, props);
    }

    private String showState(IBlockState state) {
        String age = "n";
        String yield = "n";
        String withered = "n";
        if (this.hasAge()) {
            age = String.format("%d", this.getAge(state));
        }
        if (this.hasYield()) {
            if (this.witherable) {
                withered = this.getWithered(state) ? "T" : "F";
                if (this.maxYield != 0) {
                    yield = "0";
                }
            } else {
                yield = String.format("%d", this.getYield(state));
            }
        }
        return "(" + age + "," + yield + "," + withered + ")";
    }

    public int getMetaFromState(IBlockState state) {
        int meta = 0;
        if (this.hasAge()) {
            meta |= state.getValue(this.age.getVal()) << this.age.getOffset();
        }
        if (this.hasYield()) {
            meta |= state.getValue(this.yield.getVal()) << this.yield.getOffset();
        }
        System.out.println("metadata from state " + this.showState(state) + ": " + String.format("0x%01x", meta));
        return meta & 0x0f; // hard limit metadata to 4 bits, even if the bitfields are busted
    }

    public IBlockState getStateFromMeta(int meta) {
        IBlockState state = this.getDefaultState();
        if (this.hasAge()) {
            state = state.withProperty(this.age.getVal(), this.age.readValue(meta));
        }
        if (this.hasYield()) {
            state = state.withProperty(this.yield.getVal(), this.yield.readValue(meta));
        }
        System.out.println("state from metadata " + String.format("0x%01x", meta) + ": " + this.showState(state));
        return state;
    }
}
