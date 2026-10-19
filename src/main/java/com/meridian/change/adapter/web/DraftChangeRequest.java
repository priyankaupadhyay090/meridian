package com.meridian.change.adapter.web;

import com.meridian.routing.domain.RoutingRule;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DraftChangeRequest(@NotEmpty List<RoutingRule> rules) {
}
