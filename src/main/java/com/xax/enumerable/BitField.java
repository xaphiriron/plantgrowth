package com.xax.enumerable;

public class BitField<T> {
    private int bitsUsed;
    private int mask;
    private int offset;
    private final T val;
    private final static int bitCount[] = {2,4,8,16,32,64,128,256};
    
    public BitField (T t, int max) {
        this.val = t;
        init (max, 0);
    }
    public BitField (T t, int max, int offset) {
        this.val = t;
        init (max, offset);
    }
    private void init(int max, int offset) {
        int p = this.nextPowerOf2(max);
        this.mask = (p - 1) << offset;
        this.offset = offset;
        this.bitsUsed = 0;
        // this isn't the most efficient way probably but i can't remember the constant form of converting a power of two into a count. i mean, log 2 i guess, but...
        for (int b : bitCount) {
            this.bitsUsed++;
            if (b >= p) {
                break;
            }
        }
    }
    
    public int readValue (int meta) {
        return (meta & this.mask) >> this.offset;
    }
    
    public int getMask () {
        return this.mask;
    }
    public int getUsedBits() {
        return this.bitsUsed;
    }
    public T getVal() {
        return this.val;
    }
    public int getOffset() {
        return this.offset;
    }
    
    private int nextPowerOf2 (int v) {
        int n = 2;
        while (n < v) {
            n = n << 1;
        }
        return n;
    }
}
