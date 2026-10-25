package com.mahghuuuls.setworks.core.eval.condition;

import com.mahghuuuls.setworks.api.ICondition;
import com.mahghuuuls.setworks.api.IConditionContext;
import com.mahghuuuls.setworks.api.IConditionKind;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.Map;

/** Built-in condition: the player's health is below a fraction of the maximum ({@code fraction}, 0 exclusive to 1 inclusive). */
public final class HealthBelowKind implements IConditionKind {

    public static final String ID = "setworks:health_below";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String validate(Map<String, String> params) {
        try {
            parseFraction(params);
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    @Override
    public ICondition create(Map<String, String> params) {
        final double fraction = parseFraction(params);
        return new ICondition() {
            @Override
            public boolean test(EntityPlayerMP player, IConditionContext context) {
                return isBelow(player.getHealth(), player.getMaxHealth(), fraction);
            }
        };
    }

    @Override
    public String describe(Map<String, String> params) {
        try {
            return "health below " + Math.round(parseFraction(params) * 100) + "%";
        } catch (IllegalArgumentException e) {
            return "health below (invalid: " + e.getMessage() + ")";
        }
    }

    /** True when health is strictly below the fraction of the maximum. */
    public static boolean isBelow(float health, float maxHealth, double fraction) {
        return health < maxHealth * fraction;
    }

    /** Parses {@code fraction} or throws {@link IllegalArgumentException} with the problem. */
    public static double parseFraction(Map<String, String> params) {
        String text = params.get("fraction");
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("fraction is missing (0 to 1, for example 0.5 for half health)");
        }
        double fraction;
        try {
            fraction = Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("fraction " + text + " is not a number");
        }
        if (!(fraction > 0.0 && fraction <= 1.0)) {
            throw new IllegalArgumentException("fraction must be above 0 and at most 1, not " + text);
        }
        return fraction;
    }
}
