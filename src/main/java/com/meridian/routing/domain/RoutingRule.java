package com.meridian.routing.domain;

public record RoutingRule(String id, RuleMatch match, int priority, CarrierService carrierService) {
}
