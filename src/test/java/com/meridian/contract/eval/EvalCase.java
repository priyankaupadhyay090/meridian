package com.meridian.contract.eval;

import com.meridian.compliance.domain.SenderType;
import com.meridian.compliance.domain.TermsBasis;

import java.util.List;

public record EvalCase(String id, String carrierName, String text, SenderType senderType, TermsBasis termsBasis,
                       List<ExpectedRule> expected, List<String> traps, List<String> expectedFindings) {
}
