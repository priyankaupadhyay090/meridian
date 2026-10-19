package com.meridian.contract.domain;

import com.meridian.routing.domain.CarrierService;
import com.meridian.routing.domain.CustomerTier;
import com.meridian.routing.domain.RoutingRule;
import com.meridian.routing.domain.RuleMatch;
import com.meridian.routing.domain.ServiceLevel;
import com.meridian.routing.domain.WeightBand;

public record RuleProposal(String destinationPrefix, ServiceLevel serviceLevel, WeightBand weightBand,
                           CustomerTier customerTier, int priority, CarrierService carrierService,
                           String sourceQuote) {

    public RoutingRule toRule(String id) {
        return new RoutingRule(id, new RuleMatch(destinationPrefix, serviceLevel, weightBand, customerTier),
                priority, carrierService);
    }
}
