package com.meridian.compliance.domain;

import java.time.LocalDate;

public record ContractContext(SenderType senderType, TermsBasis termsBasis, LocalDate checkedOn) {
}
