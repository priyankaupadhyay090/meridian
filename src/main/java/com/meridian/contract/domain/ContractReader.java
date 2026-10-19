package com.meridian.contract.domain;

import java.util.ArrayList;
import java.util.List;

public class ContractReader {

    private final ClauseSplitter splitter = new ClauseSplitter();
    private final RoutingClauseReader routingReader;
    private final TermReader termReader = new TermReader();

    public ContractReader(ClauseCatalog catalog) {
        this.routingReader = new RoutingClauseReader(catalog);
    }

    public ContractReading read(CarrierContract contract) {
        List<Clause> clauses = splitter.split(contract.text());
        List<RuleProposal> proposals = new ArrayList<>();
        List<UnresolvedClause> unresolved = new ArrayList<>();
        List<Clause> unrecognised = new ArrayList<>();
        for (Clause clause : clauses) {
            if (routingReader.isRoutingClause(clause)) {
                ClauseOutcome outcome = routingReader.read(clause);
                if (outcome.proposal() != null) {
                    proposals.add(outcome.proposal());
                } else {
                    unresolved.add(outcome.unresolved());
                }
            } else if (routingReader.instruction(clause).isPresent()) {
                unresolved.add(routingReader.instruction(clause).get());
            } else if (!termReader.isTermClause(clause)) {
                unrecognised.add(clause);
            }
        }
        return new ContractReading(proposals, unresolved, unrecognised, termReader.read(clauses));
    }
}
