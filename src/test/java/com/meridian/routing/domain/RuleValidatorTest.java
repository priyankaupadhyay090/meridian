package com.meridian.routing.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleValidatorTest {

    private final RuleValidator validator = new RuleValidator();

    @Test
    void validRuleHasNoProblems() {
        RoutingRule rule = new RoutingRule("r1", new RuleMatch("10", null, new WeightBand(0, 5000), null), 100,
                CarrierService.DHL_PAKET);

        assertThat(validator.problemsIn(rule)).isEmpty();
    }

    @Test
    void reportsEveryProblemInARule() {
        RoutingRule rule = new RoutingRule("r1", new RuleMatch("1A", null, new WeightBand(5000, 100), null), 0, null);

        assertThat(validator.problemsIn(rule)).hasSize(4);
    }

    @Test
    void duplicateRuleIdsAreRejected() {
        RoutingRule first = new RoutingRule("same", new RuleMatch("1", null, null, null), 1, CarrierService.DHL_PAKET);
        RoutingRule second = new RoutingRule("same", new RuleMatch("2", null, null, null), 1, CarrierService.DHL_PAKET);

        assertThat(validator.problemsIn(List.of(first, second))).containsExactly("rule id same is used more than once");
    }

    @Test
    void samePriorityAndOverlappingMatchIsAConflictNamingBothRules() {
        RoutingRule first = new RoutingRule("a", new RuleMatch("10", null, new WeightBand(0, 5000), null), 100,
                CarrierService.DHL_PAKET);
        RoutingRule second = new RoutingRule("b", new RuleMatch("10", null, new WeightBand(2000, 9000), null), 100,
                CarrierService.DPD_CLASSIC);

        assertThat(validator.conflictsIn(List.of(first, second)))
                .singleElement()
                .satisfies(conflict -> assertThat(conflict.message()).contains("a", "b"));
    }

    @Test
    void differentServiceLevelsDoNotConflict() {
        RoutingRule standard = new RoutingRule("a", new RuleMatch("10", ServiceLevel.STANDARD, null, null), 100,
                CarrierService.DHL_PAKET);
        RoutingRule express = new RoutingRule("b", new RuleMatch("10", ServiceLevel.EXPRESS, null, null), 100,
                CarrierService.DPD_EXPRESS);

        assertThat(validator.conflictsIn(List.of(standard, express))).isEmpty();
    }
}
