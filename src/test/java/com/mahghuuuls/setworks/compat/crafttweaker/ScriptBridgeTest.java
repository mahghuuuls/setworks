package com.mahghuuuls.setworks.compat.crafttweaker;

import com.mahghuuuls.setworks.api.ItemFilter;
import com.mahghuuuls.setworks.api.RegistrationResult;
import com.mahghuuuls.setworks.api.ThresholdMode;
import com.mahghuuuls.setworks.api.Visibility;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The pure parts of the CraftTweaker bridge: source capture, the queue, the filter rules, and the names. */
class ScriptBridgeTest {

    @Test
    void scriptSourceNamesFileAndLine() {
        assertEquals("script:sets.zs:12", ScriptSource.parse("sets.zs:12").describe());
        assertEquals("script:sub/sets.zs:3", ScriptSource.parse(" sub/sets.zs:3 ").describe());
        assertEquals("script:sets.zs", ScriptSource.parse("sets.zs").describe(), "no line number: the file alone");
        assertEquals("script:C:/x/sets.zs", ScriptSource.parse("C:/x/sets.zs").describe(), "a drive letter is not a line");
        assertEquals("script:script", ScriptSource.parse(null).describe());
        assertEquals("script:script", ScriptSource.parse("  ").describe());
        assertEquals("script:script", ScriptSource.parse("<?>").describe(), "CraftTweaker's text when no script frame is on the stack");
    }

    private static final class Log implements ScriptQueue.Reporter {
        final List<String> lines = new ArrayList<String>();

        public void rejected(String describe, String message) {
            lines.add("rejected " + describe + ": " + message);
        }

        public void failed(String describe, RuntimeException error) {
            lines.add("failed " + describe + ": " + error.getMessage());
        }

        public void late(String describe) {
            lines.add("late " + describe);
        }
    }

    private static ScriptQueue.Entry entry(final String name, final List<String> order, final RegistrationResult result) {
        return new ScriptQueue.Entry() {
            public String describe() {
                return name;
            }

            public RegistrationResult apply() {
                order.add(name);
                if (result == null) {
                    throw new IllegalArgumentException("bad " + name);
                }
                return result;
            }
        };
    }

    @Test
    void queueAppliesInOrderOnceAndReportsEachProblem() {
        Log log = new Log();
        ScriptQueue queue = new ScriptQueue(log);
        List<String> order = new ArrayList<String>();
        assertTrue(queue.enqueue(entry("set a", order, RegistrationResult.ok())));
        assertTrue(queue.enqueue(entry("bonus b", order, RegistrationResult.error("duplicate"))));
        assertTrue(queue.enqueue(entry("set c", order, null)));
        assertTrue(queue.enqueue(entry("bonus d", order, RegistrationResult.ok())));
        assertEquals(4, queue.size());
        assertFalse(queue.isApplied());
        assertEquals(2, queue.applyAll(), "two registered without a problem");
        assertEquals(Arrays.asList("set a", "bonus b", "set c", "bonus d"), order, "call order kept; a failure stops nothing");
        assertEquals(Arrays.asList("rejected bonus b: duplicate", "failed set c: bad set c"), log.lines);
        assertEquals(0, queue.size(), "emptied: a second apply registers nothing twice");
        assertTrue(queue.isApplied());
        assertEquals(0, queue.applyAll());
        assertFalse(queue.enqueue(entry("set e", order, RegistrationResult.ok())), "after the apply a call is dropped");
        assertEquals("late set e", log.lines.get(2));
        assertEquals(4, order.size(), "the late entry never applies");
    }

    @Test
    void filterRulesFollowCraftTweakerMeaning() {
        assertEquals(ItemFilter.item("minecraft:iron_helmet"), FilterRules.filterFor("minecraft:iron_helmet", 0, false, null),
                "a damageable item without sub-types: any damage");
        assertEquals(ItemFilter.item("minecraft:iron_helmet"), FilterRules.filterFor("minecraft:iron_helmet", 7, false, null),
                "a damaged pattern of such an item still means the item");
        assertEquals(ItemFilter.item("minecraft:wool", 5), FilterRules.filterFor("minecraft:wool", 5, true, null),
                "a sub-type item names its metadata");
        assertEquals(ItemFilter.item("minecraft:wool"), FilterRules.filterFor("minecraft:wool", OreDictionary.WILDCARD_VALUE, true, null),
                "the wildcard means any");
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Owner", "Steve");
        tag.setInteger("Level", 3);
        ItemFilter tagged = FilterRules.filterFor("mod:ring", 0, false, tag);
        assertEquals(2, tagged.requiredNbt().size(), "each top-level key is a containment rule");
        assertEquals("Owner", tagged.requiredNbt().get(0).path());
        assertEquals("\"Steve\"", tagged.requiredNbt().get(0).value());
        assertEquals("3", tagged.requiredNbt().get(1).value());
    }

    @Test
    void loaderScopeNamesTheFailingFileAndTheSkippedOnes() {
        List<String> visited = Arrays.asList("setworks_a.zs", "sub\\setworks_broken.zs", "setworks_c.zs", "setworks_d.zs");
        assertEquals(null, LoaderScope.message("setworks", true, null, 0, null, visited), "nothing to report");
        assertEquals("loader setworks: ended in error (see crafttweaker.log); the definitions of the file that failed are missing",
                LoaderScope.message("setworks", false, null, 0, null, visited));
        String text = LoaderScope.message("setworks", false, "sub/setworks_broken.zs", 11, " unexpected token ", visited);
        assertEquals("loader setworks: sub/setworks_broken.zs line 11 failed to parse: unexpected token; CraftTweaker then skipped the loader's later files "
                + "[setworks_c.zs, setworks_d.zs]; every Setworks definition from those files is missing", text,
                "the separator and case of CraftTweaker's name do not matter");
        assertEquals("loader setworks: setworks_d.zs line 2 failed to parse; every Setworks definition from that file is missing",
                LoaderScope.message("setworks", false, "setworks_d.zs", 2, "", visited), "the last file skips nothing");
        assertTrue(LoaderScope.message("setworks", false, "elsewhere.zs", 1, null, visited).endsWith("from that file is missing"),
                "an unknown file name still reports the parse error");
    }

    @Test
    void namesMapScriptWordsAndParams() {
        assertEquals(ThresholdMode.HIGHEST_ONLY, Names.thresholdMode("highest_only"));
        assertEquals(ThresholdMode.HIGHEST_ONLY, Names.thresholdMode("Highest Only"));
        assertEquals(ThresholdMode.CUMULATIVE, Names.thresholdMode("cumulative"));
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                Names.thresholdMode("both");
            }
        });
        assertEquals(Visibility.DISCOVERED, Names.visibility("discovered"));
        assertEquals(Visibility.HIDDEN, Names.visibility("HIDDEN"));
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                Names.visibility("secret");
            }
        });
        Map<String, Object> script = new LinkedHashMap<String, Object>();
        script.put("fraction", 0.5);
        script.put("note", null);
        Map<String, String> params = Names.params(script);
        assertEquals("0.5", params.get("fraction"), "a script number arrives as its text");
        assertEquals("", params.get("note"));
        assertTrue(Names.params(null).isEmpty());
    }
}
