package com.duckcraftian.upf.system.event;

public abstract class Event {

    private String eventId;
    private long timestamp;

    public Event(String eventId, long timestamp) {
        this.eventId = eventId;
        this.timestamp = timestamp;
    }

    public String getEventId() {
        return eventId;
    }

    public long getTimestamp() {
        return timestamp;
    }
}