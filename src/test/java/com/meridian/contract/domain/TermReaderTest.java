package com.meridian.contract.domain;

import com.meridian.compliance.domain.ContractTerms;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TermReaderTest {

    private final TermReader reader = new TermReader();

    private static List<Clause> clauses(String... texts) {
        return java.util.Arrays.stream(texts).map(text -> new Clause("x", text)).toList();
    }

    @Test
    void readsLiabilityCapInEnglishAndGerman() {
        assertThat(reader.read(clauses("Liability is limited to 1.5 SDR per kilogram.")).liabilityCap().sdrPerKilogram())
                .isEqualTo(1.5);
        assertThat(reader.read(clauses("Die Haftung ist auf 2,5 Sonderziehungsrechte je Kilogramm begrenzt."))
                .liabilityCap().sdrPerKilogram()).isEqualTo(2.5);
    }

    @Test
    void readsNoticePeriodsAndLimitation() {
        ContractTerms terms = reader.read(clauses(
                "Hidden damage must be reported within 48 hours of delivery.",
                "Late delivery must be notified within 7 days.",
                "All claims expire after 6 months."));

        assertThat(terms.hiddenDamageNotice().hours()).isEqualTo(48);
        assertThat(terms.delayNotice().hours()).isEqualTo(7 * 24);
        assertThat(terms.limitation().months()).isEqualTo(6);
    }

    @Test
    void readsHeaviestWeightAndHeavyParcelCommitments() {
        ContractTerms terms = reader.read(clauses(
                "Parcels up to 31.5 kg are accepted.",
                "Parcels over 10 kg are labelled with their weight class.",
                "Parcels over 20 kg are delivered by two persons."));

        assertThat(terms.heaviestCoveredGrams()).isEqualTo(31_500);
        assertThat(terms.heavyParcelLabelling()).isNotNull();
        assertThat(terms.twoPersonDelivery()).isNotNull();
    }

    @Test
    void anExclusionAndADelayInDifferentSentencesAreNotADelayExclusion() {
        ContractTerms terms = reader.read(clauses(
                "Der Ersatz weitergehender Schäden ist ausgeschlossen. Bei Überschreitung der Lieferfrist ist die Haftung auf den dreifachen Betrag der Fracht begrenzt."));

        assertThat(terms.delayLiabilityExclusion()).isNull();
    }

    @Test
    void anExclusionOfDelayInOneSentenceIsStillFound() {
        assertThat(reader.read(clauses("Für die Überschreitung der Lieferfrist ist die Haftung ausgeschlossen."))
                .delayLiabilityExclusion()).isNotNull();
    }

    @Test
    void aReferenceToTheStatutoryLimitsReadsAsTheStatutoryCap() {
        assertThat(reader.read(clauses("Der Paketdienst haftet bis zu den gesetzlichen Haftungsgrenzen."))
                .liabilityCap().sdrPerKilogram()).isEqualTo(8.33);
        assertThat(reader.read(clauses("Liability follows the statutory limits.", "Liability is limited to 2 SDR per kg."))
                .liabilityCap().sdrPerKilogram()).isEqualTo(2);
    }

    @Test
    void abbreviationsDoNotEndTheQuotedSentence() {
        assertThat(reader.read(clauses(
                "Eine Haftung für die Überschreitung der Lieferfrist ist ausgeschlossen, es sei denn, sie wurde in Textform (bspw. E-Mail) bestätigt. Weitere Sätze folgen."))
                .delayLiabilityExclusion())
                .isEqualTo("Eine Haftung für die Überschreitung der Lieferfrist ist ausgeschlossen, es sei denn, sie wurde in Textform (bspw. E-Mail) bestätigt.");
    }
}
