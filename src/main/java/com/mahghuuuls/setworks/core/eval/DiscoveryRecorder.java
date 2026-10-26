package com.mahghuuuls.setworks.core.eval;

import com.mahghuuuls.setworks.api.Visibility;
import com.mahghuuuls.setworks.api.definition.BonusDefinition;
import com.mahghuuuls.setworks.core.discovery.DiscoveryCapability;
import com.mahghuuuls.setworks.core.match.CompiledDefinitions;

import java.util.ArrayList;
import java.util.List;

/**
 * The discovery rule: a {@code discovered} bonus is discovered the first time it becomes active
 * for the player. Visible bonuses need no record and hidden ones are never shown, so only
 * {@code discovered} ids are written. Pure: it reads the transitions and updates the record.
 */
public final class DiscoveryRecorder {

    private DiscoveryRecorder() {
    }

    /** Records every new discovery among the activations and returns the newly discovered ids, in transition order. */
    public static List<String> record(List<Transition> transitions, CompiledDefinitions definitions, DiscoveryCapability record) {
        List<String> found = new ArrayList<String>();
        if (record == null) {
            return found;
        }
        for (Transition transition : transitions) {
            if (!transition.activated()) {
                continue;
            }
            BonusDefinition bonus = definitions.bonus(transition.bonusId());
            if (bonus != null && bonus.visibility() == Visibility.DISCOVERED && record.discover(bonus.id())) {
                found.add(bonus.id());
            }
        }
        return found;
    }
}
