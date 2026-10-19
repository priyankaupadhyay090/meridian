package com.meridian.contract.domain;

import com.meridian.routing.domain.CarrierService;
import com.meridian.routing.domain.CustomerTier;
import com.meridian.routing.domain.ServiceLevel;

public record ClauseInterpretation(boolean routingRule, boolean conditional, String destinationPrefix,
                                   ServiceLevel serviceLevel, Double minWeightKg, Double maxWeightKg,
                                   CustomerTier customerTier, CarrierService carrierService, String quote,
                                   String reason) {
}
