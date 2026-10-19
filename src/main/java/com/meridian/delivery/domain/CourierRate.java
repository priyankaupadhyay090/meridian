package com.meridian.delivery.domain;


import java.time.LocalDate;
import java.util.stream.IntStream;

public record CourierRate(Courier courier, String service, int maxWeightGrams, SizeRule sizeRule, Integer maxLengthCm,
                          Integer maxWidthCm, Integer maxHeightCm, Integer maxLongestPlusShortestCm, long priceCents,
                          int deliveryDaysMin, int deliveryDaysMax, DeliveryTimeBasis deliveryTimeBasis,
                          String source, SourceKind sourceKind, LocalDate checkedOn, String note) {

    public boolean fits(Parcel parcel) {
        if (parcel.weightGrams() > maxWeightGrams) {
            return false;
        }
        return switch (sizeRule) {
            case LONGEST_PLUS_SHORTEST -> parcel.longestPlusShortest() <= maxLongestPlusShortestCm;
            case MAX_DIMENSIONS -> {
                int[] parcelSides = parcel.sortedSides();
                int[] limit = IntStream.of(maxLengthCm, maxWidthCm, maxHeightCm).sorted().toArray();
                yield parcelSides[0] <= limit[0] && parcelSides[1] <= limit[1] && parcelSides[2] <= limit[2];
            }
        };
    }
}
