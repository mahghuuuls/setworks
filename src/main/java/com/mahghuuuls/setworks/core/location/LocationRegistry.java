package com.mahghuuuls.setworks.core.location;

import com.mahghuuuls.setworks.api.ILocation;
import com.mahghuuuls.setworks.api.RegistrationResult;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Every location Setworks knows, by id. Built-ins first, then addon registrations during startup. */
public final class LocationRegistry {

    private final Map<String, ILocation> locations = new LinkedHashMap<String, ILocation>();
    private volatile boolean frozen;

    public RegistrationResult register(ILocation location) {
        if (location == null || location.id() == null || location.id().trim().isEmpty()) {
            return RegistrationResult.error("location has no id");
        }
        if (frozen) {
            return RegistrationResult.error("locations are frozen; " + location.id() + " was ignored");
        }
        ILocation existing = locations.get(location.id());
        if (existing != null) {
            return RegistrationResult.error("location " + location.id() + " is already registered by "
                    + existing.source().describe());
        }
        locations.put(location.id(), location);
        return RegistrationResult.ok();
    }

    public void freeze() {
        frozen = true;
    }

    public boolean exists(String id) {
        return locations.containsKey(id);
    }

    public ILocation get(String id) {
        return locations.get(id);
    }

    public Collection<ILocation> all() {
        return Collections.unmodifiableCollection(locations.values());
    }

    public Collection<String> ids() {
        return Collections.unmodifiableCollection(locations.keySet());
    }
}
