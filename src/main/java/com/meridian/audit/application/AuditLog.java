package com.meridian.audit.application;

import com.meridian.audit.domain.AuditEntry;

import java.util.List;

public interface AuditLog {

    void record(String actor, String action, String subject, String detail);

    List<AuditEntry> entries();
}
