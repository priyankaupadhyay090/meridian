package com.meridian.routing.application;

import com.meridian.routing.domain.RoutingRule;
import com.meridian.routing.domain.RuleSet;

import java.util.List;
import java.util.Optional;

public interface RuleSetStore {

    RuleSet current();

    RuleSet activate(int expectedCurrentVersion, List<RoutingRule> rules);

    Optional<RuleSet> previous();

    List<RuleSet> history();
}
