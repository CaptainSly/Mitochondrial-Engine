package com.duckcraftian.upf.system.registries;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Registry<T> {

    private Map<String, T> registry;

    public Registry() {
        registry = new HashMap<>();
    }

    public T get(String id) {
        if (!registry.containsKey(id))
            throw new IllegalArgumentException("Registry does not contain key: " + id);

        return registry.get(id);
    }

    public void put(String id, T value) {
        if (registry.containsKey(id))
            throw new IllegalArgumentException("The Registry already contains the key: " + id);

        registry.put(id, value);
    }

    public void replace(String id, T value) {
        if (!registry.containsKey(id))
            throw new IllegalArgumentException("The Registry does not contain key: " + id);

        registry.put(id, value);
    }

    public Map<String, T> getRegistry() {
        return Collections.unmodifiableMap(registry);
    }

}