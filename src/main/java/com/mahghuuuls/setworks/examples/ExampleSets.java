package com.mahghuuuls.setworks.examples;

import com.mahghuuuls.setworks.api.BonusBuilder;
import com.mahghuuuls.setworks.api.DefinitionSource;
import com.mahghuuuls.setworks.api.ItemFilter;
import com.mahghuuuls.setworks.api.PieceBuilder;
import com.mahghuuuls.setworks.api.SetBuilder;
import com.mahghuuuls.setworks.api.SetworksApi;
import com.mahghuuuls.setworks.api.ThresholdMode;
import com.mahghuuuls.setworks.api.Visibility;

import java.util.Collections;

/**
 * The example definitions that ship with Setworks, registered only when {@code enableExampleSets}
 * is true. Vanilla items and vanilla attributes and potions only. They demonstrate the authoring
 * surface; packs copy and change them.
 */
public final class ExampleSets {

    public static final DefinitionSource SOURCE = DefinitionSource.builtin("setworks:examples");

    /** System property that adds development fixtures: deliberately broken definitions and the {@link DevFixtures}. */
    public static final String INVALID_EXAMPLE_PROPERTY = "setworks.invalidExample";

    private ExampleSets() {
    }

    public static void register() {
        // A four-piece armor set with a partial and a full bonus. Both add armor, so with three or
        // more pieces the two modifiers sum to +4.
        SetworksApi.registerSet(SetBuilder.set("setworks:iron_armor")
                .displayName("Iron Armor")
                .armor(ItemFilter.item("minecraft:iron_helmet"), ItemFilter.item("minecraft:iron_chestplate"),
                        ItemFilter.item("minecraft:iron_leggings"), ItemFilter.item("minecraft:iron_boots")), SOURCE);
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:iron_resolve")
                .requires("setworks:iron_armor", 2)
                .attribute("generic.armor", 2, "add")
                .description("Iron Resolve: +2 armor"), SOURCE);
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:iron_wall")
                .requiresAll("setworks:iron_armor")
                .attribute("generic.maxHealth", 4, "add")
                .potion("minecraft:speed", 1, false)
                .description("Iron Wall: +4 max health and Speed I"), SOURCE);

        // Conditioned bonuses: a condition is checked right after an equipment change and otherwise on
        // the interval (the global 20 ticks, or the bonus's own 5 ticks for Iron Second Wind).
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:iron_last_stand")
                .requires("setworks:iron_armor", 2)
                .condition("setworks:health_below", Collections.singletonMap("fraction", "0.5"))
                .attribute("generic.attackDamage", 4, "add")
                .description("Iron Last Stand: +4 attack damage below half health"), SOURCE);
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:iron_second_wind")
                .requiresAll("setworks:iron_armor")
                .condition("setworks:health_below", Collections.singletonMap("fraction", "0.5"))
                .conditionInterval(5)
                .potion("minecraft:regeneration", 1, false)
                .description("Iron Second Wind: Regeneration I below half health"), SOURCE);
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:iron_stance")
                .requires("setworks:iron_armor", 2)
                .condition("setworks:sneaking", Collections.<String, String>emptyMap())
                .attribute("generic.knockbackResistance", 1, "add")
                .description("Iron Stance: no knockback while sneaking"), SOURCE);

        // A weapon set and a bonus that needs the full armor set plus any weapon of it.
        SetworksApi.registerSet(SetBuilder.set("setworks:iron_weapons")
                .displayName("Iron Weapons")
                .piece(PieceBuilder.piece("weapon")
                        .accept("minecraft:iron_sword").accept("minecraft:iron_axe")
                        .at("mainhand")), SOURCE);
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:iron_strike")
                .requiresAll("setworks:iron_armor")
                .requires("setworks:iron_weapons", 1)
                .attribute("generic.attackDamage", 2, "add")
                .description("Iron Strike: +2 attack damage while holding an iron weapon")
                .displayUnder("setworks:iron_armor"), SOURCE);

        // Alternatives: iron or chain pieces fill the same named piece.
        SetworksApi.registerSet(SetBuilder.set("setworks:mixed_mail")
                .displayName("Mixed Mail")
                .piece(PieceBuilder.piece("head").accept("minecraft:iron_helmet").accept("minecraft:chainmail_helmet").at("head"))
                .piece(PieceBuilder.piece("chest").accept("minecraft:iron_chestplate").accept("minecraft:chainmail_chestplate").at("chest"))
                .piece(PieceBuilder.piece("legs").accept("minecraft:iron_leggings").accept("minecraft:chainmail_leggings").at("legs"))
                .piece(PieceBuilder.piece("feet").accept("minecraft:iron_boots").accept("minecraft:chainmail_boots").at("feet")), SOURCE);
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:mixed_mail_full")
                .requiresAll("setworks:mixed_mail")
                .attribute("generic.knockbackResistance", 0.5, "add")
                .description("Mixed Mail: half knockback; any iron or chain piece counts"), SOURCE);

        // A carried item in an explicitly selected location.
        SetworksApi.registerSet(SetBuilder.set("setworks:wayfinder")
                .displayName("Wayfinder")
                .piece(PieceBuilder.piece("compass").accept("minecraft:compass").at("hotbar")), SOURCE);
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:wayfinder_sense")
                .requiresAll("setworks:wayfinder")
                .potion("minecraft:night_vision", 1, false)
                .description("Wayfinder: night vision while a compass is in the hotbar"), SOURCE);

        // Highest-only mode with two thresholds; the full-set speed replaces the two-piece speed.
        SetworksApi.registerSet(SetBuilder.set("setworks:leather_focus")
                .displayName("Leather Focus")
                .thresholdMode(ThresholdMode.HIGHEST_ONLY)
                .armor(ItemFilter.item("minecraft:leather_helmet"), ItemFilter.item("minecraft:leather_chestplate"),
                        ItemFilter.item("minecraft:leather_leggings"), ItemFilter.item("minecraft:leather_boots")), SOURCE);
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:leather_quick")
                .requires("setworks:leather_focus", 2)
                .attribute("generic.movementSpeed", 0.1, "multiply_total")
                .description("Leather Quick: 10% faster"), SOURCE);
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:leather_silent")
                .requiresAll("setworks:leather_focus")
                .attribute("generic.movementSpeed", 0.2, "multiply_total")
                .description("Leather Silent: 20% faster, replaces Leather Quick when all four are worn"), SOURCE);

        // A hidden scripted bonus and a discovered bonus on the iron set.
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:iron_secret")
                .requiresAll("setworks:iron_armor")
                .visibility(Visibility.HIDDEN)
                .description("Iron Secret: never shown"), SOURCE);
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:iron_memory")
                .requires("setworks:iron_armor", 3)
                .visibility(Visibility.DISCOVERED)
                .attribute("generic.armor", 2, "add")
                .description("Iron Memory: +2 more armor, shown once you have had three pieces on"), SOURCE);

        // Metadata and ore dictionary filters.
        SetworksApi.registerSet(SetBuilder.set("setworks:blue_banner")
                .displayName("Blue Banner")
                .piece(PieceBuilder.piece("wool").accept(ItemFilter.item("minecraft:wool", 11)).at("offhand"))
                .piece(PieceBuilder.piece("ingot").accept(ItemFilter.ore("ingotIron")).at("hotbar")), SOURCE);
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:blue_banner_full")
                .requiresAll("setworks:blue_banner")
                .potion("minecraft:luck", 1, false)
                .description("Blue Banner: Luck I with blue wool in the off hand and an iron ingot in the hotbar"), SOURCE);

        if (Boolean.getBoolean(INVALID_EXAMPLE_PROPERTY)) {
            SetworksApi.registerSet(SetBuilder.set("setworks:broken_example")
                    .displayName("Broken Example")
                    .piece(PieceBuilder.piece("head").accept("minecraft:iron_helmet").at("head"))
                    .piece(PieceBuilder.piece("chest").accept("minecraft:iron_chestplate").at("chest"))
                    .piece(PieceBuilder.piece("legs").accept("minecraft:no_such_item").at("legs")), SOURCE);
            SetworksApi.registerBonus(BonusBuilder.bonus("setworks:broken_example_full")
                    .requiresAll("setworks:broken_example")
                    .description("Broken Example: should never be active"), SOURCE);
            // An effect with an unknown operation: rejected at validation; its own one-piece set is disabled with it.
            SetworksApi.registerSet(SetBuilder.set("setworks:broken_effect_set")
                    .displayName("Broken Effect Set")
                    .piece(PieceBuilder.piece("head").accept("minecraft:golden_helmet").at("head")), SOURCE);
            SetworksApi.registerBonus(BonusBuilder.bonus("setworks:broken_effect")
                    .requiresAll("setworks:broken_effect_set")
                    .attribute("generic.armor", 1, "times")
                    .description("Broken Effect: should never be active"), SOURCE);
            // A condition with an impossible fraction: rejected at validation; its own one-piece set is disabled with it.
            SetworksApi.registerSet(SetBuilder.set("setworks:broken_condition_set")
                    .displayName("Broken Condition Set")
                    .piece(PieceBuilder.piece("chest").accept("minecraft:golden_chestplate").at("chest")), SOURCE);
            SetworksApi.registerBonus(BonusBuilder.bonus("setworks:broken_condition")
                    .requiresAll("setworks:broken_condition_set")
                    .condition("setworks:health_below", Collections.singletonMap("fraction", "2"))
                    .description("Broken Condition: should never be active"), SOURCE);
            DevFixtures.register();
        }
    }
}
