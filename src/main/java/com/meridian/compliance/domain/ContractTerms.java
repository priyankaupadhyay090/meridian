package com.meridian.compliance.domain;

public record ContractTerms(LiabilityCap liabilityCap, NoticePeriod hiddenDamageNotice, NoticePeriod delayNotice,
                            LimitationPeriod limitation, String delayLiabilityExclusion, int heaviestCoveredGrams,
                            String heavyParcelLabelling, String twoPersonDelivery) {

    public record LiabilityCap(double sdrPerKilogram, String clause) {
    }

    public record NoticePeriod(int hours, String clause) {
    }

    public record LimitationPeriod(int months, String clause) {
    }
}
