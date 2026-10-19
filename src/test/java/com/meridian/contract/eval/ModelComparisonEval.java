package com.meridian.contract.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.contract.adapter.model.OllamaClauseInterpreter;
import com.meridian.contract.application.ClauseInterpreter;
import com.meridian.contract.domain.AssistOutcome;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("model")
class ModelComparisonEval {

    private final EvalRunner runner = new EvalRunner();

    private static ClauseInterpreter ollama() {
        String url = System.getProperty("ollama.url", "http://localhost:11434");
        String model = System.getProperty("ollama.model", "qwen2.5:7b");
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        requests.setReadTimeout(Duration.ofSeconds(120));
        return new OllamaClauseInterpreter(RestClient.builder().baseUrl(url).requestFactory(requests).build(),
                new ObjectMapper(), model);
    }

    @Test
    void compareRulesAloneWithRulesAndModel() throws Exception {
        ClauseInterpreter model = ollama();
        StringBuilder report = new StringBuilder("# Rules alone compared with rules and a local model\n\n")
                .append("Model: ").append(model.name()).append("\n\n")
                .append("| Set | Reader | Cases fully correct | Rule precision | Rule recall | Traps turned into rules | Model answers accepted | Rejected by the guard | Kept for a person |\n")
                .append("|---|---|---|---|---|---|---|---|---|\n");
        int modelTraps = 0;
        for (String set : List.of("contract-reading", "contract-reading-hard")) {
            for (ClauseInterpreter interpreter : List.of(ClauseInterpreter.NONE, model)) {
                List<EvalRunner.Result> results = runner.run("/evals/" + set + ".json", interpreter);
                report.append(row(set, interpreter, results));
                if (interpreter == model) {
                    modelTraps += results.stream().mapToInt(result -> result.score().trapViolations()).sum();
                }
            }
        }
        report.append("\n## Per case with the model\n\n| Set | Case | Expected rules | Proposed | Correct | Traps hit |\n|---|---|---|---|---|---|\n");
        for (String set : List.of("contract-reading", "contract-reading-hard")) {
            for (EvalRunner.Result result : runner.run("/evals/" + set + ".json", model)) {
                CaseScore score = result.score();
                report.append("| %s | %s | %d | %d | %d | %d |\n".formatted(set, score.caseId(), score.expected(),
                        score.accepted(), score.correct(), score.trapViolations()));
            }
        }
        report.append("\n## Model answers\n\n| Set | Clause | Outcome | Reason |\n|---|---|---|---|\n");
        for (String set : List.of("contract-reading", "contract-reading-hard")) {
            for (EvalRunner.Result result : runner.run("/evals/" + set + ".json", model)) {
                result.contractImport().assisted().forEach(reading -> report.append("| %s | %s | %s | %s |\n".formatted(
                        set, reading.clause().text().replace("|", "/"), reading.outcome(),
                        reading.reason().replace("|", "/"))));
            }
        }
        Path out = Path.of("build", "reports", "evals", "model-comparison.md");
        Files.createDirectories(out.getParent());
        Files.writeString(out, report);
        System.out.println(report);

        assertThat(modelTraps).as("trap clauses turned into rules with the model").isZero();
    }

    private static String row(String set, ClauseInterpreter interpreter, List<EvalRunner.Result> results) {
        int expected = results.stream().mapToInt(result -> result.score().expected()).sum();
        int accepted = results.stream().mapToInt(result -> result.score().accepted()).sum();
        int correct = results.stream().mapToInt(result -> result.score().correct()).sum();
        int traps = results.stream().mapToInt(result -> result.score().trapViolations()).sum();
        long perfect = results.stream().filter(result -> result.score().perfect()).count();
        long modelAccepted = count(results, AssistOutcome.RULE_ACCEPTED);
        long guarded = count(results, AssistOutcome.REJECTED);
        long kept = count(results, AssistOutcome.KEPT_FOR_PERSON) + count(results, AssistOutcome.NOT_A_RULE)
                + count(results, AssistOutcome.NO_ANSWER);
        return "| %s | %s | %d of %d | %s | %s | %d | %d | %d | %d |\n".formatted(set,
                interpreter.enabled() ? "rules + model" : "rules", perfect, results.size(), ratio(correct, accepted),
                ratio(correct, expected), traps, modelAccepted, guarded, kept);
    }

    private static long count(List<EvalRunner.Result> results, AssistOutcome outcome) {
        return results.stream().flatMap(result -> result.contractImport().assisted().stream())
                .filter(reading -> reading.outcome() == outcome)
                .count();
    }

    private static String ratio(int part, int whole) {
        return whole == 0 ? "n/a" : String.format(Locale.ROOT, "%.2f (%d/%d)", (double) part / whole, part, whole);
    }
}
