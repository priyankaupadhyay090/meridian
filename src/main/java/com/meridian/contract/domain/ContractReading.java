package com.meridian.contract.domain;

import com.meridian.compliance.domain.ContractTerms;

import java.util.List;

public record ContractReading(List<RuleProposal> proposals, List<UnresolvedClause> unresolved,
                              List<Clause> unrecognised, ContractTerms terms) {

    public ContractReading {
        proposals = List.copyOf(proposals);
        unresolved = List.copyOf(unresolved);
        unrecognised = List.copyOf(unrecognised);
    }
}
