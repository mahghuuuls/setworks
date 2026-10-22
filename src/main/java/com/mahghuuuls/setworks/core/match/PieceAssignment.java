package com.mahghuuuls.setworks.core.match;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assigns slots to the named pieces of one set so that the number of satisfied pieces is the
 * maximum possible. Each piece counts at most once and each slot fills at most one piece.
 * The result does not depend on registration order or slot order beyond tie-breaking between
 * equally large assignments. Maximum bipartite matching by augmenting paths; sizes are tiny
 * (at most 32 pieces, a few dozen slots).
 */
public final class PieceAssignment {

    private PieceAssignment() {
    }

    /** Piece name to the slot that fills it, in piece order; unsatisfied pieces are absent. */
    public static Map<String, SlotCandidate> assign(CompiledSet set, List<SlotCandidate> candidates) {
        List<CompiledSet.Piece> pieces = set.pieces();
        int pieceCount = pieces.size();
        int slotCount = candidates.size();
        if (pieceCount == 0 || slotCount == 0) {
            return Collections.emptyMap();
        }
        // adjacency[piece] = slots that piece accepts
        List<List<Integer>> adjacency = new ArrayList<List<Integer>>(pieceCount);
        for (int p = 0; p < pieceCount; p++) {
            CompiledSet.Piece piece = pieces.get(p);
            List<Integer> slots = new ArrayList<Integer>();
            for (int s = 0; s < slotCount; s++) {
                SlotCandidate candidate = candidates.get(s);
                if (candidate.item() != null && !candidate.item().isEmpty()
                        && piece.acceptsLocation(candidate.locationId()) && piece.matches(candidate.item())) {
                    slots.add(s);
                }
            }
            adjacency.add(slots);
        }
        int[] slotToPiece = new int[slotCount];
        Arrays.fill(slotToPiece, -1);
        for (int p = 0; p < pieceCount; p++) {
            boolean[] visited = new boolean[slotCount];
            augment(p, adjacency, slotToPiece, visited);
        }
        Map<String, SlotCandidate> result = new LinkedHashMap<String, SlotCandidate>();
        SlotCandidate[] bySlot = new SlotCandidate[pieceCount];
        for (int s = 0; s < slotCount; s++) {
            if (slotToPiece[s] >= 0) {
                bySlot[slotToPiece[s]] = candidates.get(s);
            }
        }
        for (int p = 0; p < pieceCount; p++) {
            if (bySlot[p] != null) {
                result.put(pieces.get(p).name(), bySlot[p]);
            }
        }
        return result;
    }

    private static boolean augment(int piece, List<List<Integer>> adjacency, int[] slotToPiece, boolean[] visited) {
        for (int slot : adjacency.get(piece)) {
            if (visited[slot]) {
                continue;
            }
            visited[slot] = true;
            if (slotToPiece[slot] < 0 || augment(slotToPiece[slot], adjacency, slotToPiece, visited)) {
                slotToPiece[slot] = piece;
                return true;
            }
        }
        return false;
    }
}
