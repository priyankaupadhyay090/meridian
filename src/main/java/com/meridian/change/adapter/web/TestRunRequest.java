package com.meridian.change.adapter.web;

import com.meridian.routing.domain.RoutingRule;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TestRunRequest(@Size(max = 50) List<String> remove, @Size(max = 50) List<RoutingRule> add) {

    List<String> removeOrEmpty() {
        return remove == null ? List.of() : remove;
    }

    List<RoutingRule> addOrEmpty() {
        return add == null ? List.of() : add;
    }
}
