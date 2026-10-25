package com.mahghuuuls.setworks.core.eval.condition;

import com.mahghuuuls.setworks.api.ICondition;
import com.mahghuuuls.setworks.api.IConditionContext;
import com.mahghuuuls.setworks.api.IConditionKind;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.Map;

/** Built-in condition: the player is sneaking. No parameters. */
public final class SneakingKind implements IConditionKind {

    public static final String ID = "setworks:sneaking";

    private static final ICondition SNEAKING = new ICondition() {
        @Override
        public boolean test(EntityPlayerMP player, IConditionContext context) {
            return player.isSneaking();
        }
    };

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String validate(Map<String, String> params) {
        return params.isEmpty() ? null : "sneaking takes no parameters, got " + params.keySet();
    }

    @Override
    public ICondition create(Map<String, String> params) {
        return SNEAKING;
    }

    @Override
    public String describe(Map<String, String> params) {
        return "sneaking";
    }
}
