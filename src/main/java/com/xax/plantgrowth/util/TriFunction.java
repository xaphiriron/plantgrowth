package com.xax.plantgrowth.util;

public interface TriFunction<A,B,C,D> {
    D apply(A a, B b, C c);
}
