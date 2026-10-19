package com.meridian.library.application;

import com.meridian.audit.application.AuditLog;
import com.meridian.common.NotFoundException;
import com.meridian.library.domain.Business;
import com.meridian.library.domain.ContractFile;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class ContractLibraryService {

    public static final int MAX_FILES_PER_BUSINESS = 50;

    private final ContractFileStore store;
    private final ContractFileRepository files;
    private final AuditLog auditLog;
    private final Clock clock;

    public ContractLibraryService(ContractFileStore store, ContractFileRepository files, AuditLog auditLog,
                                  Clock clock) {
        this.store = store;
        this.files = files;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    public ContractFile upload(Business business, String user, NewContractFile file) {
        String hash = sha256(file.content());
        List<ContractFile> own = ofBusiness(business.id());
        var existing = own.stream().filter(candidate -> candidate.sha256().equals(hash)).findFirst();
        if (existing.isPresent()) {
            return existing.get();
        }
        if (own.size() >= MAX_FILES_PER_BUSINESS) {
            throw new IllegalArgumentException("a business can keep at most %d contracts; delete one first"
                    .formatted(MAX_FILES_PER_BUSINESS));
        }
        String id = "doc-" + UUID.randomUUID().toString().substring(0, 8);
        String key = "businesses/%s/contracts/%s/%s".formatted(business.id(), id, safeName(file.fileName()));
        store.put(key, file.content(), file.contentType());
        ContractFile stored = new ContractFile(id, business.id(), safeName(file.fileName()), file.format(),
                file.contentType(), file.content().length, hash, file.carrierName(), key, user, clock.instant());
        files.save(stored);
        auditLog.record(user, "contract-file.uploaded", id, "%s for %s".formatted(stored.fileName(), business.name()));
        return stored;
    }

    public List<ContractFile> ofBusiness(String businessId) {
        return files.findAll().stream()
                .filter(file -> file.belongsTo(businessId))
                .sorted(Comparator.comparing(ContractFile::uploadedAt).reversed())
                .toList();
    }

    public ContractFile ownFile(String businessId, String id) {
        return files.findById(id)
                .filter(file -> file.belongsTo(businessId))
                .orElseThrow(() -> new NotFoundException("contract file", id));
    }

    public byte[] ownContent(String businessId, String id) {
        return store.get(ownFile(businessId, id).storageKey());
    }

    public void delete(String businessId, String id, String user) {
        ContractFile file = ownFile(businessId, id);
        store.delete(file.storageKey());
        files.delete(id);
        auditLog.record(user, "contract-file.deleted", id, file.fileName());
    }

    public List<ContractFile> all(String staffUser) {
        auditLog.record(staffUser, "contract-file.list-viewed", "all", "internal team viewed the list of business files");
        return files.findAll().stream().sorted(Comparator.comparing(ContractFile::uploadedAt).reversed()).toList();
    }

    public ContractFile anyFile(String id) {
        return files.findById(id).orElseThrow(() -> new NotFoundException("contract file", id));
    }

    public byte[] contentForStaff(String id, String staffUser) {
        ContractFile file = anyFile(id);
        auditLog.record(staffUser, "contract-file.opened-by-staff", id,
                "%s of business %s".formatted(file.fileName(), file.businessId()));
        return store.get(file.storageKey());
    }

    static String safeName(String fileName) {
        String name = fileName == null ? "contract" : fileName.replaceAll(".*[/\\\\]", "");
        String safe = name.replaceAll("[^A-Za-z0-9._-]", "-").replaceAll("-{2,}", "-");
        if (safe.isBlank() || safe.startsWith(".")) {
            safe = "contract" + safe;
        }
        return safe.length() > 100 ? safe.substring(safe.length() - 100) : safe;
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
