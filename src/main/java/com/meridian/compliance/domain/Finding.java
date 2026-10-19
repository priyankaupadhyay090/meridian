package com.meridian.compliance.domain;

public record Finding(String ruleId, Severity severity, String lawReference, String message, String clause,
                      String source) {
}
