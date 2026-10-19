package com.meridian.compliance.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class GermanFreightLawCheck {

    public static final double STATUTORY_SDR_PER_KG = 8.33;
    public static final double CORRIDOR_LOW_SDR = 2;
    public static final double CORRIDOR_HIGH_SDR = 40;
    public static final int HIDDEN_DAMAGE_NOTICE_HOURS = 7 * 24;
    public static final int DELAY_NOTICE_HOURS = 21 * 24;
    public static final int LIMITATION_MONTHS = 12;
    public static final int LABELLING_THRESHOLD_GRAMS = 10_000;
    public static final int TWO_PERSON_THRESHOLD_GRAMS = 20_000;
    public static final LocalDate LABELLING_APPLIES_FROM = LocalDate.of(2025, 1, 1);

    private static final Pattern EXCEPTION = Pattern.compile(
            "es sei denn|sofern nicht|außer wenn|ausgenommen|unless|except where|except if");

    private static final String HGB = "https://www.gesetze-im-internet.de/hgb/";
    private static final String POSTG = "https://www.gesetze-im-internet.de/postg_2024/";

    public List<Finding> check(ContractTerms terms, ContractContext context) {
        List<Finding> findings = new ArrayList<>();
        liabilityCap(terms, context, findings);
        noticePeriods(terms, context, findings);
        limitation(terms, context, findings);
        delayLiability(terms, context, findings);
        heavyParcels(terms, context, findings);
        return findings;
    }

    private void liabilityCap(ContractTerms terms, ContractContext context, List<Finding> findings) {
        ContractTerms.LiabilityCap cap = terms.liabilityCap();
        if (cap == null || cap.sdrPerKilogram() >= STATUTORY_SDR_PER_KG) {
            return;
        }
        String amount = "%s SDR per kg".formatted(
                BigDecimal.valueOf(cap.sdrPerKilogram()).stripTrailingZeros().toPlainString());
        if (context.senderType() == SenderType.CONSUMER) {
            findings.add(new Finding("HGB-449-3-LIABILITY", Severity.VIOLATION, "§ 449 Abs. 3 HGB",
                    "%s is below the statutory 8.33 SDR per kg; with a consumer as sender the liability rules cannot be changed to their disadvantage."
                            .formatted(amount), cap.clause(), HGB + "__449.html"));
        } else if (context.termsBasis() == TermsBasis.INDIVIDUALLY_NEGOTIATED) {
            findings.add(new Finding("HGB-449-1-LIABILITY", Severity.INFO, "§ 449 Abs. 1 HGB",
                    "%s differs from § 431 HGB; this is possible because the terms are recorded as individually negotiated."
                            .formatted(amount), cap.clause(), HGB + "__449.html"));
        } else if (cap.sdrPerKilogram() < CORRIDOR_LOW_SDR) {
            findings.add(new Finding("HGB-449-2-LIABILITY", Severity.VIOLATION, "§ 449 Abs. 2 HGB",
                    "%s is below the corridor of 2 to 40 SDR per kg that standard terms may use instead of § 431 HGB."
                            .formatted(amount), cap.clause(), HGB + "__449.html"));
        } else {
            findings.add(new Finding("HGB-449-2-NOTICE", Severity.CHECK, "§ 449 Abs. 2 Nr. 1 HGB",
                    "%s is inside the 2 to 40 SDR corridor; in standard terms it only holds if the other party was clearly informed of it."
                            .formatted(amount), cap.clause(), HGB + "__449.html"));
        }
    }

    private void noticePeriods(ContractTerms terms, ContractContext context, List<Finding> findings) {
        shorterThanLaw(terms.hiddenDamageNotice(), HIDDEN_DAMAGE_NOTICE_HOURS, "HGB-438-2-HIDDEN", "§ 438 Abs. 2 HGB",
                "hidden loss or damage", "7 days", context, findings);
        shorterThanLaw(terms.delayNotice(), DELAY_NOTICE_HOURS, "HGB-438-3-DELAY", "§ 438 Abs. 3 HGB",
                "late delivery", "21 days", context, findings);
    }

    private void shorterThanLaw(ContractTerms.NoticePeriod notice, int statutoryHours, String ruleId, String reference,
                                String subject, String statutory, ContractContext context, List<Finding> findings) {
        if (notice == null || notice.hours() >= statutoryHours) {
            return;
        }
        if (isProtected(context)) {
            findings.add(new Finding(ruleId, Severity.VIOLATION, reference + " i.V.m. § 449 Abs. 1 HGB",
                    "The contract asks for notice of %s within %d hours; the law allows %s, and standard terms or consumer contracts cannot shorten it."
                            .formatted(subject, notice.hours(), statutory), notice.clause(), HGB + "__438.html"));
        } else {
            findings.add(new Finding(ruleId, Severity.INFO, reference,
                    "The notice period for %s is shorter than the statutory %s; possible because the terms are individually negotiated."
                            .formatted(subject, statutory), notice.clause(), HGB + "__438.html"));
        }
    }

    private void limitation(ContractTerms terms, ContractContext context, List<Finding> findings) {
        ContractTerms.LimitationPeriod limitation = terms.limitation();
        if (limitation == null || limitation.months() >= LIMITATION_MONTHS) {
            return;
        }
        Severity severity = context.termsBasis() == TermsBasis.INDIVIDUALLY_NEGOTIATED ? Severity.INFO : Severity.VIOLATION;
        findings.add(new Finding("HGB-439-4-LIMITATION", severity, "§ 439 Abs. 4 HGB",
                "Claims would expire after %d months instead of one year; the limitation period can only be shortened by an individually negotiated agreement."
                        .formatted(limitation.months()), limitation.clause(), HGB + "__439.html"));
    }

    private void delayLiability(ContractTerms terms, ContractContext context, List<Finding> findings) {
        if (terms.delayLiabilityExclusion() == null || !isProtected(context)) {
            return;
        }
        if (EXCEPTION.matcher(terms.delayLiabilityExclusion().toLowerCase()).find()) {
            findings.add(new Finding("HGB-423-431-DELAY-EXCEPTION", Severity.CHECK,
                    "§§ 423, 431 Abs. 3 i.V.m. § 449 Abs. 1 HGB",
                    "The contract excludes liability for late delivery but makes an exception. Under § 423 HGB a delivery time also applies without an agreement: the time a careful carrier would reasonably need. In standard terms the delay liability can only be changed by an individually negotiated agreement, so a person should check whether the exception covers that case.",
                    terms.delayLiabilityExclusion(), HGB + "__423.html"));
            return;
        }
        findings.add(new Finding("HGB-431-3-DELAY", Severity.VIOLATION, "§ 431 Abs. 3 i.V.m. § 449 Abs. 1 HGB",
                "The contract excludes liability for late delivery; the law caps it at three times the freight, and that cannot be excluded in standard terms or towards consumers.",
                terms.delayLiabilityExclusion(), HGB + "__431.html"));
    }

    private void heavyParcels(ContractTerms terms, ContractContext context, List<Finding> findings) {
        if (terms.heaviestCoveredGrams() > LABELLING_THRESHOLD_GRAMS && terms.heavyParcelLabelling() == null
                && !context.checkedOn().isBefore(LABELLING_APPLIES_FROM)) {
            findings.add(new Finding("POSTG-73-1-LABEL", Severity.CHECK, "§ 73 Abs. 1 PostG",
                    "The contract covers parcels over 10 kg but says nothing about labelling them; since 1 January 2025 parcels over 10 kg and over 20 kg must carry a clear weight notice before they reach delivery.",
                    null, POSTG + "__73.html"));
        }
        if (terms.heaviestCoveredGrams() > TWO_PERSON_THRESHOLD_GRAMS && terms.twoPersonDelivery() == null) {
            findings.add(new Finding("POSTG-73-2-TWO-PERSON", Severity.INFO, "§ 73 Abs. 2 PostG",
                    "The contract covers parcels over 20 kg; these must be delivered by two people or with a suitable technical aid, from six months after the ordinance under § 73 Abs. 2 Satz 2 PostG takes effect (§ 112 Abs. 10 PostG).",
                    null, POSTG + "__73.html"));
        }
    }

    private static boolean isProtected(ContractContext context) {
        return context.senderType() == SenderType.CONSUMER || context.termsBasis() == TermsBasis.STANDARD_TERMS;
    }
}
