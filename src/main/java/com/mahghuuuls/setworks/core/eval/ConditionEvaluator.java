package com.mahghuuuls.setworks.core.eval;

import com.mahghuuuls.setworks.api.IConditionContext;
import com.mahghuuuls.setworks.api.definition.BonusDefinition;

/**
 * Evaluates the conditions of one bonus for one player. The evaluator calls it only when the
 * bonus's equipment is complete and its interval is due; it never runs on the client. The player is
 * the server entity in the game and {@code null} in tests.
 */
public interface ConditionEvaluator {

    enum Outcome { PASS, FAIL, ERROR }

    final class Result {
        private final Outcome outcome;
        private final String detail;

        public Result(Outcome outcome, String detail) {
            this.outcome = outcome;
            this.detail = detail == null ? "" : detail;
        }

        public Outcome outcome() {
            return outcome;
        }

        /** The failing or erroring condition's kind id, with the error text for an error. */
        public String detail() {
            return detail;
        }

        public static Result pass() {
            return new Result(Outcome.PASS, "");
        }
    }

    Result evaluate(BonusDefinition bonus, Object player, IConditionContext context);

    /** Passes every bonus without conditions and fails closed on any that has one; used before evaluation is wired. */
    ConditionEvaluator NONE = new ConditionEvaluator() {
        @Override
        public Result evaluate(BonusDefinition bonus, Object player, IConditionContext context) {
            if (bonus.conditions().isEmpty()) {
                return Result.pass();
            }
            return new Result(Outcome.ERROR, "no condition evaluator is installed");
        }
    };
}
