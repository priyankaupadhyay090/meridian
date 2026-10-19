package com.meridian.routing.domain;

import java.util.Objects;

public record RuleMatch(String destinationPrefix, ServiceLevel serviceLevel, WeightBand weightBand,
                        CustomerTier customerTier) {

    public boolean matches(ShipmentProfile profile) {
        return profile.destinationPostcode().startsWith(destinationPrefix)
                && (serviceLevel == null || serviceLevel == profile.serviceLevel())
                && (weightBand == null || weightBand.contains(profile.weightGrams()))
                && (customerTier == null || customerTier == profile.customerTier());
    }

    public boolean overlaps(RuleMatch other) {
        return destinationPrefix.equals(other.destinationPrefix)
                && sameOrOpen(serviceLevel, other.serviceLevel)
                && sameOrOpen(customerTier, other.customerTier)
                && (weightBand == null || other.weightBand == null || weightBand.overlaps(other.weightBand));
    }

    public int specificity() {
        return destinationPrefix.length();
    }

    private static <T> boolean sameOrOpen(T left, T right) {
        return left == null || right == null || Objects.equals(left, right);
    }
}
