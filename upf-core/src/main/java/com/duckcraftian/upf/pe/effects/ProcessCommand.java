package com.duckcraftian.upf.pe.effects;

import com.duckcraftian.upf.pe.script.ScriptVM;

public interface ProcessCommand {

    void init();

    int initialize(int slot);

    int send(int slot, int arg1, int arg2, int arg3, int arg4, int arg5, ScriptVM caller);

    int start(int slot);

    int execute(int slot);

    int stop(int slot);

    void unload();

}