package com.duckcraftian.upf.system.event;

public record RegisteredListener(int priority, EventListener<? extends Event> listener, boolean ignoreCancelled) {


}