package com.mahghuuuls.setworks.examples;

import com.mahghuuuls.setworks.api.BonusBuilder;
import com.mahghuuuls.setworks.api.DefinitionSource;
import com.mahghuuuls.setworks.api.ItemFilter;
import com.mahghuuuls.setworks.api.PieceBuilder;
import com.mahghuuuls.setworks.api.SetBuilder;
import com.mahghuuuls.setworks.api.SetworksApi;
import com.mahghuuuls.setworks.api.ThresholdMode;
import com.mahghuuuls.setworks.api.Visibility;

/**
 * The example definitions that ship with Setworks, registered only when {@code enableExampleSets}
 * is true. Vanilla items only. They demonstrate the authoring surface; packs copy and change them.
 */
public final class ExampleSets {

    public static final DefinitionSource SOURCE = DefinitionSource.builtin("setworks:examples");

    /** System property that adds one deliberately broken set, for development checks of the validate command. */
    public static final String INVALID_EXAMPLE_PROPERTY = "setworks.invalidExample";

    private ExampleSets() {
    }

    public static void register() {
        // A four-piece armor set with a partial and a full bonus.
        SetworksApi.registerSet(SetBuilder.set("setworks:iron_armor")
                .displayName("Iron Armor")
                .armor(ItemFilter.item("minecraft:iron_helmet"), ItemFilter.item("minecraft:iron_chestplate"),
                        ItemFilter.item("minecraft:iron_leggings"), ItemFilter.item("minecraft:iron_boots")), SOURCE);
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:iron_resolve")
                .requires("setworks:iron_armor", 2)
                .description("Iron Resolve: you feel sturdier"), SOURCE);
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:iron_wall")
                .requiresAll("setworks:iron_armor")
                .description("Iron Wall: you feel unbreakable"), SOURCE);

        // A weapon set and a bonus that needs the full armor set plus any weapon of it.
        SetworksApi.registerSet(SetBuilder.set("setworks:iron_weapons")
                .displayName("Iron Weapons")
                .piece(PieceBuilder.piece("weapon")
                        .accept("minecraft:iron_sword").accept("minecraft:iron_axe")
                        .at("mainhand")), SOURCE);
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:iron_strike")
                .requiresAll("setworks:iron_armor")
                .requires("setworks:iron_weapons", 1)
                .description("Iron Strike: your iron weapons hit harder")
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
                .description("Mixed Mail: any iron or chain piece counts"), SOURCE);

        // A carried item in an explicitly selected location.
        SetworksApi.registerSet(SetBuilder.set("setworks:wayfinder")
                .displayName("Wayfinder")
                .piece(PieceBuilder.piece("compass").accept("minecraft:compass").at("hotbar")), SOURCE);
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:wayfinder_sense")
                .requiresAll("setworks:wayfinder")
                .description("Wayfinder: a compass in the hotbar"), SOURCE);

        // Highest-only mode with two thresholds.
        SetworksApi.registerSet(SetBuilder.set("setworks:leather_focus")
                .displayName("Leather Focus")
                .thresholdMode(ThresholdMode.HIGHEST_ONLY)
                .armor(ItemFilter.item("minecraft:leather_helmet"), ItemFilter.item("minecraft:leather_chestplate"),
                        ItemFilter.item("minecraft:leather_leggings"), ItemFilter.item("minecraft:leather_boots")), SOURCE);
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:leather_quick")
                .requires("setworks:leather_focus", 2)
                .description("Leather Quick: light on your feet"), SOURCE);
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:leather_silent")
                .requiresAll("setworks:leather_focus")
                .description("Leather Silent: replaces Leather Quick when all four are worn"), SOURCE);

        // A hidden bonus and a discovered bonus on the iron set.
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:iron_secret")
                .requiresAll("setworks:iron_armor")
                .visibility(Visibility.HIDDEN)
                .description("Iron Secret: never shown"), SOURCE);
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:iron_memory")
                .requires("setworks:iron_armor", 3)
                .visibility(Visibility.DISCOVERED)
                .description("Iron Memory: shown once you have had three pieces on"), SOURCE);

        // Metadata and ore dictionary filters.
        SetworksApi.registerSet(SetBuilder.set("setworks:blue_banner")
                .displayName("Blue Banner")
                .piece(PieceBuilder.piece("wool").accept(ItemFilter.item("minecraft:wool", 11)).at("offhand"))
                .piece(PieceBuilder.piece("ingot").accept(ItemFilter.ore("ingotIron")).at("hotbar")), SOURCE);
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:blue_banner_full")
                .requiresAll("setworks:blue_banner")
                .description("Blue Banner: blue wool in the off hand and an iron ingot in the hotbar"), SOURCE);

        if (Boolean.getBoolean(INVALID_EXAMPLE_PROPERTY)) {
            SetworksApi.registerSet(SetBuilder.set("setworks:broken_example")
                    .displayName("Broken Example")
                    .piece(PieceBuilder.piece("head").accept("minecraft:iron_helmet").at("head"))
                    .piece(PieceBuilder.piece("chest").accept("minecraft:iron_chestplate").at("chest"))
                    .piece(PieceBuilder.piece("legs").accept("minecraft:no_such_item").at("legs")), SOURCE);
            SetworksApi.registerBonus(BonusBuilder.bonus("setworks:broken_example_full")
                    .requiresAll("setworks:broken_example")
                    .description("Broken Example: should never be active"), SOURCE);
        }
    }
}
