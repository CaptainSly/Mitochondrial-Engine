package com.duckcraftian.upf.system.event;

public class CancellableEvent extends Event {

    private boolean cancelled;

    public CancellableEvent(String eventId, long timestamp) {
        super(eventId, timestamp);
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;

    }

    public boolean isCancelled() {
        return cancelled;
    }

}