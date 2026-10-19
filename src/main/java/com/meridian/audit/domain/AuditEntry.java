package com.meridian.audit.domain;

import java.time.Instant;

public record AuditEntry(Instant at, String actor, String action, String subject, String detail) {
}
