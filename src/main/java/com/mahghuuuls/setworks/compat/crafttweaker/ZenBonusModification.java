package com.mahghuuuls.setworks.compat.crafttweaker;

import com.mahghuuuls.setworks.api.BonusModification;
import com.mahghuuuls.setworks.api.DefinitionSource;
import com.mahghuuuls.setworks.api.RegistrationResult;
import com.mahghuuuls.setworks.api.SetworksApi;
import com.mahghuuuls.setworks.api.Visibility;
import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * Changes to an existing bonus (from a mod, the examples, or another script): description,
 * visibility, display sets. Created by {@code Setworks.modifyBonus(id)}; applied when the definitions
 * freeze, after every registration.
 */
@ZenRegister
@ZenClass("mods.setworks.BonusModification")
public final class ZenBonusModification {

    private final BonusModification modification = BonusModification.modify();

    ZenBonusModification(final String id, final DefinitionSource source) {
        ScriptQueue.get().enqueue(new ScriptQueue.Entry() {
            @Override
            public String describe() {
                return "modifyBonus " + id + " (" + source.describe() + ")";
            }

            @Override
            public RegistrationResult apply() {
                return SetworksApi.modifyBonus(id, modification, source);
            }
        });
    }

    @ZenMethod
    public ZenBonusModification description(String text) {
        modification.description(text);
        return this;
    }

    /** {@code "visible"}, {@code "discovered"}, or {@code "hidden"}. */
    @ZenMethod
    public ZenBonusModification visibility(String mode) {
        modification.visibility(Names.visibility(mode));
        return this;
    }

    @ZenMethod
    public ZenBonusModification hidden() {
        modification.visibility(Visibility.HIDDEN);
        return this;
    }

    @ZenMethod
    public ZenBonusModification displayUnder(String[] setIds) {
        modification.displayUnder(setIds);
        return this;
    }
}
