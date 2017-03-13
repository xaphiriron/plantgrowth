package com.xax.plantgrowth.util;

@FunctionalInterface
public interface QuadPredicate<A,B,C,D> {
    boolean test(A a, B b, C c, D d);
}
