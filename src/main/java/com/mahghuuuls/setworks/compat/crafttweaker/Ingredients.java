package com.mahghuuuls.setworks.compat.crafttweaker;

import com.mahghuuuls.setworks.api.ItemFilter;
import crafttweaker.api.data.IData;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.item.IngredientOr;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.api.oredict.IOreDictEntry;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts script ingredients to item filters. Supported: an item stack pattern such as
 * {@code <minecraft:iron_helmet>}, {@code <minecraft:wool:5>}, {@code <minecraft:iron_sword:*>}, or
 * one with a tag; an ore dictionary entry such as {@code <ore:ingotIron>}; and {@code |} of those,
 * which is expanded to the items it lists when the script runs. Conditions, transformers, amounts,
 * and {@code <*>} are refused with a message, because a piece filter cannot express them.
 */
final class Ingredients {

    private Ingredients() {
    }

    static List<ItemFilter> toFilters(IIngredient ingredient, String what) {
        if (ingredient == null) {
            throw new IllegalArgumentException(what + " must not be null");
        }
        List<ItemFilter> filters = new ArrayList<ItemFilter>();
        if (ingredient instanceof IItemStack) {
            filters.add(fromStack((IItemStack) ingredient));
        } else if (ingredient instanceof IOreDictEntry) {
            filters.add(ItemFilter.ore(((IOreDictEntry) ingredient).getName()));
        } else if (ingredient instanceof IngredientOr) {
            for (IItemStack stack : ingredient.getItems()) {
                filters.add(fromStack(stack));
            }
            if (filters.isEmpty()) {
                throw new IllegalArgumentException(what + ": " + ingredient.toCommandString() + " lists no item");
            }
        } else {
            throw new IllegalArgumentException(what + ": " + ingredient.toCommandString()
                    + " is not supported; use an item, an ore entry, or items joined with |, without conditions, transformers, or amounts");
        }
        return filters;
    }

    static List<ItemFilter> toFilters(IIngredient[] ingredients, String what) {
        if (ingredients == null || ingredients.length == 0) {
            throw new IllegalArgumentException(what + " needs at least one item");
        }
        List<ItemFilter> filters = new ArrayList<ItemFilter>();
        for (IIngredient ingredient : ingredients) {
            filters.addAll(toFilters(ingredient, what));
        }
        return filters;
    }

    private static ItemFilter fromStack(IItemStack stack) {
        if (stack == null || stack.getDefinition() == null) {
            throw new IllegalArgumentException("item pattern names no item");
        }
        NBTTagCompound tag = null;
        IData data = stack.getTag();
        if (data != null) {
            tag = CraftTweakerMC.getNBTCompound(data);
            if (tag != null && tag.getKeySet().isEmpty()) {
                tag = null;
            }
        }
        // getDamage is the raw value (the wildcard stays the wildcard); getMetadata would map it through the item.
        return FilterRules.filterFor(stack.getDefinition().getId(), stack.getDamage(), stack.getHasSubtypes(), tag);
    }
}
