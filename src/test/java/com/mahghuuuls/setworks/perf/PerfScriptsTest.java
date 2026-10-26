package com.mahghuuuls.setworks.perf;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The generated scripts have the shape the evaluation-cost check needs, and the test writes them under {@code build/perf}. */
class PerfScriptsTest {

    @Test
    void fiftySetsOverlapAndHalfCarryNbtFilters() throws IOException {
        String nbt = PerfScripts.script(50, true);
        String plain = PerfScripts.script(50, false);
        assertEquals(50, count(nbt, "Setworks.armorSet("));
        assertEquals(50, count(nbt, "_two\""), "one two-piece bonus per set");
        assertEquals(50, count(nbt, "_all\""), "one full-set bonus per set");
        assertEquals(25 * 4, count(nbt, ".withTag({perf: \"on\"})"), "every second set tags its four items");
        assertEquals(0, count(plain, ".withTag("));
        assertEquals(count(nbt, "<minecraft:"), count(plain, "<minecraft:"), "the same items, only the tags differ");
        assertTrue(nbt.startsWith("#loader setworks\n"));
        assertEquals(20, PerfScripts.setsWithTwoPiecesOf(50, "iron").size(), "a full iron suit gives two pieces to twenty sets");
        assertEquals(0, count(nbt, "perf:set_50"));
        File dir = new File("build/perf");
        assertTrue(dir.isDirectory() || dir.mkdirs());
        write(new File(dir, "setworks_perf_nbt.zs"), nbt);
        write(new File(dir, "setworks_perf_plain.zs"), plain);
    }

    private static int count(String text, String needle) {
        int n = 0;
        int at = 0;
        while ((at = text.indexOf(needle, at)) >= 0) {
            n++;
            at += needle.length();
        }
        return n;
    }

    private static void write(File file, String text) throws IOException {
        Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
        try {
            writer.write(text);
        } finally {
            writer.close();
        }
    }
}
