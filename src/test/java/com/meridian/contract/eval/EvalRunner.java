package com.meridian.contract.eval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.audit.adapter.persistence.InMemoryAuditLog;
import com.meridian.change.adapter.persistence.InMemoryRuleChangeRepository;
import com.meridian.change.application.RuleChangeService;
import com.meridian.contract.adapter.persistence.InMemoryContractImportRepository;
import com.meridian.contract.application.ClauseInterpreter;
import com.meridian.contract.application.ContractImportService;
import com.meridian.contract.domain.ClauseCatalog;
import com.meridian.contract.domain.ContractImport;
import com.meridian.routing.adapter.persistence.InMemoryRuleSetStore;
import com.meridian.routing.application.RoutingService;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

class EvalRunner {

    record Result(EvalCase evalCase, ContractImport contractImport, CaseScore score) {
    }

    private final ObjectMapper mapper = new ObjectMapper();
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-10T09:00:00Z"), ZoneOffset.UTC);
    private final ContractEvalScorer scorer = new ContractEvalScorer();
    private final ClauseCatalog catalog = read("/seed/clause-catalog.json", new TypeReference<>() {
    });

    List<Result> run(String evalSet, ClauseInterpreter interpreter) {
        List<EvalCase> cases = read(evalSet, new TypeReference<>() {
        });
        return cases.stream().map(evalCase -> {
            InMemoryRuleSetStore ruleSets = new InMemoryRuleSetStore(List.of());
            InMemoryAuditLog auditLog = new InMemoryAuditLog(clock);
            RuleChangeService changes = new RuleChangeService(new InMemoryRuleChangeRepository(), ruleSets, List::of,
                    auditLog, clock);
            ContractImportService service = new ContractImportService(catalog, changes, new RoutingService(ruleSets),
                    new InMemoryContractImportRepository(), auditLog, clock, interpreter);
            ContractImport result = service.importContract("eval", evalCase.carrierName(), evalCase.text(),
                    evalCase.senderType(), evalCase.termsBasis());
            return new Result(evalCase, result, scorer.score(evalCase, result));
        }).toList();
    }

    private <T> T read(String resource, TypeReference<T> type) {
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            return mapper.readValue(in, type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
