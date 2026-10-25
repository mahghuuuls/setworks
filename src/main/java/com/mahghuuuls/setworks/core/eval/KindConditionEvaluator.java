package com.mahghuuuls.setworks.core.eval;

import com.mahghuuuls.setworks.api.ICondition;
import com.mahghuuuls.setworks.api.IConditionContext;
import com.mahghuuuls.setworks.api.IConditionKind;
import com.mahghuuuls.setworks.api.definition.BonusDefinition;
import com.mahghuuuls.setworks.api.definition.ConditionSpec;
import com.mahghuuuls.setworks.core.match.CompiledDefinitions;
import net.minecraft.entity.player.EntityPlayerMP;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The condition evaluator of the game: one {@link ICondition} per condition of every enabled bonus,
 * created once from the registered kinds (or taken as supplied for a direct condition), tested in
 * order with AND semantics. The first failing condition decides the result and names itself by
 * kind id. A condition that throws gives "condition error": the error is logged once, naming the
 * bonus, the condition, and the error, and not again until that condition evaluates without error.
 * The error episode belongs to the condition, not to the player: one instance serves every player,
 * so a condition that throws for one player and evaluates without error for another is logged again
 * after each such evaluation.
 */
public final class KindConditionEvaluator implements ConditionEvaluator {

    private static final class Compiled {
        final ConditionSpec spec;
        final ICondition condition;
        final String creationProblem;

        Compiled(ConditionSpec spec, ICondition condition, String creationProblem) {
            this.spec = spec;
            this.condition = condition;
            this.creationProblem = creationProblem;
        }
    }

    private final Map<String, List<Compiled>> byBonus = new LinkedHashMap<String, List<Compiled>>();
    private final ConditionKindRegistry kinds;
    private final Logger logger;
    private final Set<String> erroring = new HashSet<String>();
    private int warningsLogged;

    public KindConditionEvaluator(CompiledDefinitions definitions, ConditionKindRegistry kinds, Logger logger) {
        this.kinds = kinds;
        this.logger = logger;
        for (BonusDefinition bonus : definitions.bonuses()) {
            List<Compiled> compiled = new ArrayList<Compiled>();
            for (ConditionSpec spec : bonus.conditions()) {
                compiled.add(compile(spec));
            }
            byBonus.put(bonus.id(), Collections.unmodifiableList(compiled));
        }
    }

    private Compiled compile(ConditionSpec spec) {
        if (spec.instance() != null) {
            return new Compiled(spec, spec.instance(), null);
        }
        IConditionKind kind = kinds.get(spec.kindId());
        if (kind == null) {
            return new Compiled(spec, null, "condition kind " + spec.kindId() + " is not registered");
        }
        try {
            ICondition condition = kind.create(spec.params());
            if (condition == null) {
                return new Compiled(spec, null, "condition kind " + spec.kindId() + " created nothing");
            }
            return new Compiled(spec, condition, null);
        } catch (RuntimeException e) {
            return new Compiled(spec, null, "condition kind " + spec.kindId() + " failed to create: " + e);
        }
    }

    @Override
    public Result evaluate(BonusDefinition bonus, Object player, IConditionContext context) {
        List<Compiled> compiled = byBonus.get(bonus.id());
        if (compiled == null || compiled.isEmpty()) {
            return Result.pass();
        }
        EntityPlayerMP entity = player instanceof EntityPlayerMP ? (EntityPlayerMP) player : null;
        for (int index = 0; index < compiled.size(); index++) {
            Compiled item = compiled.get(index);
            String key = bonus.id() + "#" + index;
            if (item.condition == null) {
                warnOnce(key, bonus, item.spec, item.creationProblem);
                return new Result(Outcome.ERROR, item.spec.kindId() + ": " + item.creationProblem);
            }
            boolean passed;
            try {
                passed = item.condition.test(entity, context);
            } catch (Exception | LinkageError t) {
                // Errors of the JVM itself (out of memory, stack overflow) are not a condition's fault and pass through.
                warnOnce(key, bonus, item.spec, t.toString());
                return new Result(Outcome.ERROR, item.spec.kindId() + ": " + t);
            }
            erroring.remove(key);
            if (!passed) {
                return new Result(Outcome.FAIL, item.spec.kindId());
            }
        }
        return Result.pass();
    }

    private void warnOnce(String key, BonusDefinition bonus, ConditionSpec spec, String error) {
        if (erroring.add(key)) {
            warningsLogged++;
            logger.warn("condition {} of bonus {} failed with an error; the bonus stays inactive until it evaluates without error (logged once): {}",
                    spec.kindId(), bonus.id(), error);
        }
    }

    /** How many error warnings were logged so far; one per condition per error episode. */
    public int warningsLogged() {
        return warningsLogged;
    }

    /** True while the condition at {@code index} of the bonus is in an error episode. */
    public boolean isErroring(String bonusId, int index) {
        return erroring.contains(bonusId + "#" + index);
    }

    /** An addon's description, or its failure text; one broken describe must not break the inspect command. */
    private static String safeDescribe(IConditionKind kind, Map<String, String> params) {
        try {
            String text = kind.describe(params);
            return text == null ? "" : text;
        } catch (RuntimeException e) {
            return "(describe threw " + e + ")";
        }
    }

    /** One line per condition of the bonus: kind id and the kind's description, or the label of a direct condition. */
    public List<String> conditionLines(BonusDefinition bonus) {
        List<String> lines = new ArrayList<String>();
        List<Compiled> compiled = byBonus.get(bonus.id());
        if (compiled == null) {
            return lines;
        }
        for (int index = 0; index < compiled.size(); index++) {
            Compiled item = compiled.get(index);
            StringBuilder line = new StringBuilder("  condition ").append(index).append(": ").append(item.spec.kindId());
            if (item.spec.instance() != null) {
                line.append(" (direct)");
            } else {
                IConditionKind kind = kinds.get(item.spec.kindId());
                if (kind != null) {
                    line.append(' ').append(safeDescribe(kind, item.spec.params()));
                }
            }
            if (item.creationProblem != null) {
                line.append(" [").append(item.creationProblem).append(']');
            } else if (isErroring(bonus.id(), index)) {
                line.append(" [error]");
            }
            lines.add(line.toString());
        }
        return lines;
    }
}
