package com.mahghuuuls.setworks.network;

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
 * What one player's client must know about that player: per set, the names of the satisfied
 * pieces; and the ids of the bonuses whose required pieces are all worn (whatever their
 * conditions say). Counts are the sizes of the piece lists. Immutable.
 */
public final class DisplayState {

    private final Map<String, List<String>> satisfiedPieces;
    private final Set<String> equippedBonuses;

    public DisplayState(Map<String, List<String>> satisfiedPieces, Set<String> equippedBonuses) {
        Map<String, List<String>> copy = new LinkedHashMap<String, List<String>>();
        for (Map.Entry<String, List<String>> entry : satisfiedPieces.entrySet()) {
            copy.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<String>(entry.getValue())));
        }
        this.satisfiedPieces = Collections.unmodifiableMap(copy);
        this.equippedBonuses = Collections.unmodifiableSet(new LinkedHashSet<String>(equippedBonuses));
    }

    public static DisplayState empty() {
        return new DisplayState(Collections.<String, List<String>>emptyMap(), Collections.<String>emptySet());
    }

    public List<String> satisfiedPieces(String setId) {
        List<String> pieces = satisfiedPieces.get(setId);
        return pieces == null ? Collections.<String>emptyList() : pieces;
    }

    public int count(String setId) {
        return satisfiedPieces(setId).size();
    }

    public boolean isSatisfied(String setId, String pieceName) {
        return satisfiedPieces(setId).contains(pieceName);
    }

    /** True when the player wears every piece this bonus requires; the green of its tooltip line. */
    public boolean isEquipped(String bonusId) {
        return equippedBonuses.contains(bonusId);
    }

    public Set<String> equippedBonuses() {
        return equippedBonuses;
    }

    public Map<String, List<String>> allSatisfiedPieces() {
        return satisfiedPieces;
    }

    public void write(ByteBuf buf) {
        ByteBufUtils.writeVarInt(buf, satisfiedPieces.size(), 5);
        for (Map.Entry<String, List<String>> entry : satisfiedPieces.entrySet()) {
            Codec.writeString(buf, entry.getKey());
            ByteBufUtils.writeVarInt(buf, entry.getValue().size(), 5);
            for (String piece : entry.getValue()) {
                Codec.writeString(buf, piece);
            }
        }
        ByteBufUtils.writeVarInt(buf, equippedBonuses.size(), 5);
        for (String bonus : equippedBonuses) {
            Codec.writeString(buf, bonus);
        }
    }

    public static DisplayState read(ByteBuf buf) {
        int setCount = ByteBufUtils.readVarInt(buf, 5);
        Map<String, List<String>> pieces = new LinkedHashMap<String, List<String>>();
        for (int s = 0; s < setCount; s++) {
            String setId = Codec.readString(buf);
            int count = ByteBufUtils.readVarInt(buf, 5);
            List<String> names = new ArrayList<String>(count);
            for (int p = 0; p < count; p++) {
                names.add(Codec.readString(buf));
            }
            pieces.put(setId, names);
        }
        int bonusCount = ByteBufUtils.readVarInt(buf, 5);
        Set<String> equipped = new LinkedHashSet<String>();
        for (int b = 0; b < bonusCount; b++) {
            equipped.add(Codec.readString(buf));
        }
        return new DisplayState(pieces, equipped);
    }
}
