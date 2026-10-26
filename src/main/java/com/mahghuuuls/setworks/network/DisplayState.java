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
 * pieces; and the ids of the active bonuses. Counts are the sizes of the piece lists. Immutable.
 */
public final class DisplayState {

    private final Map<String, List<String>> satisfiedPieces;
    private final Set<String> activeBonuses;

    public DisplayState(Map<String, List<String>> satisfiedPieces, Set<String> activeBonuses) {
        Map<String, List<String>> copy = new LinkedHashMap<String, List<String>>();
        for (Map.Entry<String, List<String>> entry : satisfiedPieces.entrySet()) {
            copy.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<String>(entry.getValue())));
        }
        this.satisfiedPieces = Collections.unmodifiableMap(copy);
        this.activeBonuses = Collections.unmodifiableSet(new LinkedHashSet<String>(activeBonuses));
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

    public boolean isActive(String bonusId) {
        return activeBonuses.contains(bonusId);
    }

    public Set<String> activeBonuses() {
        return activeBonuses;
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
        ByteBufUtils.writeVarInt(buf, activeBonuses.size(), 5);
        for (String bonus : activeBonuses) {
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
        Set<String> active = new LinkedHashSet<String>();
        for (int b = 0; b < bonusCount; b++) {
            active.add(Codec.readString(buf));
        }
        return new DisplayState(pieces, active);
    }
}
