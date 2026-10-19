package com.meridian.contract.domain;

import java.util.List;

public record RejectedProposal(RuleProposal proposal, List<String> reasons) {
}
