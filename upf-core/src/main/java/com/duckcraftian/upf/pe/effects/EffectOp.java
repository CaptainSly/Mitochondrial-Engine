package com.duckcraftian.upf.pe.effects;

@FunctionalInterface
public interface EffectOp {

    boolean run(int slot, int a, int b);

}