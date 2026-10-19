package com.meridian.change.adapter.web;

import com.meridian.change.domain.DryRunReport;
import com.meridian.change.domain.RouteDifference;
import com.meridian.routing.domain.RuleConflict;

import java.util.List;

public record DryRunView(int baseVersion, int evaluated, int changed, int unrouted, boolean passed,
                         List<RouteDifference> examples, List<RuleConflict> conflicts) {

    public static DryRunView of(DryRunReport report) {
        return report == null ? null : new DryRunView(report.baseVersion(), report.evaluated(), report.changed(),
                report.unrouted(), report.passed(), report.examples(), report.conflicts());
    }
}
