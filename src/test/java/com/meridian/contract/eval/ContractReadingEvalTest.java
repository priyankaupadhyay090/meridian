package com.meridian.contract.eval;

import com.meridian.contract.application.ClauseInterpreter;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContractReadingEvalTest {

    private final EvalRunner runner = new EvalRunner();
    private final ContractEvalScorer scorer = new ContractEvalScorer();

    @Test
    void readerHandlesEveryLabelledContract() throws Exception {
        List<CaseScore> scores = runner.run("/evals/contract-reading.json", ClauseInterpreter.NONE).stream()
                .map(EvalRunner.Result::score)
                .toList();

        String report = scorer.report(scores);
        Path out = Path.of("build", "reports", "evals", "contract-reading.md");
        Files.createDirectories(out.getParent());
        Files.writeString(out, report);

        assertThat(scores).as(report).allMatch(CaseScore::perfect);
    }

    @Test
    void rulesNeverTurnATrapIntoARuleInTheHarderSet() {
        List<CaseScore> scores = runner.run("/evals/contract-reading-hard.json", ClauseInterpreter.NONE).stream()
                .map(EvalRunner.Result::score)
                .toList();

        assertThat(scores).allMatch(score -> score.trapViolations() == 0 && score.wrong() == 0);
    }
}
