package com.meridian.contract.adapter.web;

import com.meridian.compliance.domain.SenderType;
import com.meridian.compliance.domain.TermsBasis;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContractImportRequest(@NotBlank @Size(max = 120) String carrierName,
                                    @NotBlank @Size(max = 100_000) String text,
                                    SenderType senderType,
                                    TermsBasis termsBasis) {

    SenderType senderTypeOrDefault() {
        return senderType == null ? SenderType.BUSINESS : senderType;
    }

    TermsBasis termsBasisOrDefault() {
        return termsBasis == null ? TermsBasis.STANDARD_TERMS : termsBasis;
    }
}
