package com.mahghuuuls.setworks.core.location;

import com.mahghuuuls.setworks.api.DefinitionSource;
import com.mahghuuuls.setworks.api.ILocation;
import com.mahghuuuls.setworks.api.RegistrationResult;
import com.mahghuuuls.setworks.api.SlotStack;
import net.minecraft.entity.player.EntityPlayer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** An addon location registers by id with its source, a second owner of the id is refused, and the listing keeps order and sources. */
class LocationRegistryTest {

    private static ILocation location(final String id, final DefinitionSource source) {
        return new ILocation() {
            public String id() {
                return id;
            }

            public DefinitionSource source() {
                return source;
            }

            public List<SlotStack> read(EntityPlayer player) {
                return Collections.emptyList();
            }
        };
    }

    @Test
    void registersAddonLocationsAndRefusesADoubleExposure() {
        LocationRegistry registry = new LocationRegistry();
        DefinitionSource builtin = DefinitionSource.builtin("setworks:vanilla");
        DefinitionSource baubles = DefinitionSource.builtin("setworks:baubles");
        DefinitionSource addon = DefinitionSource.mod("rings");
        assertTrue(registry.register(location("head", builtin)).isOk());
        assertTrue(registry.register(location("baubles:ring", baubles)).isOk());
        RegistrationResult twice = registry.register(location("baubles:ring", addon));
        assertFalse(twice.isOk());
        assertEquals("location baubles:ring is already registered by setworks:baubles", twice.message());
        assertFalse(registry.register(location("  ", addon)).isOk(), "an id is required");
        assertFalse(registry.register(null).isOk());
        assertTrue(registry.exists("baubles:ring"));
        assertFalse(registry.exists("baubles:trinket"));
        assertEquals(Arrays.asList("head", "baubles:ring"), new ArrayList<String>(registry.ids()), "registration order kept for the listing");
        List<String> listing = new ArrayList<String>();
        for (ILocation location : registry.all()) {
            listing.add(location.id() + " source=" + location.source().describe());
        }
        assertEquals(Arrays.asList("head source=setworks:vanilla", "baubles:ring source=setworks:baubles"), listing);
        registry.freeze();
        RegistrationResult late = registry.register(location("rings:toe", addon));
        assertFalse(late.isOk(), "after the freeze nothing is added");
        assertEquals("locations are frozen; rings:toe was ignored", late.message());
        assertEquals(2, registry.all().size());
    }
}
