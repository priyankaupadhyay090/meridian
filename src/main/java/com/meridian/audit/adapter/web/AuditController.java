package com.meridian.audit.adapter.web;

import com.meridian.audit.application.AuditLog;
import com.meridian.audit.domain.AuditEntry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/console")
public class AuditController {

    private final AuditLog auditLog;

    public AuditController(AuditLog auditLog) {
        this.auditLog = auditLog;
    }

    @GetMapping("/audit-log")
    List<AuditEntry> entries() {
        return auditLog.entries();
    }
}
