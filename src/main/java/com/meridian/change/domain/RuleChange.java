package com.meridian.change.domain;

import com.meridian.routing.domain.RoutingRule;

import java.time.Instant;
import java.util.List;

public class RuleChange {

    private final String id;
    private final String author;
    private final ChangeSource source;
    private final String sourceReference;
    private final int baseVersion;
    private final List<RoutingRule> proposedRules;
    private final Instant createdAt;
    private ChangeStatus status = ChangeStatus.DRAFT;
    private DryRunReport dryRun;
    private String reviewer;
    private String reviewComment;
    private Integer activatedVersion;

    public RuleChange(String id, String author, ChangeSource source, String sourceReference, int baseVersion,
                      List<RoutingRule> proposedRules, Instant createdAt) {
        this.id = id;
        this.author = author;
        this.source = source;
        this.sourceReference = sourceReference;
        this.baseVersion = baseVersion;
        this.proposedRules = List.copyOf(proposedRules);
        this.createdAt = createdAt;
    }

    public void recordDryRun(DryRunReport report) {
        requireStatus(ChangeStatus.DRAFT, "run a dry run");
        this.dryRun = report;
    }

    public void submit(String user) {
        requireStatus(ChangeStatus.DRAFT, "submit");
        if (!author.equals(user)) {
            throw new ChangeWorkflowException("only the author %s can submit change %s".formatted(author, id));
        }
        if (dryRun == null) {
            throw new ChangeWorkflowException("change %s needs a dry run before it can be submitted".formatted(id));
        }
        if (!dryRun.passed()) {
            throw new ChangeWorkflowException("change %s has rule conflicts and cannot be submitted".formatted(id));
        }
        status = ChangeStatus.PENDING_APPROVAL;
    }

    public void checkApprovableBy(String user) {
        requireStatus(ChangeStatus.PENDING_APPROVAL, "approve");
        requireSecondPerson(user);
    }

    public void approve(String user, String comment, int activatedVersion) {
        checkApprovableBy(user);
        this.reviewer = user;
        this.reviewComment = comment;
        this.activatedVersion = activatedVersion;
        status = ChangeStatus.ACTIVE;
    }

    public void reject(String user, String comment) {
        requireStatus(ChangeStatus.PENDING_APPROVAL, "reject");
        requireSecondPerson(user);
        this.reviewer = user;
        this.reviewComment = comment;
        status = ChangeStatus.REJECTED;
    }

    private void requireSecondPerson(String user) {
        if (author.equals(user)) {
            throw new FourEyesViolationException(id, user);
        }
    }

    private void requireStatus(ChangeStatus expected, String action) {
        if (status != expected) {
            throw new ChangeWorkflowException("cannot %s change %s while it is %s".formatted(action, id, status));
        }
    }

    public String id() {
        return id;
    }

    public String author() {
        return author;
    }

    public ChangeSource source() {
        return source;
    }

    public String sourceReference() {
        return sourceReference;
    }

    public int baseVersion() {
        return baseVersion;
    }

    public List<RoutingRule> proposedRules() {
        return proposedRules;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public ChangeStatus status() {
        return status;
    }

    public DryRunReport dryRun() {
        return dryRun;
    }

    public String reviewer() {
        return reviewer;
    }

    public String reviewComment() {
        return reviewComment;
    }

    public Integer activatedVersion() {
        return activatedVersion;
    }
}
