package com.meridian.routing.domain;

public record RuleConflict(String firstRuleId, String secondRuleId, String message) {

    public static RuleConflict between(RoutingRule first, RoutingRule second) {
        return new RuleConflict(first.id(), second.id(),
                "Rules %s and %s have the same priority %d and overlapping matches for prefix %s"
                        .formatted(first.id(), second.id(), first.priority(), first.match().destinationPrefix()));
    }
}
