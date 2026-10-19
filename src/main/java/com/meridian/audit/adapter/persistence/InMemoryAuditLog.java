package com.meridian.audit.adapter.persistence;

import com.meridian.audit.application.AuditLog;
import com.meridian.audit.domain.AuditEntry;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class InMemoryAuditLog implements AuditLog {

    private final List<AuditEntry> entries = new CopyOnWriteArrayList<>();
    private final Clock clock;

    public InMemoryAuditLog(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void record(String actor, String action, String subject, String detail) {
        entries.add(new AuditEntry(clock.instant(), actor, action, subject, detail));
    }

    @Override
    public List<AuditEntry> entries() {
        return entries.reversed();
    }
}
