package com.meridian.contract.eval;

import com.meridian.compliance.domain.ContractContext;
import com.meridian.compliance.domain.Finding;
import com.meridian.compliance.domain.SenderType;
import com.meridian.compliance.domain.Severity;
import com.meridian.compliance.domain.TermsBasis;
import com.meridian.contract.domain.ContractImport;
import com.meridian.contract.domain.RuleProposal;
import com.meridian.routing.domain.CarrierService;
import com.meridian.routing.domain.ServiceLevel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContractEvalScorerTest {

    private static final EvalCase CASE = new EvalCase("case", "DPD", "text", SenderType.BUSINESS,
            TermsBasis.STANDARD_TERMS,
            List.of(new ExpectedRule("10", ServiceLevel.EXPRESS, null, null, null, CarrierService.DPD_EXPRESS)),
            List.of("9"), List.of("HGB-449-2-LIABILITY"));

    private final ContractEvalScorer scorer = new ContractEvalScorer();

    private static RuleProposal proposal(String prefix, CarrierService service) {
        return new RuleProposal(prefix, ServiceLevel.EXPRESS, null, null, 100, service, "quote");
    }

    private static Finding finding(String ruleId) {
        return new Finding(ruleId, Severity.VIOLATION, "ref", "message", null, "source");
    }

    private static ContractImport result(List<RuleProposal> accepted, List<Finding> findings) {
        return new ContractImport("id", "DPD", "olivia", Instant.EPOCH,
                new ContractContext(SenderType.BUSINESS, TermsBasis.STANDARD_TERMS, LocalDate.EPOCH), accepted,
                List.of(), List.of(), List.of(), findings, List.of(), "rules", null, null);
    }

    @Test
    void exactRuleAndExpectedFindingArePerfect() {
        CaseScore score = scorer.score(CASE, result(List.of(proposal("10", CarrierService.DPD_EXPRESS)),
                List.of(finding("HGB-449-2-LIABILITY"))));

        assertThat(score.perfect()).isTrue();
    }

    @Test
    void wrongCarrierIsCountedAsWrongAndMissed() {
        CaseScore score = scorer.score(CASE, result(List.of(proposal("10", CarrierService.DHL_EXPRESS)), List.of()));

        assertThat(score.wrong()).isEqualTo(1);
        assertThat(score.missed()).isEqualTo(1);
        assertThat(score.perfect()).isFalse();
    }

    @Test
    void ruleForATrapClauseIsReported() {
        CaseScore score = scorer.score(CASE, result(List.of(proposal("10", CarrierService.DPD_EXPRESS),
                proposal("9", CarrierService.GLS_EXPRESS)), List.of(finding("HGB-449-2-LIABILITY"))));

        assertThat(score.trapViolations()).isEqualTo(1);
    }

    @Test
    void reportShowsPrecisionAndRecall() {
        String report = scorer.report(List.of(new CaseScore("case", 2, 2, 1, 1, 0, 1, 1, 1)));

        assertThat(report).contains("0.50 (1/2)", "1.00 (1/1)", "| case | 2 | 2 | 1 | 1 | 0 | 1 | 1 | 1 |");
    }
}
