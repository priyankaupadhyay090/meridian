package com.meridian.routing.domain;

public class StaleRuleSetException extends RuntimeException {

    public StaleRuleSetException(int expectedVersion, int currentVersion) {
        super("the change was prepared against version %d but version %d is active now"
                .formatted(expectedVersion, currentVersion));
    }
}
