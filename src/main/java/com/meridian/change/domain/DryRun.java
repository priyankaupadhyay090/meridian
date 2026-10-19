package com.meridian.change.domain;

import com.meridian.routing.domain.CarrierService;
import com.meridian.routing.domain.RoutingDecision;
import com.meridian.routing.domain.RoutingRule;
import com.meridian.routing.domain.RuleSet;
import com.meridian.routing.domain.RuleValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DryRun {

    private static final int MAX_EXAMPLES = 10;

    private final RuleValidator validator = new RuleValidator();

    public DryRunReport compare(RuleSet current, List<RoutingRule> proposed, List<SampleShipment> shipments) {
        RuleSet candidate = new RuleSet(current.version() + 1, proposed);
        List<RouteDifference> differences = new ArrayList<>();
        int unrouted = 0;
        for (SampleShipment shipment : shipments) {
            CarrierService before = current.resolve(shipment.profile()).map(RoutingDecision::carrierService).orElse(null);
            CarrierService after = candidate.resolve(shipment.profile()).map(RoutingDecision::carrierService).orElse(null);
            if (after == null) {
                unrouted++;
            }
            if (!Objects.equals(before, after)) {
                differences.add(new RouteDifference(shipment.id(), before, after));
            }
        }
        return new DryRunReport(current.version(), shipments.size(), differences.size(), unrouted,
                differences.stream().limit(MAX_EXAMPLES).toList(), validator.conflictsIn(proposed));
    }
}
