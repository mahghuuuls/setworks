package com.mahghuuuls.setworks.examples;

import com.mahghuuuls.setworks.api.BonusBuilder;
import com.mahghuuuls.setworks.api.BonusListener;
import com.mahghuuuls.setworks.api.BonusState;
import com.mahghuuuls.setworks.api.CombinationRule;
import com.mahghuuuls.setworks.api.DefinitionSource;
import com.mahghuuuls.setworks.api.ICondition;
import com.mahghuuuls.setworks.api.IConditionContext;
import com.mahghuuuls.setworks.api.IConditionKind;
import com.mahghuuuls.setworks.api.IEffectKind;
import com.mahghuuuls.setworks.api.PieceBuilder;
import com.mahghuuuls.setworks.api.SetBuilder;
import com.mahghuuuls.setworks.api.SetworksApi;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Development-only fixtures, registered only when the system property
 * {@code setworks.invalidExample} is true. They exist so an operator can see, on a running game,
 * the validation, listener, and addon behavior that ordinary examples cannot show: a duplicate
 * registration that is rejected, a disabled bonus, a listener that throws beside one that keeps
 * working, an addon effect kind that logs every call with its instance id, and an effect kind that
 * fails, a condition kind that always throws, a condition kind that throws only while sneaking (so an
 * error episode can end and start again), two Speed levels from two bonuses, and a bonus with two
 * conditions. Never enabled in a normal pack.
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

        // An addon effect kind that only logs: every apply and withdraw with its stable instance id.
        SetworksApi.registerEffectKind(new IEffectKind() {
            @Override
            public String id() {
                return "fixture:counter";
            }

            @Override
            public CombinationRule combinationRule() {
                return CombinationRule.SUM;
            }

            @Override
            public String describe(Map<String, String> params) {
                return "counter +" + params.get("amount");
            }

            @Override
            public void apply(EntityPlayerMP player, String instanceId, Map<String, String> params) {
                LOGGER.info("[setworks-dev] counter apply id={} amount={} player={}", instanceId, params.get("amount"), player.getName());
            }

            @Override
            public void withdraw(EntityPlayerMP player, String instanceId, Map<String, String> params) {
                LOGGER.info("[setworks-dev] counter withdraw id={} amount={} player={}", instanceId, params.get("amount"), player.getName());
            }
        });
        // An effect kind whose apply always fails: the bonus stays active, the effect is recorded as failed.
        SetworksApi.registerEffectKind(new IEffectKind() {
            @Override
            public String id() {
                return "fixture:fails";
            }

            @Override
            public CombinationRule combinationRule() {
                return CombinationRule.REPLACE;
            }

            @Override
            public String describe(Map<String, String> params) {
                return "always fails";
            }

            @Override
            public void apply(EntityPlayerMP player, String instanceId, Map<String, String> params) {
                throw new IllegalStateException("dev fixture effect fails on purpose");
            }

            @Override
            public void withdraw(EntityPlayerMP player, String instanceId, Map<String, String> params) {
            }
        });
        // Two bonuses on the counter kind: with two or more iron pieces both are active, two apply calls, distinct ids.
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:fixture_count_one")
                .requires("setworks:iron_armor", 1)
                .effect("fixture:counter", Collections.singletonMap("amount", "1"))
                .description("Fixture Counter One"), SOURCE);
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:fixture_count_two")
                .requires("setworks:iron_armor", 2)
                .effect("fixture:counter", Collections.singletonMap("amount", "2"))
                .description("Fixture Counter Two"), SOURCE);
        // A condition kind that always throws: its bonus reads "condition error", logged once per error episode.
        SetworksApi.registerConditionKind(new IConditionKind() {
            @Override
            public String id() {
                return "fixture:throws";
            }

            @Override
            public ICondition create(Map<String, String> params) {
                return new ICondition() {
                    @Override
                    public boolean test(EntityPlayerMP player, IConditionContext context) {
                        throw new IllegalStateException("dev fixture condition throws on purpose");
                    }
                };
            }

            @Override
            public String describe(Map<String, String> params) {
                return "always throws";
            }
        });
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:fixture_throwing_condition")
                .requires("setworks:iron_armor", 1)
                .condition("fixture:throws", Collections.<String, String>emptyMap())
                .description("Fixture Throwing Condition"), SOURCE);
        // Throws only while the player sneaks: standing up ends the error episode, sneaking again starts a new one (a second warning).
        SetworksApi.registerConditionKind(new IConditionKind() {
            @Override
            public String id() {
                return "fixture:throws_when_sneaking";
            }

            @Override
            public ICondition create(Map<String, String> params) {
                return new ICondition() {
                    @Override
                    public boolean test(EntityPlayerMP player, IConditionContext context) {
                        if (player.isSneaking()) {
                            throw new IllegalStateException("dev fixture condition throws while sneaking");
                        }
                        return true;
                    }
                };
            }

            @Override
            public String describe(Map<String, String> params) {
                return "throws while sneaking";
            }
        });
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:fixture_recovering_condition")
                .requires("setworks:iron_armor", 1)
                .condition("fixture:throws_when_sneaking", Collections.<String, String>emptyMap())
                .description("Fixture Recovering Condition"), SOURCE);
        // Two Setworks sources of one potion at different levels: the higher wins while both are active, the lower returns when it withdraws.
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:fixture_speed_one")
                .requires("setworks:iron_armor", 2)
                .potion("minecraft:speed", 1, false)
                .description("Fixture Speed One"), SOURCE);
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:fixture_speed_two")
                .requiresAll("setworks:iron_armor")
                .potion("minecraft:speed", 2, false)
                .description("Fixture Speed Two"), SOURCE);
        // Description given as a language key: the client translates it from the mod's language file.
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:fixture_keyed")
                .requires("setworks:iron_armor", 1)
                .description("setworks.fixture.keyed.description"), SOURCE);
        // No description: the client shows the generated one ("+2 Armor, ${effect.moveSpeed} II" translated).
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:fixture_generated")
                .requires("setworks:iron_armor", 2)
                .attribute("generic.armor", 2, "add")
                .potion("minecraft:speed", 2, false), SOURCE);
        // Two conditions: both must pass; the first failing one names itself in the reason.
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:fixture_two_conditions")
                .requires("setworks:iron_armor", 1)
                .condition("setworks:health_below", Collections.singletonMap("fraction", "0.5"))
                .condition("setworks:sneaking", Collections.<String, String>emptyMap())
                .description("Fixture Two Conditions"), SOURCE);
        SetworksApi.registerBonus(BonusBuilder.bonus("setworks:fixture_failing")
                .requires("setworks:iron_armor", 1)
                .effect("fixture:fails", Collections.<String, String>emptyMap())
                .description("Fixture Failing Effect"), SOURCE);
    }
}
