package com.meridian.routing.adapter.persistence;

import com.meridian.routing.application.RuleSetStore;
import com.meridian.routing.domain.RoutingRule;
import com.meridian.routing.domain.RuleSet;
import com.meridian.routing.domain.StaleRuleSetException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryRuleSetStore implements RuleSetStore {

    private final List<RuleSet> versions = new ArrayList<>();

    public InMemoryRuleSetStore(List<RoutingRule> initialRules) {
        versions.add(new RuleSet(1, initialRules));
    }

    @Override
    public synchronized RuleSet current() {
        return versions.getLast();
    }

    @Override
    public synchronized RuleSet activate(int expectedCurrentVersion, List<RoutingRule> rules) {
        RuleSet current = current();
        if (current.version() != expectedCurrentVersion) {
            throw new StaleRuleSetException(expectedCurrentVersion, current.version());
        }
        RuleSet next = new RuleSet(current.version() + 1, rules);
        versions.add(next);
        return next;
    }

    @Override
    public synchronized Optional<RuleSet> previous() {
        return versions.size() < 2 ? Optional.empty() : Optional.of(versions.get(versions.size() - 2));
    }

    @Override
    public synchronized List<RuleSet> history() {
        return List.copyOf(versions).reversed();
    }
}
