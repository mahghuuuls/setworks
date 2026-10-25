package com.mahghuuuls.setworks.core.effect;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Stable identities for effect instances. The instance id names the set, the bonus, and the
 * effect's position in the bonus, so the same definition yields the same id in every session;
 * the attribute UUID is derived from it, so a modifier can be found again after a rebuild.
 */
public final class EffectIds {

    private static final String UUID_NAMESPACE = "setworks:effect:";

    private EffectIds() {
    }

    /** {@code <first required set>/<bonus>/<index>}, for example {@code setworks:iron_armor/setworks:iron_resolve/0}. */
    public static String instanceId(String setId, String bonusId, int index) {
        return setId + "/" + bonusId + "/" + index;
    }

    /** A name-based UUID of the instance id; equal ids give equal UUIDs, different ids different ones. */
    public static UUID attributeUuid(String instanceId) {
        return UUID.nameUUIDFromBytes((UUID_NAMESPACE + instanceId).getBytes(StandardCharsets.UTF_8));
    }
}
