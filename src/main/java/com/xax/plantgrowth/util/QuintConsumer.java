package com.xax.plantgrowth.util;

@FunctionalInterface
public interface QuintConsumer<A,B,C,D,E> {
    void accept(A a, B b, C c, D d, E e);
}
