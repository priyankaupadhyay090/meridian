package com.meridian.contract.domain;

public record ClauseOutcome(RuleProposal proposal, UnresolvedClause unresolved) {

    public static ClauseOutcome proposal(RuleProposal proposal) {
        return new ClauseOutcome(proposal, null);
    }

    public static ClauseOutcome unresolved(Clause clause, String reason) {
        return new ClauseOutcome(null, new UnresolvedClause(clause, reason));
    }
}
