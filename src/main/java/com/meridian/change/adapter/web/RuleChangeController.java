package com.meridian.change.adapter.web;

import com.meridian.change.application.RuleChangeService;
import com.meridian.change.domain.ChangeSource;
import com.meridian.routing.domain.RuleSet;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/console")
public class RuleChangeController {

    private final RuleChangeService changes;

    public RuleChangeController(RuleChangeService changes) {
        this.changes = changes;
    }

    @PostMapping("/changes")
    @ResponseStatus(HttpStatus.CREATED)
    RuleChangeView draft(@Valid @RequestBody DraftChangeRequest request, Principal user) {
        return RuleChangeView.of(changes.draft(user.getName(), request.rules(), ChangeSource.MANUAL, null));
    }

    @GetMapping("/changes")
    List<RuleChangeView> all() {
        return changes.all().stream().map(RuleChangeView::of).toList();
    }

    @GetMapping("/changes/{id}")
    RuleChangeView one(@PathVariable String id) {
        return RuleChangeView.of(changes.find(id));
    }

    @PostMapping("/changes/{id}/dry-run")
    DryRunView dryRun(@PathVariable String id, Principal user) {
        return DryRunView.of(changes.runDryRun(id, user.getName()));
    }

    @PostMapping("/changes/{id}/submit")
    RuleChangeView submit(@PathVariable String id, Principal user) {
        return RuleChangeView.of(changes.submit(id, user.getName()));
    }

    @PostMapping("/changes/{id}/approve")
    RuleChangeView approve(@PathVariable String id, @Valid @RequestBody ReviewRequest request, Principal user) {
        return RuleChangeView.of(changes.approve(id, user.getName(), request.comment()));
    }

    @PostMapping("/changes/{id}/reject")
    RuleChangeView reject(@PathVariable String id, @Valid @RequestBody ReviewRequest request, Principal user) {
        return RuleChangeView.of(changes.reject(id, user.getName(), request.comment()));
    }

    @PostMapping("/rules/rollback")
    RuleSet rollback(Principal user) {
        return changes.rollback(user.getName());
    }
}
