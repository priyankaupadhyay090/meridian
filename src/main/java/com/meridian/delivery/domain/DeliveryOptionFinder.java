package com.meridian.delivery.domain;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class DeliveryOptionFinder {

    public static final String SHOP_PRICE_SOURCE = "price set by the shop";

    public List<DeliveryOption> options(Parcel parcel, Courier shopDefault, long shopShippingCents,
                                        Set<Courier> shopCouriers, List<CourierRate> rates) {
        List<DeliveryOption> options = new ArrayList<>();
        for (Courier courier : shopCouriers) {
            Optional<CourierRate> rate = rates.stream()
                    .filter(r -> r.courier() == courier && r.fits(parcel))
                    .min(Comparator.comparingLong(CourierRate::priceCents));
            if (rate.isEmpty()) {
                continue;
            }
            CourierRate r = rate.get();
            boolean isDefault = courier == shopDefault;
            options.add(new DeliveryOption(courier, r.service(), isDefault ? shopShippingCents : r.priceCents(),
                    r.deliveryDaysMin(), r.deliveryDaysMax(), r.deliveryTimeBasis(),
                    isDefault ? SHOP_PRICE_SOURCE : r.source(), isDefault ? null : r.sourceKind(),
                    isDefault ? Set.of(OptionLabel.SHOP_DEFAULT) : Set.of()));
        }
        return label(options);
    }

    private static List<DeliveryOption> label(List<DeliveryOption> options) {
        if (options.isEmpty()) {
            return options;
        }
        long cheapest = options.stream().mapToLong(DeliveryOption::priceCents).min().orElseThrow();
        int fastest = options.stream().mapToInt(DeliveryOption::deliveryDaysMax).min().orElseThrow();
        return options.stream()
                .map(option -> {
                    Set<OptionLabel> labels = option.labels().isEmpty() ? EnumSet.noneOf(OptionLabel.class)
                            : EnumSet.copyOf(option.labels());
                    if (option.priceCents() == cheapest) {
                        labels.add(OptionLabel.CHEAPEST);
                    }
                    if (option.deliveryDaysMax() == fastest) {
                        labels.add(OptionLabel.FASTEST);
                    }
                    return option.withLabels(labels);
                })
                .sorted(Comparator.comparingLong(DeliveryOption::priceCents)
                        .thenComparingInt(DeliveryOption::deliveryDaysMax)
                        .thenComparing(option -> option.courier().name()))
                .toList();
    }
}
