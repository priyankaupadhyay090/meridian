package com.meridian.change.domain;

import com.meridian.routing.domain.RoutingRule;

import java.util.List;

public record RuleChangeExample(String id, String title, String description, List<String> remove,
                                List<RoutingRule> add) {
}
