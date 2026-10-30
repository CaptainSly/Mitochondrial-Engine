package com.duckcraftian.upf.pe.script;

@FunctionalInterface
public interface Handler {

    boolean run(ScriptVM vm);

}