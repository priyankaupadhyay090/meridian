package com.meridian.contract.domain;

import com.meridian.compliance.domain.ContractContext;
import com.meridian.compliance.domain.Finding;

import java.time.Instant;
import java.util.List;

public record ContractImport(String id, String carrierName, String importedBy, Instant importedAt,
                             ContractContext context, List<RuleProposal> accepted, List<RejectedProposal> rejected,
                             List<UnresolvedClause> unresolved, List<Clause> unrecognised, List<Finding> findings,
                             List<AssistedReading> assisted, String reader, String changeId, String noDraftReason) {
}
