package com.mahghuuuls.setworks.perf;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the performance scripts of the evaluation-cost check: {@code count} four-piece sets over
 * the five vanilla armor materials, overlapping (set {@code i} takes head and chest from one material
 * and legs and feet from another), each with a two-piece bonus and a full-set bonus carrying one
 * attribute effect. In the NBT variant every second set requires a tag on its items, which a plain
 * item never has, so those sets never count but their filters run at every scan. Pure text; a test
 * writes the files.
 */
public final class PerfScripts {

    static final String[] MATERIALS = {"leather", "chainmail", "iron", "golden", "diamond"};
    static final String[] SLOTS = {"helmet", "chestplate", "leggings", "boots"};

    private PerfScripts() {
    }

    public static String script(int count, boolean nbt) {
        StringBuilder out = new StringBuilder();
        out.append("#loader setworks\n\n// Generated performance script: ").append(count)
                .append(" overlapping four-piece sets, ").append(nbt ? "every second set with an NBT filter" : "id-only filters")
                .append(". Not for a pack.\n\nimport mods.setworks.Setworks;\n\n");
        for (int i = 0; i < count; i++) {
            String a = MATERIALS[i % MATERIALS.length];
            String b = MATERIALS[(i + 2) % MATERIALS.length];
            boolean tagged = nbt && i % 2 == 1;
            String id = "perf:set_" + i;
            out.append("Setworks.armorSet(\"").append(id).append("\", \"Perf ").append(i).append("\", ")
                    .append(item(a, SLOTS[0], tagged)).append(", ").append(item(a, SLOTS[1], tagged)).append(", ")
                    .append(item(b, SLOTS[2], tagged)).append(", ").append(item(b, SLOTS[3], tagged)).append(");\n");
            out.append("Setworks.bonus(\"").append(id).append("_two\", \"").append(id).append("\", 2, \"Perf ").append(i)
                    .append(" two\").attribute(\"generic.armor\", 1, \"add\");\n");
            out.append("Setworks.fullSetBonus(\"").append(id).append("_all\", \"").append(id).append("\", \"Perf ").append(i)
                    .append(" all\").attribute(\"generic.maxHealth\", 1, \"add\");\n\n");
        }
        return out.toString();
    }

    static String item(String material, String slot, boolean tagged) {
        String pattern = "<minecraft:" + material + "_" + slot + ">";
        return tagged ? pattern + ".withTag({perf: \"on\"})" : pattern;
    }

    /** The set ids a full suit of one material satisfies two pieces of, for the test. */
    static List<String> setsWithTwoPiecesOf(int count, String material) {
        List<String> ids = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            if (MATERIALS[i % MATERIALS.length].equals(material) || MATERIALS[(i + 2) % MATERIALS.length].equals(material)) {
                ids.add("perf:set_" + i);
            }
        }
        return ids;
    }
}
