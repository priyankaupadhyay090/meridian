package com.meridian.change.domain;

import com.meridian.routing.domain.RuleConflict;

import java.util.List;

public record DryRunReport(int baseVersion, int evaluated, int changed, int unrouted,
                           List<RouteDifference> examples, List<RuleConflict> conflicts) {

    public DryRunReport {
        examples = List.copyOf(examples);
        conflicts = List.copyOf(conflicts);
    }

    public boolean passed() {
        return conflicts.isEmpty();
    }
}
