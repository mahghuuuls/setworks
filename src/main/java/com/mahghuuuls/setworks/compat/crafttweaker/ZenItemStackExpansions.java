package com.mahghuuuls.setworks.compat.crafttweaker;

import com.mahghuuuls.setworks.api.SetworksApi;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.item.ItemStack;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.List;

/** {@code stack.setworksSets}: the ids of every enabled set some piece of which accepts the stack. */
@ZenRegister
@ZenExpansion("crafttweaker.item.IItemStack")
public final class ZenItemStackExpansions {

    private ZenItemStackExpansions() {
    }

    @ZenMethod
    public static String[] setworksSets(IItemStack stack) {
        if (stack == null) {
            return new String[0];
        }
        ItemStack internal = CraftTweakerMC.getItemStack(stack);
        List<String> ids = SetworksApi.query().setsOf(internal);
        return ids.toArray(new String[ids.size()]);
    }
}
