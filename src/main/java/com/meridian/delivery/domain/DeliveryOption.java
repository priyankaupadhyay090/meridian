package com.meridian.delivery.domain;

import java.util.Set;

public record DeliveryOption(Courier courier, String service, long priceCents, int deliveryDaysMin,
                             int deliveryDaysMax, DeliveryTimeBasis deliveryTimeBasis, String priceSource,
                             SourceKind priceSourceKind, Set<OptionLabel> labels) {

    DeliveryOption withLabels(Set<OptionLabel> newLabels) {
        return new DeliveryOption(courier, service, priceCents, deliveryDaysMin, deliveryDaysMax, deliveryTimeBasis,
                priceSource, priceSourceKind, Set.copyOf(newLabels));
    }
}
