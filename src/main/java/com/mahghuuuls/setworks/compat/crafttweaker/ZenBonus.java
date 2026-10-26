package com.mahghuuuls.setworks.compat.crafttweaker;

import com.mahghuuuls.setworks.api.BonusBuilder;
import com.mahghuuuls.setworks.api.DefinitionSource;
import com.mahghuuuls.setworks.api.ICondition;
import com.mahghuuuls.setworks.api.IConditionContext;
import com.mahghuuuls.setworks.api.RegistrationResult;
import com.mahghuuuls.setworks.api.SetworksApi;
import com.mahghuuuls.setworks.api.Visibility;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.entity.player.EntityPlayerMP;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.Map;

/**
 * A bonus being defined from a script. Created by {@code Setworks.bonus(id)}, {@code Setworks.replaceBonus(id)},
 * or a helper; registered when the definitions freeze with whatever was added to it by then. A bonus
 * without effects is a scripted bonus: Setworks applies nothing for it and scripts read its state
 * through the player queries and the events.
 */
@ZenRegister
@ZenClass("mods.setworks.Bonus")
public final class ZenBonus {

    private final String id;
    private final BonusBuilder builder;
    private int functionConditions;

    ZenBonus(final String id, final boolean replace, final DefinitionSource source) {
        this.id = id;
        this.builder = BonusBuilder.bonus(id);
        ScriptQueue.get().enqueue(new ScriptQueue.Entry() {
            @Override
            public String describe() {
                return (replace ? "replaceBonus " : "bonus ") + id + " (" + source.describe() + ")";
            }

            @Override
            public RegistrationResult apply() {
                return replace ? SetworksApi.replaceBonus(builder.build(), source) : SetworksApi.registerBonus(builder, source);
            }
        });
    }

    /** Needs at least {@code threshold} pieces of the set. */
    @ZenMethod
    public ZenBonus requires(String setId, int threshold) {
        builder.requires(setId, threshold);
        return this;
    }

    /** Needs every piece of the set. */
    @ZenMethod
    public ZenBonus requiresAll(String setId) {
        builder.requiresAll(setId);
        return this;
    }

    /** The tooltip text, plain or a language key. Required for a bonus without effects. */
    @ZenMethod
    public ZenBonus description(String text) {
        builder.description(text);
        return this;
    }

    /** {@code "visible"} (default), {@code "discovered"} (shown once the player had it active), or {@code "hidden"} (never shown). */
    @ZenMethod
    public ZenBonus visibility(String mode) {
        builder.visibility(Names.visibility(mode));
        return this;
    }

    @ZenMethod
    public ZenBonus hidden() {
        builder.visibility(Visibility.HIDDEN);
        return this;
    }

    @ZenMethod
    public ZenBonus discovered() {
        builder.visibility(Visibility.DISCOVERED);
        return this;
    }

    /** Shows a multi-set bonus only under these sets (default: under every required set). */
    @ZenMethod
    public ZenBonus displayUnder(String[] setIds) {
        builder.displayUnder(setIds);
        return this;
    }

    @ZenMethod
    public ZenBonus displayUnder(String setId) {
        builder.displayUnder(setId);
        return this;
    }

    /** How often the conditions are rechecked, in ticks (default: the global setting). */
    @ZenMethod
    public ZenBonus conditionInterval(int ticks) {
        builder.conditionInterval(ticks);
        return this;
    }

    /** A registered condition kind with its parameters, for example {@code "setworks:health_below"} with {@code {fraction: "0.5"}}. */
    @ZenMethod
    public ZenBonus condition(String kindId, Map<String, String> params) {
        builder.condition(kindId, Names.params(params));
        return this;
    }

    /** A registered condition kind without parameters, for example {@code "setworks:sneaking"}. */
    @ZenMethod
    public ZenBonus condition(String kindId) {
        builder.condition(kindId, Names.params(null));
        return this;
    }

    /** A script function as condition; the label names it in {@code inspect} and the log. */
    @ZenMethod
    public ZenBonus whenLabelled(String label, final IZenCondition function) {
        if (function == null) {
            throw new IllegalArgumentException("condition function must not be null");
        }
        builder.condition(label, new ICondition() {
            @Override
            public boolean test(EntityPlayerMP player, IConditionContext context) {
                return function.test(CraftTweakerMC.getIPlayer(player));
            }
        });
        return this;
    }

    /** A script function as condition, labelled {@code script function N}. */
    @ZenMethod
    public ZenBonus when(IZenCondition function) {
        functionConditions++;
        return whenLabelled("script function " + functionConditions, function);
    }

    /** An effect of a registered kind with its parameters. */
    @ZenMethod
    public ZenBonus effect(String kindId, Map<String, String> params) {
        builder.effect(kindId, Names.params(params));
        return this;
    }

    /** An attribute modifier: {@code "generic.armor"}, amount, and {@code "add"}, {@code "multiply_base"}, or {@code "multiply_total"}. */
    @ZenMethod
    public ZenBonus attribute(String attributeName, double amount, String operation) {
        builder.attribute(attributeName, amount, operation);
        return this;
    }

    /** A potion kept on the player while the bonus is active; level is 1-based; particles default to off. */
    @ZenMethod
    public ZenBonus potion(String potionId, int level, @Optional boolean particles) {
        builder.potion(potionId, level, particles);
        return this;
    }

    @ZenMethod
    public String getId() {
        return id;
    }
}
