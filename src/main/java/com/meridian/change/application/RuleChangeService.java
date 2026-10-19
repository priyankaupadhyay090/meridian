package com.meridian.change.application;

import com.meridian.audit.application.AuditLog;
import com.meridian.change.domain.ChangeSource;
import com.meridian.change.domain.ChangeWorkflowException;
import com.meridian.change.domain.DryRun;
import com.meridian.change.domain.DryRunReport;
import com.meridian.change.domain.RuleChange;
import com.meridian.common.NotFoundException;
import com.meridian.routing.application.RuleSetStore;
import com.meridian.routing.domain.InvalidRulesException;
import com.meridian.routing.domain.RoutingRule;
import com.meridian.routing.domain.RuleSet;
import com.meridian.routing.domain.RuleValidator;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class RuleChangeService {

    private final RuleChangeRepository changes;
    private final RuleSetStore ruleSets;
    private final SampleShipmentSource shipments;
    private final AuditLog auditLog;
    private final Clock clock;
    private final RuleValidator validator = new RuleValidator();
    private final DryRun dryRun = new DryRun();

    public RuleChangeService(RuleChangeRepository changes, RuleSetStore ruleSets, SampleShipmentSource shipments,
                             AuditLog auditLog, Clock clock) {
        this.changes = changes;
        this.ruleSets = ruleSets;
        this.shipments = shipments;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    public RuleChange draft(String author, List<RoutingRule> proposedRules, ChangeSource source,
                            String sourceReference) {
        List<String> problems = validator.problemsIn(proposedRules);
        if (!problems.isEmpty()) {
            throw new InvalidRulesException(problems);
        }
        RuleChange change = new RuleChange("chg-" + UUID.randomUUID().toString().substring(0, 8), author, source,
                sourceReference, ruleSets.current().version(), proposedRules, clock.instant());
        changes.save(change);
        auditLog.record(author, "change.drafted", change.id(),
                "%d rules, source %s".formatted(proposedRules.size(), source));
        return change;
    }

    public DryRunReport testRun(List<String> removedRuleIds, List<RoutingRule> addedRules) {
        RuleSet current = ruleSets.current();
        List<RoutingRule> proposed = new ArrayList<>(current.rules().stream()
                .filter(rule -> !removedRuleIds.contains(rule.id()))
                .toList());
        proposed.addAll(addedRules);
        List<String> problems = validator.problemsIn(proposed);
        if (!problems.isEmpty()) {
            throw new InvalidRulesException(problems);
        }
        return dryRun.compare(current, proposed, shipments.recordedShipments());
    }

    public DryRunReport runDryRun(String changeId, String user) {
        RuleChange change = find(changeId);
        RuleSet current = ruleSets.current();
        if (current.version() != change.baseVersion()) {
            throw new ChangeWorkflowException("change %s was drafted against version %d; version %d is active now"
                    .formatted(changeId, change.baseVersion(), current.version()));
        }
        DryRunReport report = dryRun.compare(current, change.proposedRules(), shipments.recordedShipments());
        change.recordDryRun(report);
        changes.save(change);
        auditLog.record(user, "change.dry-run", changeId,
                "%d of %d shipments route differently, %d conflicts"
                        .formatted(report.changed(), report.evaluated(), report.conflicts().size()));
        return report;
    }

    public RuleChange submit(String changeId, String user) {
        RuleChange change = find(changeId);
        change.submit(user);
        changes.save(change);
        auditLog.record(user, "change.submitted", changeId, "waiting for a second person");
        return change;
    }

    public synchronized RuleChange approve(String changeId, String user, String comment) {
        RuleChange change = find(changeId);
        change.checkApprovableBy(user);
        RuleSet activated = ruleSets.activate(change.baseVersion(), change.proposedRules());
        change.approve(user, comment, activated.version());
        changes.save(change);
        auditLog.record(user, "change.approved", changeId, "rule set version %d is active".formatted(activated.version()));
        return change;
    }

    public RuleChange reject(String changeId, String user, String comment) {
        RuleChange change = find(changeId);
        change.reject(user, comment);
        changes.save(change);
        auditLog.record(user, "change.rejected", changeId, comment);
        return change;
    }

    public synchronized RuleSet rollback(String user) {
        RuleSet current = ruleSets.current();
        RuleSet previous = ruleSets.previous()
                .orElseThrow(() -> new ChangeWorkflowException("there is no earlier rule set to roll back to"));
        RuleSet restored = ruleSets.activate(current.version(), previous.rules());
        auditLog.record(user, "rules.rolled-back", "rule-set",
                "version %d restores the rules of version %d".formatted(restored.version(), previous.version()));
        return restored;
    }

    public RuleChange find(String changeId) {
        return changes.findById(changeId).orElseThrow(() -> new NotFoundException("change", changeId));
    }

    public List<RuleChange> all() {
        return changes.findAll().stream()
                .sorted(Comparator.comparing(RuleChange::createdAt).reversed())
                .toList();
    }
}
