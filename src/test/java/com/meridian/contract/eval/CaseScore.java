package com.meridian.contract.eval;

public record CaseScore(String caseId, int expected, int accepted, int correct, int unresolved, int trapViolations,
                        int expectedFindings, int foundFindings, int correctFindings) {

    public int missed() {
        return expected - correct;
    }

    public int wrong() {
        return accepted - correct;
    }

    public boolean perfect() {
        return missed() == 0 && wrong() == 0 && trapViolations == 0
                && correctFindings == expectedFindings && foundFindings == expectedFindings;
    }
}
