package com.mahghuuuls.setworks.core.eval;

import com.mahghuuuls.setworks.api.definition.BonusDefinition;

/**
 * Evaluates the conditions of one bonus for one player. The evaluator calls it only when the
 * bonus's equipment is complete and its interval is due; it never runs on the client.
 * Until condition kinds exist, the default passes every bonus that has no conditions and fails
 * closed on any that has one.
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

        public String detail() {
            return detail;
        }

        public static Result pass() {
            return new Result(Outcome.PASS, "");
        }
    }

    Result evaluate(BonusDefinition bonus, Object player);

    ConditionEvaluator NONE = new ConditionEvaluator() {
        @Override
        public Result evaluate(BonusDefinition bonus, Object player) {
            if (bonus.conditions().isEmpty()) {
                return Result.pass();
            }
            return new Result(Outcome.ERROR, "no condition evaluator is installed");
        }
    };
}
