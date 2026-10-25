package com.mahghuuuls.setworks.core.effect;

import java.util.Map;

/**
 * The shape of a potion effect Setworks owns: 300 ticks, renewed every 20 ticks, not ambient. A
 * potion effect has no owner field, so on withdrawal Setworks removes the player's effect only
 * when it still looks like its own: same amplifier and at most 300 ticks left. A longer effect, a
 * drunk potion for example, is left alone; a Setworks effect whose removal is ever missed expires
 * by itself. Between refreshes the duration stays above 200 ticks, so the HUD does not blink.
 */
public final class PotionSignature {

    public static final int DURATION_TICKS = 300;
    public static final int REFRESH_INTERVAL_TICKS = 20;

    private PotionSignature() {
    }

    /** True when the player's current effect is the one Setworks applied and may be removed. */
    public static boolean shouldRemove(int ownAmplifier, int currentAmplifier, int currentDuration) {
        return currentAmplifier == ownAmplifier && currentDuration <= DURATION_TICKS;
    }

    /** Parsed potion parameters: {@code potion}, {@code level} (1-based), {@code particles}. */
    public static final class Params {
        private final String potionId;
        private final int level;
        private final boolean particles;

        Params(String potionId, int level, boolean particles) {
            this.potionId = potionId;
            this.level = level;
            this.particles = particles;
        }

        public String potionId() {
            return potionId;
        }

        public int level() {
            return level;
        }

        /** The vanilla amplifier: level minus one. */
        public int amplifier() {
            return level - 1;
        }

        public boolean particles() {
            return particles;
        }

        public String describe() {
            return potionId + " " + level + (particles ? " with particles" : "");
        }
    }

    /** Parses the parameters or throws {@link IllegalArgumentException} with the problem. */
    public static Params parse(Map<String, String> params) {
        String potion = params.get("potion");
        if (potion == null || potion.trim().isEmpty()) {
            throw new IllegalArgumentException("potion is missing");
        }
        String levelText = params.get("level");
        int level;
        try {
            level = levelText == null ? 1 : Integer.parseInt(levelText.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("level " + levelText + " is not a whole number");
        }
        if (level < 1) {
            throw new IllegalArgumentException("level must be 1 or more, not " + level);
        }
        String particlesText = params.get("particles");
        boolean particles;
        if (particlesText == null || particlesText.equalsIgnoreCase("false")) {
            particles = false;
        } else if (particlesText.equalsIgnoreCase("true")) {
            particles = true;
        } else {
            throw new IllegalArgumentException("particles must be true or false, not " + particlesText);
        }
        return new Params(potion.trim(), level, particles);
    }

    /** The problem with the parameters, or {@code null} when they parse. */
    public static String problem(Map<String, String> params) {
        try {
            parse(params);
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }
}
