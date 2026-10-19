package com.meridian.contract.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClauseSplitterTest {

    private final ClauseSplitter splitter = new ClauseSplitter();

    @Test
    void numberedClausesKeepTheirReference() {
        List<Clause> clauses = splitter.split("""
                4.1 Express parcels go with DPD.
                4.2 Economy parcels go with DHL.
                """);

        assertThat(clauses).extracting(Clause::reference).containsExactly("4.1", "4.2");
        assertThat(clauses.getFirst().text()).isEqualTo("Express parcels go with DPD.");
    }

    @Test
    void wrappedLinesBelongToTheClauseAbove() {
        List<Clause> clauses = splitter.split("""
                7. Express parcels to postcodes beginning with 10 are delivered
                by GLS Express.
                """);

        assertThat(clauses).singleElement()
                .extracting(Clause::text)
                .isEqualTo("Express parcels to postcodes beginning with 10 are delivered by GLS Express.");
    }

    @Test
    void germanParagraphReferencesAreRecognised() {
        List<Clause> clauses = splitter.split("§ 3 Abs. 1: Standardsendungen befördert DHL Paket.");

        assertThat(clauses).singleElement().extracting(Clause::reference).isEqualTo("§ 3 Abs. 1");
    }

    @Test
    void bracketedParagraphNumbersStartClauses() {
        List<Clause> clauses = splitter.split("""
                (1) Der Paketdienst haftet für Vorsatz
                (§ 428 HGB) ohne Grenze.
                (2) Im Übrigen gelten die gesetzlichen Haftungsgrenzen.
                """);

        assertThat(clauses).extracting(Clause::reference).containsExactly("(1)", "(2)");
        assertThat(clauses.getFirst().text()).isEqualTo("Der Paketdienst haftet für Vorsatz (§ 428 HGB) ohne Grenze.");
    }
}
