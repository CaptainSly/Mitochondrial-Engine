package com.duckcraftian.upf.system.event;

@FunctionalInterface
public interface EventListener<E extends Event> {

     void onEvent(E event);

}