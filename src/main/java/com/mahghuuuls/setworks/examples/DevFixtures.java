package com.mahghuuuls.setworks.examples;

import com.mahghuuuls.setworks.api.BonusListener;
import com.mahghuuuls.setworks.api.BonusState;
import com.mahghuuuls.setworks.api.DefinitionSource;
import com.mahghuuuls.setworks.api.PieceBuilder;
import com.mahghuuuls.setworks.api.SetBuilder;
import com.mahghuuuls.setworks.api.SetworksApi;
import net.minecraft.entity.player.EntityPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Development-only fixtures, registered only when the system property
 * {@code setworks.invalidExample} is true. They exist so an operator can see, on a running game,
 * the validation and listener behavior that ordinary examples cannot show: a duplicate registration
 * that is rejected, a disabled bonus, and a listener that throws beside one that keeps working.
 * Never enabled in a normal pack.
 */
public final class DevFixtures {

    public static final DefinitionSource SOURCE = DefinitionSource.builtin("setworks:dev-fixtures");
    private static final Logger LOGGER = LogManager.getLogger("Setworks/dev-fixtures");

    private DevFixtures() {
    }

    public static void register() {
        // A second registration of an existing id without the replace marker: rejected, the original stays.
        SetworksApi.registerSet(SetBuilder.set("setworks:iron_armor")
                .displayName("Duplicate Iron")
                .piece(PieceBuilder.piece("head").accept("minecraft:iron_helmet").at("head")), SOURCE);
        // Disable the full-set bonus of the highest-only example so the lower threshold applies with four pieces.
        SetworksApi.disableBonus("setworks:leather_silent", SOURCE);
        // Two listeners: the first throws on every activation, the second must still be called.
        SetworksApi.addBonusListener(new BonusListener() {
            @Override
            public void onActivated(EntityPlayer player, String bonusId, List<String> setIds) {
                throw new IllegalStateException("dev fixture listener throws on purpose for " + bonusId);
            }

            @Override
            public void onDeactivated(EntityPlayer player, String bonusId, List<String> setIds, BonusState state, String detail) {
            }
        });
        SetworksApi.addBonusListener(new BonusListener() {
            @Override
            public void onActivated(EntityPlayer player, String bonusId, List<String> setIds) {
                LOGGER.info("[setworks-dev] second listener saw activation of {} for {}", bonusId, player.getName());
            }

            @Override
            public void onDeactivated(EntityPlayer player, String bonusId, List<String> setIds, BonusState state, String detail) {
                LOGGER.info("[setworks-dev] second listener saw deactivation of {} ({})", bonusId, state.reasonText());
            }
        });
    }
}
