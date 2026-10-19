package com.meridian.library.domain;

import java.time.Instant;

public record ContractFile(String id, String businessId, String fileName, String format, String contentType,
                           long sizeBytes, String sha256, String carrierName, String storageKey, String uploadedBy,
                           Instant uploadedAt) {

    public boolean belongsTo(String business) {
        return businessId.equals(business);
    }
}
