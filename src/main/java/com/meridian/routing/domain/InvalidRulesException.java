package com.meridian.routing.domain;

import java.util.List;

public class InvalidRulesException extends RuntimeException {

    private final List<String> problems;

    public InvalidRulesException(List<String> problems) {
        super("the rules are not valid: " + String.join("; ", problems));
        this.problems = List.copyOf(problems);
    }

    public List<String> problems() {
        return problems;
    }
}
