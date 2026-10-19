package com.meridian.contract.domain;

public record AssistedReading(Clause clause, AssistOutcome outcome, RuleProposal proposal, String reason) {

    static AssistedReading accepted(Clause clause, RuleProposal proposal, String reason) {
        return new AssistedReading(clause, AssistOutcome.RULE_ACCEPTED, proposal, reason);
    }

    static AssistedReading outcome(Clause clause, AssistOutcome outcome, String reason) {
        return new AssistedReading(clause, outcome, null, reason);
    }

    public static AssistedReading noAnswer(Clause clause, String reason) {
        return new AssistedReading(clause, AssistOutcome.NO_ANSWER, null, reason);
    }
}
