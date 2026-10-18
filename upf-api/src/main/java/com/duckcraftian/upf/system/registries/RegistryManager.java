package com.duckcraftian.upf.system.registries;

import java.util.HashMap;
import java.util.Map;

/**
 * ID SCHEMA is as such: </p>
 * <p>- `namespace:registryId:objectId` <- For accessing Registry Contents </p>
 * <p>- `namespace:registryId` <- for accessing only Registries.</p>
 */
public class RegistryManager {

    private Map<String, Map<String, Registry<?>>> registryHopper;

    public RegistryManager() {
        registryHopper = new HashMap<>();
    }

    public void createNamespace(String namespace) {
        if (registryHopper.containsKey(namespace))
            throw new IllegalArgumentException("The namespace: " + namespace + " already exists");

        registryHopper.put(namespace, new HashMap<>());
    }

    public Map<String, Registry<?>> getNamespace(String namespace) {
        if (!registryHopper.containsKey(namespace))
            throw new IllegalArgumentException("The namespace: " + namespace + " does not exist");

        return registryHopper.get(namespace);
    }

    public void createRegistry(String idSchema, Registry<?> registry) {
        String[] id = idSchema.split(":");
        String namespace = id[0];
        String registryId = id[1];

        createRegistry(namespace, registryId, registry);
    }

    public void createRegistry(String namespace, String registryId, Registry<?> registry) {
        Map<String, Registry<?>> _namespace = getNamespace(namespace);
        if (_namespace.containsKey(registryId))
            throw new IllegalArgumentException("The Registry : " + registryId + " already exists in the namespace: " + namespace);

        _namespace.put(registryId, registry);
    }

    public Registry<?> getRegistry(String idSchema) {
        String[] id = idSchema.split(":");
        String namespace = id[0];
        String registryId = id[1];

        return getRegistry(namespace, registryId);
    }

    public Registry<?> getRegistry(String namespace, String registryId) {
        Map<String, Registry<?>> _namespace = getNamespace(namespace);
        if (!_namespace.containsKey(registryId))
            throw new IllegalArgumentException("The namespace: " + namespace + " does not contain a registry: " + registryId);

        return _namespace.get(registryId);
    }

}