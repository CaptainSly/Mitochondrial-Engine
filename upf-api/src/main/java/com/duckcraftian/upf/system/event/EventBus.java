package com.duckcraftian.upf.system.event;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {

    private final Map<Class<? extends Event>, List<RegisteredListener>> listeners;

    public EventBus() {
        listeners = new LinkedHashMap<>();
    }

    public <E extends Event> void subscribe(Class<E> eventClass, EventListener<E> listener, int priority, boolean ignoreCancelled) {
        List<RegisteredListener> registeredListeners = listeners.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>());
        RegisteredListener regListener = new RegisteredListener(priority, listener, ignoreCancelled);
        registeredListeners.add(regListener);
        registeredListeners.sort(Comparator.comparingInt(RegisteredListener::priority).reversed());
    }

    public <E extends Event> void unsubscribe(Class<E> eventClass, EventListener<E> listener) {
        List<RegisteredListener> registeredListeners = listeners.get(eventClass);
        if (registeredListeners == null) return;
        if (registeredListeners.isEmpty()) return;

        registeredListeners.removeIf(registered -> registered.listener() == listener);
    }

    public void fire(Event event) {
        List<RegisteredListener> registeredListeners = listeners.getOrDefault(event.getClass(), new CopyOnWriteArrayList<>());
        for (RegisteredListener listener : registeredListeners) {
            if (!(event instanceof CancellableEvent cancellableEvent && cancellableEvent.isCancelled() && listener.ignoreCancelled() == false))
                ((EventListener<Event>) listener.listener()).onEvent(event);
        }
    }

}