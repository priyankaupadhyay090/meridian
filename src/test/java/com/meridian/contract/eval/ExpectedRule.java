package com.meridian.contract.eval;

import com.meridian.contract.domain.RuleProposal;
import com.meridian.routing.domain.CarrierService;
import com.meridian.routing.domain.CustomerTier;
import com.meridian.routing.domain.ServiceLevel;
import com.meridian.routing.domain.WeightBand;

import java.util.Objects;

public record ExpectedRule(String destinationPrefix, ServiceLevel serviceLevel, CustomerTier customerTier,
                           Integer minWeightGrams, Integer maxWeightGrams, CarrierService carrierService) {

    public boolean matches(RuleProposal proposal) {
        return destinationPrefix.equals(proposal.destinationPrefix())
                && carrierService == proposal.carrierService()
                && serviceLevel == proposal.serviceLevel()
                && customerTier == proposal.customerTier()
                && Objects.equals(weightBand(), proposal.weightBand());
    }

    private WeightBand weightBand() {
        return minWeightGrams == null ? null : new WeightBand(minWeightGrams, maxWeightGrams);
    }
}
