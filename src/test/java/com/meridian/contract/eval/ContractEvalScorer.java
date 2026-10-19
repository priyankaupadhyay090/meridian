package com.meridian.contract.eval;

import com.meridian.contract.domain.ContractImport;
import com.meridian.contract.domain.RuleProposal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ContractEvalScorer {

    public CaseScore score(EvalCase evalCase, ContractImport result) {
        List<ExpectedRule> unmatched = new ArrayList<>(evalCase.expected());
        int correct = 0;
        for (RuleProposal proposal : result.accepted()) {
            for (ExpectedRule expected : unmatched) {
                if (expected.matches(proposal)) {
                    unmatched.remove(expected);
                    correct++;
                    break;
                }
            }
        }
        int trapViolations = (int) result.accepted().stream()
                .filter(proposal -> evalCase.traps().contains(proposal.destinationPrefix()))
                .count();
        List<String> found = result.findings().stream().map(finding -> finding.ruleId()).toList();
        int correctFindings = (int) found.stream().filter(evalCase.expectedFindings()::contains).count();
        return new CaseScore(evalCase.id(), evalCase.expected().size(), result.accepted().size(), correct,
                result.unresolved().size(), trapViolations, evalCase.expectedFindings().size(), found.size(),
                correctFindings);
    }

    public String report(List<CaseScore> scores) {
        int expected = scores.stream().mapToInt(CaseScore::expected).sum();
        int accepted = scores.stream().mapToInt(CaseScore::accepted).sum();
        int correct = scores.stream().mapToInt(CaseScore::correct).sum();
        int traps = scores.stream().mapToInt(CaseScore::trapViolations).sum();
        int expectedFindings = scores.stream().mapToInt(CaseScore::expectedFindings).sum();
        int foundFindings = scores.stream().mapToInt(CaseScore::foundFindings).sum();
        int correctFindings = scores.stream().mapToInt(CaseScore::correctFindings).sum();
        long perfect = scores.stream().filter(CaseScore::perfect).count();
        StringBuilder out = new StringBuilder()
                .append("# Contract reading eval\n\n")
                .append("| Metric | Value |\n|---|---|\n")
                .append("| Cases fully correct | ").append(perfect).append(" of ").append(scores.size()).append(" |\n")
                .append("| Precision of accepted proposals | ").append(ratio(correct, accepted)).append(" |\n")
                .append("| Recall of expected rules | ").append(ratio(correct, expected)).append(" |\n")
                .append("| Trap clauses turned into rules | ").append(traps).append(" |\n")
                .append("| Precision of legal findings | ").append(ratio(correctFindings, foundFindings)).append(" |\n")
                .append("| Recall of legal findings | ").append(ratio(correctFindings, expectedFindings)).append(" |\n\n")
                .append("| Case | Expected rules | Proposed | Correct | Left for a person | Traps hit | Legal findings expected | Found | Correct |\n")
                .append("|---|---|---|---|---|---|---|---|---|\n");
        for (CaseScore s : scores) {
            out.append("| %s | %d | %d | %d | %d | %d | %d | %d | %d |\n".formatted(s.caseId(), s.expected(),
                    s.accepted(), s.correct(), s.unresolved(), s.trapViolations(), s.expectedFindings(),
                    s.foundFindings(), s.correctFindings()));
        }
        return out.toString();
    }

    private static String ratio(int part, int whole) {
        return whole == 0 ? "n/a" : String.format(Locale.ROOT, "%.2f (%d/%d)", (double) part / whole, part, whole);
    }
}
