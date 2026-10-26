package com.mahghuuuls.setworks.compat.crafttweaker;

import com.mahghuuuls.setworks.api.DefinitionSource;
import com.mahghuuuls.setworks.api.ItemFilter;
import com.mahghuuuls.setworks.api.PieceBuilder;
import com.mahghuuuls.setworks.api.RegistrationResult;
import com.mahghuuuls.setworks.api.SetBuilder;
import com.mahghuuuls.setworks.api.SetworksApi;
import com.mahghuuuls.setworks.api.ThresholdMode;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IIngredient;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

/**
 * A set being defined from a script. Created by {@code Setworks.set(id)}, {@code Setworks.replaceSet(id)},
 * or a helper; registered when the definitions freeze with whatever was added to it by then, so no
 * closing call is needed. Every method returns the handle for chaining.
 */
@ZenRegister
@ZenClass("mods.setworks.Set")
public final class ZenSet {

    private final String id;
    private final SetBuilder builder;

    ZenSet(final String id, final boolean replace, final DefinitionSource source) {
        this.id = id;
        this.builder = SetBuilder.set(id);
        ScriptQueue.get().enqueue(new ScriptQueue.Entry() {
            @Override
            public String describe() {
                return (replace ? "replaceSet " : "set ") + id + " (" + source.describe() + ")";
            }

            @Override
            public RegistrationResult apply() {
                return replace ? SetworksApi.replaceSet(builder.build(), source) : SetworksApi.registerSet(builder, source);
            }
        });
    }

    /** The display name, plain text or a language key. Defaults to the id. */
    @ZenMethod
    public ZenSet name(String name) {
        builder.displayName(name);
        return this;
    }

    /** Only the highest threshold reached applies its bonuses (default: every threshold reached applies). */
    @ZenMethod
    public ZenSet highestOnly() {
        builder.thresholdMode(ThresholdMode.HIGHEST_ONLY);
        return this;
    }

    /** {@code "cumulative"} (default) or {@code "highest_only"}. */
    @ZenMethod
    public ZenSet thresholdMode(String mode) {
        builder.thresholdMode(Names.thresholdMode(mode));
        return this;
    }

    /** A named piece filled by the item at the location ({@code head}, {@code chest}, {@code legs}, {@code feet}, {@code mainhand}, {@code offhand}, {@code hotbar}, {@code inventory}, or an addon location). */
    @ZenMethod
    public ZenSet piece(String name, String location, IIngredient item) {
        return pieceAt(name, new String[] {location}, new IIngredient[] {item});
    }

    /** A named piece filled by any of the items at the location. */
    @ZenMethod
    public ZenSet pieceAny(String name, String location, IIngredient[] items) {
        return pieceAt(name, new String[] {location}, items);
    }

    /** A named piece filled by any of the items at any of the locations. */
    @ZenMethod
    public ZenSet pieceAt(String name, String[] locations, IIngredient[] items) {
        PieceBuilder piece = PieceBuilder.piece(name);
        for (ItemFilter filter : Ingredients.toFilters(items, "piece " + name + " of set " + id)) {
            piece.accept(filter);
        }
        if (locations == null || locations.length == 0) {
            throw new IllegalArgumentException("piece " + name + " of set " + id + " needs at least one location");
        }
        for (String location : locations) {
            piece.at(location);
        }
        builder.piece(piece);
        return this;
    }

    /** The four armor pieces, one item pattern each, at the vanilla armor locations. */
    @ZenMethod
    public ZenSet armor(IIngredient head, IIngredient chest, IIngredient legs, IIngredient feet) {
        piece("head", "head", head);
        piece("chest", "chest", chest);
        piece("legs", "legs", legs);
        piece("feet", "feet", feet);
        return this;
    }

    @ZenMethod
    public String getId() {
        return id;
    }
}
