package com.meridian.routing.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public record RuleSet(int version, List<RoutingRule> rules) {

    private static final Comparator<RoutingRule> MOST_SPECIFIC_FIRST = Comparator
            .comparingInt((RoutingRule rule) -> rule.match().specificity()).reversed()
            .thenComparingInt(RoutingRule::priority)
            .thenComparing(RoutingRule::id);

    public RuleSet {
        rules = List.copyOf(rules);
    }

    public Optional<RoutingDecision> resolve(ShipmentProfile profile) {
        return rules.stream()
                .filter(rule -> rule.match().matches(profile))
                .min(MOST_SPECIFIC_FIRST)
                .map(rule -> new RoutingDecision(rule.id(), rule.carrierService()));
    }
}
