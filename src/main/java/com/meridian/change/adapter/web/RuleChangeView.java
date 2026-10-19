package com.meridian.change.adapter.web;

import com.meridian.change.domain.ChangeSource;
import com.meridian.change.domain.ChangeStatus;
import com.meridian.change.domain.RuleChange;
import com.meridian.routing.domain.RoutingRule;

import java.time.Instant;
import java.util.List;

public record RuleChangeView(String id, String author, ChangeSource source, String sourceReference, int baseVersion,
                             ChangeStatus status, List<RoutingRule> proposedRules, DryRunView dryRun,
                             String reviewer, String reviewComment, Integer activatedVersion, Instant createdAt) {

    public static RuleChangeView of(RuleChange change) {
        return new RuleChangeView(change.id(), change.author(), change.source(), change.sourceReference(),
                change.baseVersion(), change.status(), change.proposedRules(), DryRunView.of(change.dryRun()), change.reviewer(),
                change.reviewComment(), change.activatedVersion(), change.createdAt());
    }
}
