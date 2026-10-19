package com.meridian.contract.application;

import com.meridian.audit.application.AuditLog;
import com.meridian.change.application.RuleChangeService;
import com.meridian.change.domain.ChangeSource;
import com.meridian.common.NotFoundException;
import com.meridian.compliance.domain.ContractContext;
import com.meridian.compliance.domain.Finding;
import com.meridian.compliance.domain.GermanFreightLawCheck;
import com.meridian.compliance.domain.SenderType;
import com.meridian.compliance.domain.Severity;
import com.meridian.compliance.domain.TermsBasis;
import com.meridian.contract.domain.AssistOutcome;
import com.meridian.contract.domain.AssistedReading;
import com.meridian.contract.domain.CarrierContract;
import com.meridian.contract.domain.Clause;
import com.meridian.contract.domain.ClauseCatalog;
import com.meridian.contract.domain.ContractCheck;
import com.meridian.contract.domain.ContractImport;
import com.meridian.contract.domain.ContractReader;
import com.meridian.contract.domain.ContractReading;
import com.meridian.contract.domain.InterpretationGuard;
import com.meridian.contract.domain.RejectedProposal;
import com.meridian.contract.domain.RuleProposal;
import com.meridian.contract.domain.UnresolvedClause;
import com.meridian.routing.application.RoutingService;
import com.meridian.routing.domain.RoutingRule;
import com.meridian.routing.domain.RuleValidator;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ContractImportService {

    private final ContractReader reader;
    private final InterpretationGuard guard;
    private final ClauseInterpreter interpreter;
    private final RuleChangeService changes;
    private final RoutingService routing;
    private final ContractImportRepository imports;
    private final AuditLog auditLog;
    private final Clock clock;
    private final RuleValidator validator = new RuleValidator();
    private final GermanFreightLawCheck lawCheck = new GermanFreightLawCheck();
    private final List<String> services;

    public ContractImportService(ClauseCatalog catalog, RuleChangeService changes, RoutingService routing,
                                 ContractImportRepository imports, AuditLog auditLog, Clock clock,
                                 ClauseInterpreter interpreter) {
        this.reader = new ContractReader(catalog);
        this.guard = new InterpretationGuard(catalog);
        this.interpreter = interpreter;
        this.services = catalog.carrierServices().stream().map(alias -> alias.service().name()).distinct().toList();
        this.changes = changes;
        this.routing = routing;
        this.imports = imports;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    public ContractCheck check(String carrierName, String text, SenderType senderType, TermsBasis termsBasis) {
        return check(carrierName, text, senderType, termsBasis, false);
    }

    public ContractCheck check(String carrierName, String text, SenderType senderType, TermsBasis termsBasis,
                               boolean withModel) {
        ContractContext context = new ContractContext(senderType, termsBasis, LocalDate.now(clock));
        ContractReading reading = reader.read(new CarrierContract("check", carrierName, text));
        List<RuleProposal> proposals = new ArrayList<>(reading.proposals());
        List<UnresolvedClause> unresolved = new ArrayList<>(reading.unresolved());
        List<Clause> unrecognised = new ArrayList<>(reading.unrecognised());
        List<AssistedReading> assisted = new ArrayList<>();
        boolean modelUsed = withModel && interpreter.enabled();
        if (modelUsed) {
            List<Clause> candidates = new ArrayList<>();
            unresolved.stream().map(UnresolvedClause::clause).filter(guard::worthAsking).forEach(candidates::add);
            unrecognised.stream().filter(guard::worthAsking).forEach(candidates::add);
            for (Clause clause : candidates) {
                AssistedReading assistedReading = interpreter.interpret(clause, services)
                        .map(interpretation -> guard.check(clause, interpretation))
                        .orElseGet(() -> AssistedReading.noAnswer(clause, "the model gave no usable answer"));
                assisted.add(assistedReading);
                if (assistedReading.outcome() == AssistOutcome.RULE_ACCEPTED) {
                    proposals.add(assistedReading.proposal());
                    unresolved.removeIf(item -> item.clause().equals(clause));
                    unrecognised.remove(clause);
                }
            }
        }
        List<RuleProposal> accepted = new ArrayList<>();
        List<RejectedProposal> rejected = new ArrayList<>();
        for (RuleProposal proposal : proposals) {
            List<String> problems = validator.problemsIn(proposal.toRule("proposal"));
            if (problems.isEmpty()) {
                accepted.add(proposal);
            } else {
                rejected.add(new RejectedProposal(proposal, problems));
            }
        }
        List<Finding> findings = lawCheck.check(reading.terms(), context);
        return ContractCheck.of(context, accepted, rejected, unresolved, unrecognised, reading.terms(), findings,
                assisted, modelUsed ? "rules and " + interpreter.name() : "rules", noDraftReason(accepted, findings));
    }

    public ContractImport importContract(String user, String carrierName, String text, SenderType senderType,
                                         TermsBasis termsBasis) {
        String contractId = "ctr-" + UUID.randomUUID().toString().substring(0, 8);
        ContractCheck check = check(carrierName, text, senderType, termsBasis, true);
        String changeId = check.canDraft() ? draftChange(user, contractId, check.accepted()) : null;
        ContractImport contractImport = new ContractImport(contractId, carrierName, user, clock.instant(),
                check.context(), check.accepted(), check.rejected(), check.unresolved(), check.unrecognised(),
                check.findings(), check.assisted(), check.reader(), changeId, check.noDraftReason());
        imports.save(contractImport);
        auditLog.record(user, "contract.imported", contractId,
                "%d rules proposed, %d clauses left for a person, %d legal findings"
                        .formatted(check.accepted().size(), check.unresolved().size(), check.findings().size()));
        return contractImport;
    }

    private static String noDraftReason(List<RuleProposal> accepted, List<Finding> findings) {
        if (findings.stream().anyMatch(finding -> finding.severity() == Severity.VIOLATION)) {
            return "the contract has terms that conflict with German freight law; resolve them before routing to this carrier";
        }
        if (accepted.isEmpty()) {
            return "no clause could be turned into a routing rule";
        }
        return null;
    }

    private String draftChange(String user, String contractId, List<RuleProposal> accepted) {
        List<RoutingRule> rules = new ArrayList<>(routing.activeRules().rules());
        for (int i = 0; i < accepted.size(); i++) {
            rules.add(accepted.get(i).toRule("%s-r%d".formatted(contractId, i + 1)));
        }
        return changes.draft(user, rules, ChangeSource.CONTRACT_IMPORT, contractId).id();
    }

    public ContractImport find(String importId) {
        return imports.findById(importId).orElseThrow(() -> new NotFoundException("contract import", importId));
    }

    public List<ContractImport> all() {
        return imports.findAll().stream()
                .sorted(Comparator.comparing(ContractImport::importedAt).reversed())
                .toList();
    }
}
