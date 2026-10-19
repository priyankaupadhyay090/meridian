package com.meridian.contract.domain;

public record Clause(String reference, String text) {

    public String lowerText() {
        return text.toLowerCase();
    }
}
