package com.meridian.contract.domain;

import com.meridian.compliance.domain.ContractContext;
import com.meridian.compliance.domain.ContractTerms;
import com.meridian.compliance.domain.Finding;

import java.util.List;

public record ContractCheck(ContractContext context, List<RuleProposal> accepted, List<RejectedProposal> rejected,
                            List<UnresolvedClause> unresolved, List<Clause> unrecognised, ContractTerms terms,
                            List<Finding> findings, List<AssistedReading> assisted, String reader, boolean canDraft,
                            String noDraftReason) {

    public static ContractCheck of(ContractContext context, List<RuleProposal> accepted,
                                   List<RejectedProposal> rejected, List<UnresolvedClause> unresolved,
                                   List<Clause> unrecognised, ContractTerms terms, List<Finding> findings,
                                   List<AssistedReading> assisted, String reader, String noDraftReason) {
        return new ContractCheck(context, List.copyOf(accepted), List.copyOf(rejected), List.copyOf(unresolved),
                List.copyOf(unrecognised), terms, List.copyOf(findings), List.copyOf(assisted), reader,
                noDraftReason == null, noDraftReason);
    }
}
