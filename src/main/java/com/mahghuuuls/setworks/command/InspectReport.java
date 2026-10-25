package com.mahghuuuls.setworks.command;

import com.mahghuuuls.setworks.api.definition.BonusDefinition;
import com.mahghuuuls.setworks.api.definition.SetRequirement;
import com.mahghuuuls.setworks.core.eval.PlayerSetState;
import com.mahghuuuls.setworks.core.match.CompiledDefinitions;
import com.mahghuuuls.setworks.core.match.CompiledSet;
import com.mahghuuuls.setworks.core.match.SlotCandidate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Renders one player's state for the inspect command. Pure text; tested without a game. */
public final class InspectReport {

    /** Supplies the per-effect and per-condition lines of a bonus; the runtime's manager and evaluator in the game, nothing in tests. */
    public interface BonusDetails {
        List<String> effectLines(UUID player, BonusDefinition bonus);

        List<String> conditionLines(BonusDefinition bonus);
    }

    public static final BonusDetails NO_DETAILS = new BonusDetails() {
        @Override
        public List<String> effectLines(UUID player, BonusDefinition bonus) {
            return null;
        }

        @Override
        public List<String> conditionLines(BonusDefinition bonus) {
            return null;
        }
    };

    private InspectReport() {
    }

    public static List<String> lines(PlayerSetState state, CompiledDefinitions definitions) {
        return lines(state, definitions, NO_DETAILS);
    }

    public static List<String> lines(PlayerSetState state, CompiledDefinitions definitions, BonusDetails details) {
        List<String> lines = new ArrayList<String>();
        lines.add("setworks state of " + state.playerName());
        for (CompiledSet set : definitions.sets()) {
            PlayerSetState.SetState setState = state.sets().get(set.id());
            int count = setState == null ? 0 : setState.count();
            lines.add(" set " + set.id() + " " + count + "/" + set.pieces().size()
                    + " mode=" + set.definition().thresholdMode().name().toLowerCase());
            for (CompiledSet.Piece piece : set.pieces()) {
                SlotCandidate filled = setState == null ? null : setState.satisfiedPieces().get(piece.name());
                if (filled == null) {
                    lines.add("  piece " + piece.name() + ": missing");
                } else {
                    lines.add("  piece " + piece.name() + ": " + filled.item().registryName()
                            + (filled.item().metadata() != 0 ? "@" + filled.item().metadata() : "")
                            + " in " + filled.locationId() + "/" + filled.slotKey());
                }
            }
        }
        for (BonusDefinition bonus : definitions.bonuses()) {
            PlayerSetState.BonusEntry entry = state.bonuses().get(bonus.id());
            StringBuilder line = new StringBuilder(" bonus ").append(bonus.id()).append(" requires");
            for (SetRequirement requirement : bonus.requirements()) {
                line.append(' ').append(requirement);
            }
            line.append(": ");
            if (entry == null) {
                line.append("not evaluated");
            } else {
                line.append(entry.state().reasonText());
                if (!entry.detail().isEmpty()) {
                    line.append(": ").append(entry.detail());
                }
            }
            if (bonus.visibility() != com.mahghuuuls.setworks.api.Visibility.VISIBLE) {
                line.append(" [").append(bonus.visibility().name().toLowerCase()).append(']');
            }
            lines.add(line.toString());
            if (!bonus.conditions().isEmpty()) {
                List<String> conditionLines = details.conditionLines(bonus);
                if (conditionLines == null) {
                    lines.add("  conditions: " + bonus.conditions());
                } else {
                    lines.addAll(conditionLines);
                }
            }
            if (bonus.effects().isEmpty()) {
                lines.add("  effects: none (scripted)");
                continue;
            }
            List<String> effectLines = details.effectLines(state.playerId(), bonus);
            if (effectLines == null) {
                lines.add("  effects: " + bonus.effects());
            } else {
                lines.addAll(effectLines);
            }
        }
        return lines;
    }
}
