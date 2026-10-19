package com.meridian.compliance.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GermanFreightLawCheckTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 10);
    private static final ContractContext BUSINESS_STANDARD =
            new ContractContext(SenderType.BUSINESS, TermsBasis.STANDARD_TERMS, TODAY);
    private static final ContractContext BUSINESS_NEGOTIATED =
            new ContractContext(SenderType.BUSINESS, TermsBasis.INDIVIDUALLY_NEGOTIATED, TODAY);
    private static final ContractContext CONSUMER =
            new ContractContext(SenderType.CONSUMER, TermsBasis.INDIVIDUALLY_NEGOTIATED, TODAY);

    private final GermanFreightLawCheck check = new GermanFreightLawCheck();

    private static ContractTerms liability(double sdr) {
        return new ContractTerms(new ContractTerms.LiabilityCap(sdr, "cap clause"), null, null, null, null, 0, null, null);
    }

    private List<String> ruleIds(ContractTerms terms, ContractContext context) {
        return check.check(terms, context).stream().map(Finding::ruleId).toList();
    }

    @Test
    void capBelowTheCorridorInStandardTermsIsAViolation() {
        List<Finding> findings = check.check(liability(1.5), BUSINESS_STANDARD);

        assertThat(findings).singleElement().satisfies(finding -> {
            assertThat(finding.ruleId()).isEqualTo("HGB-449-2-LIABILITY");
            assertThat(finding.severity()).isEqualTo(Severity.VIOLATION);
            assertThat(finding.lawReference()).isEqualTo("§ 449 Abs. 2 HGB");
            assertThat(finding.source()).startsWith("https://www.gesetze-im-internet.de/hgb/");
        });
    }

    @Test
    void capInsideTheCorridorNeedsClearNotice() {
        assertThat(check.check(liability(4), BUSINESS_STANDARD))
                .singleElement().extracting(Finding::severity).isEqualTo(Severity.CHECK);
    }

    @Test
    void statutoryOrHigherCapRaisesNothing() {
        assertThat(check.check(liability(8.33), BUSINESS_STANDARD)).isEmpty();
        assertThat(check.check(liability(40), BUSINESS_STANDARD)).isEmpty();
    }

    @Test
    void consumerCanNeverBeGivenALowerCap() {
        assertThat(ruleIds(liability(5), CONSUMER)).containsExactly("HGB-449-3-LIABILITY");
    }

    @Test
    void negotiatedCapIsOnlyNoted() {
        assertThat(check.check(liability(1.5), BUSINESS_NEGOTIATED))
                .singleElement().extracting(Finding::severity).isEqualTo(Severity.INFO);
    }

    @Test
    void shortNoticePeriodsAndLimitationInStandardTermsAreViolations() {
        ContractTerms terms = new ContractTerms(null, new ContractTerms.NoticePeriod(48, "hidden"),
                new ContractTerms.NoticePeriod(7 * 24, "delay"), new ContractTerms.LimitationPeriod(6, "limit"),
                "no liability for delay", 0, null, null);

        assertThat(ruleIds(terms, BUSINESS_STANDARD))
                .containsExactlyInAnyOrder("HGB-438-2-HIDDEN", "HGB-438-3-DELAY", "HGB-439-4-LIMITATION", "HGB-431-3-DELAY");
        assertThat(check.check(terms, BUSINESS_STANDARD)).allMatch(f -> f.severity() == Severity.VIOLATION);
    }

    @Test
    void heavyParcelsWithoutLabellingNeedACheckAndTwoPersonDeliveryIsNoted() {
        ContractTerms terms = new ContractTerms(null, null, null, null, null, 31_500, null, null);

        assertThat(ruleIds(terms, BUSINESS_STANDARD)).containsExactly("POSTG-73-1-LABEL", "POSTG-73-2-TWO-PERSON");
    }

    @Test
    void labellingDutyDoesNotApplyBefore2025() {
        ContractTerms terms = new ContractTerms(null, null, null, null, null, 15_000, null, null);
        ContractContext before = new ContractContext(SenderType.BUSINESS, TermsBasis.STANDARD_TERMS, LocalDate.of(2024, 12, 31));

        assertThat(check.check(terms, before)).isEmpty();
    }

    @Test
    void delayExclusionWithAnExceptionNeedsAPersonInsteadOfBlocking() {
        ContractTerms terms = new ContractTerms(null, null, null, null,
                "Eine Haftung für die Überschreitung der Lieferfrist ist ausgeschlossen, es sei denn, die Lieferfrist wurde bestätigt.",
                0, null, null);

        assertThat(check.check(terms, BUSINESS_STANDARD)).singleElement().satisfies(finding -> {
            assertThat(finding.ruleId()).isEqualTo("HGB-423-431-DELAY-EXCEPTION");
            assertThat(finding.severity()).isEqualTo(Severity.CHECK);
            assertThat(finding.source()).endsWith("__423.html");
        });
    }
}
