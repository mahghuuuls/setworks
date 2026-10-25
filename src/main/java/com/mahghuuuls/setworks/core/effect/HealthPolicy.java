package com.mahghuuuls.setworks.core.effect;

/**
 * What happens to current health when the maximum changes under an effect: a raised maximum
 * leaves current health where it is (no free healing), a lowered maximum clamps current health
 * to it. The same rule is documented for addon kinds that change other resource maximums.
 */
public final class HealthPolicy {

    private HealthPolicy() {
    }

    /** The health the player must have after the maximum changed: unchanged when it fits, the maximum when it exceeds it. */
    public static float healthAfterMaximumChange(float health, float maxHealth) {
        return health > maxHealth ? maxHealth : health;
    }

    /** True when current health exceeds the maximum and must be clamped. */
    public static boolean needsClamp(float health, float maxHealth) {
        return health > maxHealth;
    }
}
