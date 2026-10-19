package com.meridian.contract.domain;

import com.meridian.compliance.domain.SenderType;
import com.meridian.compliance.domain.TermsBasis;

public record ContractExample(String id, String title, String carrierName, SenderType senderType,
                              TermsBasis termsBasis, String text) {
}
