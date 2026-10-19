package com.meridian.change.adapter.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.change.application.RuleChangeService;
import com.meridian.change.domain.RuleChangeExample;
import com.meridian.common.SeedResources;
import com.meridian.routing.application.RoutingService;
import com.meridian.routing.domain.RuleSet;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public")
public class RuleSandboxController {

    private final RuleChangeService changes;
    private final RoutingService routing;
    private final List<RuleChangeExample> examples;

    public RuleSandboxController(RuleChangeService changes, RoutingService routing, ObjectMapper mapper) {
        this.changes = changes;
        this.routing = routing;
        this.examples = List.copyOf(SeedResources.read(mapper, "seed/rule-change-examples.json",
                new TypeReference<List<RuleChangeExample>>() {
                }));
    }

    @GetMapping("/rules")
    RuleSet activeRules() {
        return routing.activeRules();
    }

    @GetMapping("/rule-changes/examples")
    List<RuleChangeExample> examples() {
        return examples;
    }

    @PostMapping("/rule-changes/test-run")
    DryRunView testRun(@Valid @RequestBody TestRunRequest request) {
        return DryRunView.of(changes.testRun(request.removeOrEmpty(), request.addOrEmpty()));
    }
}
