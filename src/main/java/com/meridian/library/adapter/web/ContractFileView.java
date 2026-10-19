package com.meridian.library.adapter.web;

import com.meridian.library.domain.ContractFile;

import java.time.Instant;

public record ContractFileView(String id, String businessId, String fileName, String format, long sizeBytes,
                               String carrierName, String uploadedBy, Instant uploadedAt) {

    static ContractFileView of(ContractFile file) {
        return new ContractFileView(file.id(), file.businessId(), file.fileName(), file.format(), file.sizeBytes(),
                file.carrierName(), file.uploadedBy(), file.uploadedAt());
    }
}
