package com.mahghuuuls.setworks.network;

import com.mahghuuuls.setworks.api.ItemFilter;
import com.mahghuuuls.setworks.api.Visibility;
import com.mahghuuuls.setworks.api.definition.BonusDefinition;
import com.mahghuuuls.setworks.api.definition.PieceDefinition;
import com.mahghuuuls.setworks.api.definition.SetDefinition;
import com.mahghuuuls.setworks.api.definition.SetRequirement;
import com.mahghuuuls.setworks.core.definition.FrozenDefinitions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the display snapshot for one player from the frozen definitions. Hidden bonuses are never
 * included; a {@code discovered} bonus is included only when the visibility filter says the player
 * has discovered it. The threshold shown under a set is the
 * bonus's requirement for that set resolved against the set's piece count.
 */
public final class SnapshotBuilder {

    /** Decides per bonus and player whether a non-hidden bonus may be shown; discovery plugs in here. */
    public interface VisibilityFilter {
        boolean mayShow(BonusDefinition bonus);
    }

    /** Only {@code visible} bonuses; {@code discovered} ones wait for discovery. */
    public static final VisibilityFilter VISIBLE_ONLY = new VisibilityFilter() {
        @Override
        public boolean mayShow(BonusDefinition bonus) {
            return bonus.visibility() == Visibility.VISIBLE;
        }
    };

    /** {@code visible} bonuses, plus the {@code discovered} ones whose ids are in the player's record. */
    public static VisibilityFilter forPlayer(final Set<String> discovered) {
        return new VisibilityFilter() {
            @Override
            public boolean mayShow(BonusDefinition bonus) {
                return bonus.visibility() == Visibility.VISIBLE
                        || (bonus.visibility() == Visibility.DISCOVERED && discovered.contains(bonus.id()));
            }
        };
    }

    /** Gives each item filter its label for the Shift list; the server reads the item registry, tests use ids. */
    public interface Labels {
        /** The label of an item and metadata, and whether it is a translation key. */
        String itemLabel(String itemId, int metadata);
    }

    /** Labels that are the item ids themselves, not keys. */
    public static final Labels RAW_IDS = new Labels() {
        @Override
        public String itemLabel(String itemId, int metadata) {
            return null;
        }
    };

    private final DescriptionGenerator descriptions;
    private final Labels labels;

    public SnapshotBuilder(DescriptionGenerator descriptions, Labels labels) {
        this.descriptions = descriptions;
        this.labels = labels;
    }

    /** The display data of the named bonuses only (no sets), for a discovery message; unknown, disabled, or hidden ids are skipped. */
    public DisplaySnapshot bonusesOnly(FrozenDefinitions definitions, Collection<String> bonusIds) {
        List<DisplaySnapshot.BonusDisplay> bonuses = new ArrayList<DisplaySnapshot.BonusDisplay>();
        for (String id : bonusIds) {
            BonusDefinition bonus = definitions.bonus(id);
            if (bonus == null || bonus.visibility() == Visibility.HIDDEN) {
                continue;
            }
            bonuses.add(bonusDisplay(definitions, bonus));
        }
        return new DisplaySnapshot(new ArrayList<DisplaySnapshot.SetDisplay>(), bonuses);
    }

    public DisplaySnapshot build(FrozenDefinitions definitions, VisibilityFilter filter) {
        List<DisplaySnapshot.SetDisplay> sets = new ArrayList<DisplaySnapshot.SetDisplay>();
        for (SetDefinition set : definitions.enabledSets()) {
            List<DisplaySnapshot.PieceDisplay> pieces = new ArrayList<DisplaySnapshot.PieceDisplay>();
            for (PieceDefinition piece : set.pieces()) {
                List<DisplaySnapshot.AlternativeDisplay> alternatives = new ArrayList<DisplaySnapshot.AlternativeDisplay>();
                for (ItemFilter alternative : piece.alternatives()) {
                    alternatives.add(alternativeOf(alternative));
                }
                pieces.add(new DisplaySnapshot.PieceDisplay(piece.name(), alternatives));
            }
            sets.add(new DisplaySnapshot.SetDisplay(set.id(), set.displayName(), set.thresholdMode(), pieces));
        }
        List<DisplaySnapshot.BonusDisplay> bonuses = new ArrayList<DisplaySnapshot.BonusDisplay>();
        for (FrozenDefinitions.Entry<BonusDefinition> entry : definitions.allBonuses()) {
            if (!entry.enabled()) {
                continue;
            }
            BonusDefinition bonus = entry.definition();
            if (bonus.visibility() == Visibility.HIDDEN || !filter.mayShow(bonus)) {
                continue;
            }
            bonuses.add(bonusDisplay(definitions, bonus));
        }
        return new DisplaySnapshot(sets, bonuses);
    }

    private DisplaySnapshot.BonusDisplay bonusDisplay(FrozenDefinitions definitions, BonusDefinition bonus) {
        Map<String, Integer> thresholds = new LinkedHashMap<String, Integer>();
        for (SetRequirement requirement : bonus.requirements()) {
            if (!bonus.displayedUnder(requirement.setId())) {
                continue;
            }
            SetDefinition set = definitions.set(requirement.setId());
            if (set == null) {
                continue;
            }
            thresholds.put(requirement.setId(), requirement.effectiveThreshold(set));
        }
        return new DisplaySnapshot.BonusDisplay(bonus.id(), descriptions.describe(bonus), bonus.isMultiSet(), thresholds);
    }

    private DisplaySnapshot.AlternativeDisplay alternativeOf(ItemFilter filter) {
        if (filter.isOre()) {
            return new DisplaySnapshot.AlternativeDisplay(filter, "#" + filter.oreName(), false);
        }
        int metadata = filter.metadata() == ItemFilter.ANY_METADATA ? 0 : filter.metadata();
        String key = labels.itemLabel(filter.itemId(), metadata);
        if (key == null) {
            return new DisplaySnapshot.AlternativeDisplay(filter, filter.itemId(), false);
        }
        return new DisplaySnapshot.AlternativeDisplay(filter, key, true);
    }
}
