package com.mahghuuuls.setworks.client;

import com.mahghuuuls.setworks.network.DisplaySnapshot;
import com.mahghuuuls.setworks.network.DisplayState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Builds the tooltip lines for a stack from the snapshot and the player's state. One block per
 * set the stack belongs to: a blank line, the set name in gold with {@code (worn/total)}, then one
 * {@code (N) description} line per bonus displayed under the set, green when active and gray
 * otherwise, single-set bonuses first by ascending threshold and multi-set bonuses after in author
 * order. While Shift is held the piece lines follow the name: alternatives joined by " or ",
 * white when the piece is satisfied and gray otherwise. Nothing else is added. Pure text with
 * colour codes; the translator supplies language lookups.
 */
public final class TooltipRenderer {

    /** Translates a text that may be a language key or contain {@code ${key}} tokens. */
    public interface Translator {
        String translate(String textOrKey);
    }

    /** Returns the text unchanged; for tests. */
    public static final Translator RAW = new Translator() {
        @Override
        public String translate(String textOrKey) {
            return textOrKey;
        }
    };

    static final String GOLD = "§6";
    static final String GREEN = "§a";
    static final String GRAY = "§7";
    static final String WHITE = "§f";

    private TooltipRenderer() {
    }

    public static List<String> render(DisplaySnapshot snapshot, DisplayState state, List<String> setIds, boolean shift,
                                      Translator translator) {
        List<String> lines = new ArrayList<String>();
        for (String setId : setIds) {
            DisplaySnapshot.SetDisplay set = snapshot.set(setId);
            if (set == null) {
                continue;
            }
            lines.add("");
            int worn = state.count(setId);
            lines.add(GOLD + translator.translate(set.displayName()) + " (" + worn + "/" + set.pieces().size() + ")");
            if (shift) {
                for (DisplaySnapshot.PieceDisplay piece : set.pieces()) {
                    lines.add((state.isSatisfied(setId, piece.name()) ? WHITE : GRAY) + pieceLabel(piece, translator));
                }
            }
            for (DisplaySnapshot.BonusDisplay bonus : bonusesUnder(snapshot, setId)) {
                String colour = state.isActive(bonus.id()) ? GREEN : GRAY;
                lines.add(colour + "(" + bonus.thresholds().get(setId) + ") " + translator.translate(bonus.description()));
            }
        }
        return lines;
    }

    /** Single-set bonuses by ascending threshold under this set, then multi-set bonuses in author order. */
    static List<DisplaySnapshot.BonusDisplay> bonusesUnder(DisplaySnapshot snapshot, final String setId) {
        List<DisplaySnapshot.BonusDisplay> single = new ArrayList<DisplaySnapshot.BonusDisplay>();
        List<DisplaySnapshot.BonusDisplay> multi = new ArrayList<DisplaySnapshot.BonusDisplay>();
        for (DisplaySnapshot.BonusDisplay bonus : snapshot.bonuses()) {
            if (!bonus.displayedUnder(setId)) {
                continue;
            }
            (bonus.multiSet() ? multi : single).add(bonus);
        }
        Collections.sort(single, new Comparator<DisplaySnapshot.BonusDisplay>() {
            @Override
            public int compare(DisplaySnapshot.BonusDisplay a, DisplaySnapshot.BonusDisplay b) {
                return Integer.compare(a.thresholds().get(setId), b.thresholds().get(setId));
            }
        });
        single.addAll(multi);
        return single;
    }

    static String pieceLabel(DisplaySnapshot.PieceDisplay piece, Translator translator) {
        StringBuilder text = new StringBuilder();
        for (DisplaySnapshot.AlternativeDisplay alternative : piece.alternatives()) {
            if (text.length() > 0) {
                text.append(" or ");
            }
            text.append(alternative.labelIsKey() ? translator.translate(alternative.label()) : alternative.label());
        }
        return text.toString();
    }
}
