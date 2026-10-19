package com.meridian.contract.adapter.web;

import com.meridian.compliance.domain.SenderType;
import com.meridian.compliance.domain.TermsBasis;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContractCheckRequest(@Size(max = 120) String carrierName,
                                   @NotBlank @Size(max = 100_000) String text,
                                   @NotNull SenderType senderType,
                                   @NotNull TermsBasis termsBasis) {
}
