package com.meridian.routing.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class RuleValidator {

    public static final int HIGHEST_PRIORITY = 1;
    public static final int LOWEST_PRIORITY = 1000;

    private static final Pattern PREFIX = Pattern.compile("\\d{1,5}");

    public List<String> problemsIn(RoutingRule rule) {
        List<String> problems = new ArrayList<>();
        RuleMatch match = rule.match();
        if (rule.id() == null || rule.id().isBlank()) {
            problems.add("rule id is missing");
        }
        if (match == null) {
            problems.add("rule %s has no match".formatted(rule.id()));
            return problems;
        }
        if (match.destinationPrefix() == null || !PREFIX.matcher(match.destinationPrefix()).matches()) {
            problems.add("rule %s needs a destination prefix of 1 to 5 digits".formatted(rule.id()));
        }
        if (match.weightBand() != null && !match.weightBand().isValid()) {
            problems.add("rule %s has an invalid weight band %s".formatted(rule.id(), match.weightBand()));
        }
        if (rule.priority() < HIGHEST_PRIORITY || rule.priority() > LOWEST_PRIORITY) {
            problems.add("rule %s needs a priority between %d and %d"
                    .formatted(rule.id(), HIGHEST_PRIORITY, LOWEST_PRIORITY));
        }
        if (rule.carrierService() == null) {
            problems.add("rule %s has no carrier service".formatted(rule.id()));
        }
        return problems;
    }

    public List<String> problemsIn(List<RoutingRule> rules) {
        List<String> problems = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (RoutingRule rule : rules) {
            problems.addAll(problemsIn(rule));
            if (rule.id() != null && !ids.add(rule.id())) {
                problems.add("rule id %s is used more than once".formatted(rule.id()));
            }
        }
        return problems;
    }

    public List<RuleConflict> conflictsIn(List<RoutingRule> rules) {
        List<RuleConflict> conflicts = new ArrayList<>();
        for (int i = 0; i < rules.size(); i++) {
            for (int j = i + 1; j < rules.size(); j++) {
                RoutingRule first = rules.get(i);
                RoutingRule second = rules.get(j);
                if (first.priority() == second.priority() && first.match().overlaps(second.match())) {
                    conflicts.add(RuleConflict.between(first, second));
                }
            }
        }
        return conflicts;
    }
}
