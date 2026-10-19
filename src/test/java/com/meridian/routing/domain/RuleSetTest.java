package com.meridian.routing.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleSetTest {

    private static RoutingRule rule(String id, String prefix, int priority, CarrierService service) {
        return new RoutingRule(id, new RuleMatch(prefix, null, null, null), priority, service);
    }

    private static ShipmentProfile parcelTo(String postcode) {
        return new ShipmentProfile(postcode, ServiceLevel.STANDARD, 1000, CustomerTier.BASIC);
    }

    @Test
    void longestMatchingPrefixWins() {
        RuleSet rules = new RuleSet(1, List.of(
                rule("broad", "1", 1, CarrierService.DHL_PAKET),
                rule("narrow", "104", 900, CarrierService.GLS_EXPRESS)));

        assertThat(rules.resolve(parcelTo("10437"))).map(RoutingDecision::ruleId).contains("narrow");
    }

    @Test
    void priorityBreaksTiesBetweenEquallySpecificRules() {
        RuleSet rules = new RuleSet(1, List.of(
                rule("low", "10", 200, CarrierService.DHL_PAKET),
                rule("high", "10", 50, CarrierService.DPD_CLASSIC)));

        assertThat(rules.resolve(parcelTo("10115"))).map(RoutingDecision::carrierService)
                .contains(CarrierService.DPD_CLASSIC);
    }

    @Test
    void ruleLimitedToAServiceLevelOnlyMatchesThatLevel() {
        RoutingRule express = new RoutingRule("express", new RuleMatch("1", ServiceLevel.EXPRESS, null, null), 10,
                CarrierService.DPD_EXPRESS);
        RuleSet rules = new RuleSet(1, List.of(rule("base", "1", 500, CarrierService.DHL_PAKET), express));

        assertThat(rules.resolve(parcelTo("12000"))).map(RoutingDecision::ruleId).contains("base");
        assertThat(rules.resolve(new ShipmentProfile("12000", ServiceLevel.EXPRESS, 1000, CustomerTier.BASIC)))
                .map(RoutingDecision::ruleId).contains("express");
    }

    @Test
    void weightBandIncludesItsLowerLimitAndExcludesItsUpperLimit() {
        WeightBand band = new WeightBand(1000, 5000);

        assertThat(band.contains(1000)).isTrue();
        assertThat(band.contains(4999)).isTrue();
        assertThat(band.contains(5000)).isFalse();
    }

    @Test
    void noMatchingRuleMeansNoRoute() {
        RuleSet rules = new RuleSet(1, List.of(rule("only", "9", 1, CarrierService.DHL_PAKET)));

        assertThat(rules.resolve(parcelTo("10115"))).isEmpty();
    }
}
