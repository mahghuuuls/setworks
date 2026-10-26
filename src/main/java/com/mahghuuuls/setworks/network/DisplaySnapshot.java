package com.mahghuuuls.setworks.network;

import com.mahghuuuls.setworks.api.ItemFilter;
import com.mahghuuuls.setworks.api.ThresholdMode;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Everything a client may know about the definitions: the enabled sets with their pieces and
 * alternatives (as filters, so the client can tell which sets a stack belongs to, plus a label per
 * alternative), and the bonuses this player may see with their description and the threshold under
 * each set they are displayed under. No rule travels with it: thresholds are numbers, activity
 * comes with the state. Immutable; written to and read from a byte buffer by the messages.
 */
public final class DisplaySnapshot {

    /** One alternative of a piece: the filter for matching and a label for the Shift list. */
    public static final class AlternativeDisplay {
        private final ItemFilter filter;
        private final String label;
        private final boolean labelIsKey;

        public AlternativeDisplay(ItemFilter filter, String label, boolean labelIsKey) {
            this.filter = filter;
            this.label = label;
            this.labelIsKey = labelIsKey;
        }

        public ItemFilter filter() {
            return filter;
        }

        /** The item's translation key when {@link #labelIsKey()} is true, otherwise plain text (an ore name). */
        public String label() {
            return label;
        }

        public boolean labelIsKey() {
            return labelIsKey;
        }
    }

    public static final class PieceDisplay {
        private final String name;
        private final List<AlternativeDisplay> alternatives;

        public PieceDisplay(String name, List<AlternativeDisplay> alternatives) {
            this.name = name;
            this.alternatives = Collections.unmodifiableList(new ArrayList<AlternativeDisplay>(alternatives));
        }

        public String name() {
            return name;
        }

        public List<AlternativeDisplay> alternatives() {
            return alternatives;
        }
    }

    public static final class SetDisplay {
        private final String id;
        private final String displayName;
        private final ThresholdMode thresholdMode;
        private final List<PieceDisplay> pieces;

        public SetDisplay(String id, String displayName, ThresholdMode thresholdMode, List<PieceDisplay> pieces) {
            this.id = id;
            this.displayName = displayName;
            this.thresholdMode = thresholdMode;
            this.pieces = Collections.unmodifiableList(new ArrayList<PieceDisplay>(pieces));
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }

        public ThresholdMode thresholdMode() {
            return thresholdMode;
        }

        public List<PieceDisplay> pieces() {
            return pieces;
        }
    }

    public static final class BonusDisplay {
        private final String id;
        private final String description;
        private final boolean multiSet;
        private final Map<String, Integer> thresholds;

        /** {@code thresholds}: the sets this bonus is displayed under, in author order, with the piece threshold for each. */
        public BonusDisplay(String id, String description, boolean multiSet, Map<String, Integer> thresholds) {
            this.id = id;
            this.description = description;
            this.multiSet = multiSet;
            this.thresholds = Collections.unmodifiableMap(new LinkedHashMap<String, Integer>(thresholds));
        }

        public String id() {
            return id;
        }

        /** Author text, a language key, or generated text with {@code ${key}} tokens the client translates. */
        public String description() {
            return description;
        }

        /** True when the bonus requires more than one set; such bonuses are listed after the single-set ones. */
        public boolean multiSet() {
            return multiSet;
        }

        public Map<String, Integer> thresholds() {
            return thresholds;
        }

        public boolean displayedUnder(String setId) {
            return thresholds.containsKey(setId);
        }
    }

    private final List<SetDisplay> sets;
    private final List<BonusDisplay> bonuses;

    public DisplaySnapshot(List<SetDisplay> sets, List<BonusDisplay> bonuses) {
        this.sets = Collections.unmodifiableList(new ArrayList<SetDisplay>(sets));
        this.bonuses = Collections.unmodifiableList(new ArrayList<BonusDisplay>(bonuses));
    }

    public static DisplaySnapshot empty() {
        return new DisplaySnapshot(Collections.<SetDisplay>emptyList(), Collections.<BonusDisplay>emptyList());
    }

    public List<SetDisplay> sets() {
        return sets;
    }

    public SetDisplay set(String id) {
        for (SetDisplay set : sets) {
            if (set.id().equals(id)) {
                return set;
            }
        }
        return null;
    }

    public List<BonusDisplay> bonuses() {
        return bonuses;
    }

    /** The ids of the bonuses this snapshot carries; a state message names no other bonus. */
    public Set<String> bonusIds() {
        Set<String> ids = new LinkedHashSet<String>();
        for (BonusDisplay bonus : bonuses) {
            ids.add(bonus.id());
        }
        return ids;
    }

    // ---- wire format ----

    public void write(ByteBuf buf) {
        ByteBufUtils.writeVarInt(buf, sets.size(), 5);
        for (SetDisplay set : sets) {
            Codec.writeString(buf, set.id());
            Codec.writeString(buf, set.displayName());
            buf.writeByte(set.thresholdMode().ordinal());
            ByteBufUtils.writeVarInt(buf, set.pieces().size(), 5);
            for (PieceDisplay piece : set.pieces()) {
                Codec.writeString(buf, piece.name());
                ByteBufUtils.writeVarInt(buf, piece.alternatives().size(), 5);
                for (AlternativeDisplay alternative : piece.alternatives()) {
                    Codec.writeFilter(buf, alternative.filter());
                    Codec.writeString(buf, alternative.label());
                    buf.writeBoolean(alternative.labelIsKey());
                }
            }
        }
        ByteBufUtils.writeVarInt(buf, bonuses.size(), 5);
        for (BonusDisplay bonus : bonuses) {
            Codec.writeString(buf, bonus.id());
            Codec.writeString(buf, bonus.description());
            buf.writeBoolean(bonus.multiSet());
            ByteBufUtils.writeVarInt(buf, bonus.thresholds().size(), 5);
            for (Map.Entry<String, Integer> threshold : bonus.thresholds().entrySet()) {
                Codec.writeString(buf, threshold.getKey());
                ByteBufUtils.writeVarInt(buf, threshold.getValue(), 5);
            }
        }
    }

    public static DisplaySnapshot read(ByteBuf buf) {
        int setCount = ByteBufUtils.readVarInt(buf, 5);
        List<SetDisplay> sets = new ArrayList<SetDisplay>(setCount);
        for (int s = 0; s < setCount; s++) {
            String id = Codec.readString(buf);
            String name = Codec.readString(buf);
            ThresholdMode mode = ThresholdMode.values()[buf.readByte()];
            int pieceCount = ByteBufUtils.readVarInt(buf, 5);
            List<PieceDisplay> pieces = new ArrayList<PieceDisplay>(pieceCount);
            for (int p = 0; p < pieceCount; p++) {
                String pieceName = Codec.readString(buf);
                int alternativeCount = ByteBufUtils.readVarInt(buf, 5);
                List<AlternativeDisplay> alternatives = new ArrayList<AlternativeDisplay>(alternativeCount);
                for (int a = 0; a < alternativeCount; a++) {
                    ItemFilter filter = Codec.readFilter(buf);
                    String label = Codec.readString(buf);
                    boolean labelIsKey = buf.readBoolean();
                    alternatives.add(new AlternativeDisplay(filter, label, labelIsKey));
                }
                pieces.add(new PieceDisplay(pieceName, alternatives));
            }
            sets.add(new SetDisplay(id, name, mode, pieces));
        }
        int bonusCount = ByteBufUtils.readVarInt(buf, 5);
        List<BonusDisplay> bonuses = new ArrayList<BonusDisplay>(bonusCount);
        for (int b = 0; b < bonusCount; b++) {
            String id = Codec.readString(buf);
            String description = Codec.readString(buf);
            boolean multiSet = buf.readBoolean();
            int thresholdCount = ByteBufUtils.readVarInt(buf, 5);
            Map<String, Integer> thresholds = new LinkedHashMap<String, Integer>();
            for (int t = 0; t < thresholdCount; t++) {
                String setId = Codec.readString(buf);
                thresholds.put(setId, ByteBufUtils.readVarInt(buf, 5));
            }
            bonuses.add(new BonusDisplay(id, description, multiSet, thresholds));
        }
        return new DisplaySnapshot(sets, bonuses);
    }
}
