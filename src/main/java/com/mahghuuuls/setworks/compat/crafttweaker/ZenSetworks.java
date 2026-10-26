package com.mahghuuuls.setworks.compat.crafttweaker;

import com.mahghuuuls.setworks.api.DefinitionSource;
import com.mahghuuuls.setworks.api.ItemFilter;
import com.mahghuuuls.setworks.api.RegistrationResult;
import com.mahghuuuls.setworks.api.SetworksApi;
import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import crafttweaker.api.player.IPlayer;
import net.minecraft.entity.player.EntityPlayer;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.List;
import java.util.Map;

/**
 * The script entry {@code mods.setworks.Setworks}: one static method per API call plus the helpers.
 * Every definition call is queued and applied at load complete before the definitions freeze,
 * whichever loader ran the script ({@code #loader setworks} files run at load complete themselves).
 * Each call records the script file and line it came from, so a rejected definition names it.
 * The query methods answer from the last evaluated state and never evaluate anything.
 */
@ZenRegister
@ZenClass("mods.setworks.Setworks")
public final class ZenSetworks {

    private ZenSetworks() {
    }

    // ---- sets ----

    /** A new set; add pieces with the returned handle. */
    @ZenMethod
    public static ZenSet set(String id) {
        return new ZenSet(id, false, source());
    }

    /** A set that replaces a mod's or an example set of the same id. */
    @ZenMethod
    public static ZenSet replaceSet(String id) {
        return new ZenSet(id, true, source());
    }

    /** An armor set from four item patterns, one per armor location. */
    @ZenMethod
    public static ZenSet armorSet(String id, String name, IIngredient head, IIngredient chest, IIngredient legs, IIngredient feet) {
        return set(id).name(name).armor(head, chest, legs, feet);
    }

    /** A one-piece set of weapons held in the main hand. */
    @ZenMethod
    public static ZenSet weaponSet(String id, String name, IIngredient[] weapons) {
        return set(id).name(name).pieceAny("weapon", "mainhand", weapons);
    }

    @ZenMethod
    public static void disableSet(String id) {
        final DefinitionSource source = source();
        final String setId = id;
        ScriptQueue.get().enqueue(new ScriptQueue.Entry() {
            @Override
            public String describe() {
                return "disableSet " + setId + " (" + source.describe() + ")";
            }

            @Override
            public RegistrationResult apply() {
                return SetworksApi.disableSet(setId, source);
            }
        });
    }

    /** More items for an existing piece of any set, for example a mod's set. */
    @ZenMethod
    public static void addAlternatives(String setId, String pieceName, IIngredient[] items) {
        final DefinitionSource source = source();
        final String set = setId;
        final String piece = pieceName;
        final List<ItemFilter> filters = Ingredients.toFilters(items, "alternatives for piece " + pieceName + " of set " + setId);
        ScriptQueue.get().enqueue(new ScriptQueue.Entry() {
            @Override
            public String describe() {
                return "addAlternatives " + set + "/" + piece + " (" + source.describe() + ")";
            }

            @Override
            public RegistrationResult apply() {
                return SetworksApi.addAlternatives(set, piece, filters, source);
            }
        });
    }

    // ---- bonuses ----

    /** A new bonus; add requirements, effects, and conditions with the returned handle. */
    @ZenMethod
    public static ZenBonus bonus(String id) {
        return new ZenBonus(id, false, source());
    }

    /** A bonus that needs {@code threshold} pieces of one set, with its description. */
    @ZenMethod
    public static ZenBonus bonus(String id, String setId, int threshold, String description) {
        return bonus(id).requires(setId, threshold).description(description);
    }

    /** A bonus that needs every piece of one set, with its description. */
    @ZenMethod
    public static ZenBonus fullSetBonus(String id, String setId, String description) {
        return bonus(id).requiresAll(setId).description(description);
    }

    /** A bonus that replaces a mod's or an example bonus of the same id. */
    @ZenMethod
    public static ZenBonus replaceBonus(String id) {
        return new ZenBonus(id, true, source());
    }

    @ZenMethod
    public static void disableBonus(String id) {
        final DefinitionSource source = source();
        final String bonusId = id;
        ScriptQueue.get().enqueue(new ScriptQueue.Entry() {
            @Override
            public String describe() {
                return "disableBonus " + bonusId + " (" + source.describe() + ")";
            }

            @Override
            public RegistrationResult apply() {
                return SetworksApi.disableBonus(bonusId, source);
            }
        });
    }

    /** Changes the description, visibility, or display sets of an existing bonus. */
    @ZenMethod
    public static ZenBonusModification modifyBonus(String id) {
        return new ZenBonusModification(id, source());
    }

    // ---- queries (also available on IPlayer and IItemStack) ----

    @ZenMethod
    public static int pieceCount(IPlayer player, String setId) {
        return ZenPlayerExpansions.setworksPieceCount(player, setId);
    }

    @ZenMethod
    public static boolean isActive(IPlayer player, String bonusId) {
        return ZenPlayerExpansions.hasSetworksBonus(player, bonusId);
    }

    @ZenMethod
    public static String bonusState(IPlayer player, String bonusId) {
        return ZenPlayerExpansions.setworksBonusState(player, bonusId);
    }

    @ZenMethod
    public static Map<String, String> satisfiedPieces(IPlayer player, String setId) {
        return ZenPlayerExpansions.setworksSatisfiedPieces(player, setId);
    }

    @ZenMethod
    public static String[] setsOf(IItemStack stack) {
        return ZenItemStackExpansions.setworksSets(stack);
    }

    // ---- helpers ----

    static DefinitionSource source() {
        String where = null;
        try {
            where = CraftTweakerAPI.getScriptFileAndLine();
        } catch (RuntimeException ignored) {
            where = null;
        }
        return ScriptSource.parse(where);
    }

    static EntityPlayer player(IPlayer player) {
        return player == null ? null : CraftTweakerMC.getPlayer(player);
    }
}
