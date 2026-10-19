package com.meridian.parcel.application;

import com.meridian.audit.application.AuditLog;
import com.meridian.common.NotFoundException;
import com.meridian.parcel.domain.EventOutcome;
import com.meridian.parcel.domain.EventResult;
import com.meridian.parcel.domain.Parcel;
import com.meridian.parcel.domain.ParcelEvent;
import com.meridian.parcel.domain.ParcelState;
import com.meridian.parcel.domain.ParcelStateMachine;
import com.meridian.parcel.domain.RemediationDecision;
import com.meridian.parcel.domain.RemediationGate;
import com.meridian.parcel.domain.ReviewItem;
import com.meridian.parcel.domain.TransitionResult;
import com.meridian.routing.application.RoutingService;
import com.meridian.routing.domain.RoutingDecision;
import com.meridian.routing.domain.ShipmentProfile;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class ParcelService {

    private final ParcelRepository parcels;
    private final ReviewQueue reviewQueue;
    private final RoutingService routing;
    private final AuditLog auditLog;
    private final Clock clock;
    private final ParcelStateMachine stateMachine = new ParcelStateMachine();
    private final RemediationGate remediationGate = new RemediationGate();

    public ParcelService(ParcelRepository parcels, ReviewQueue reviewQueue, RoutingService routing,
                         AuditLog auditLog, Clock clock) {
        this.parcels = parcels;
        this.reviewQueue = reviewQueue;
        this.routing = routing;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    public Parcel register(String parcelId, ShipmentProfile profile, String user) {
        RoutingDecision decision = routing.route(profile)
                .orElseThrow(() -> new NotFoundException("route for destination", profile.destinationPostcode()));
        Parcel parcel = new Parcel(parcelId, profile, ParcelState.CREATED, decision.ruleId(),
                decision.carrierService().name());
        parcels.save(parcel);
        auditLog.record(user, "parcel.registered", parcelId,
                "routed to %s by rule %s".formatted(decision.carrierService(), decision.ruleId()));
        return parcel;
    }

    public synchronized EventResult apply(ParcelEvent event) {
        Parcel parcel = find(event.parcelId());
        if (!parcels.markProcessed(event.eventId())) {
            return new EventResult(parcel.id(), event.eventId(), EventOutcome.DUPLICATE_IGNORED, parcel.state(), null);
        }
        TransitionResult transition = stateMachine.apply(parcel.state(), event.type());
        if (transition.accepted()) {
            parcels.save(parcel.withState(transition.to()));
            return new EventResult(parcel.id(), event.eventId(), EventOutcome.APPLIED, transition.to(), null);
        }
        if (remediationGate.decide(parcel.state(), event.type()) == RemediationDecision.SAFE_TO_AUTO) {
            return new EventResult(parcel.id(), event.eventId(), EventOutcome.STALE_IGNORED, parcel.state(), null);
        }
        ReviewItem item = new ReviewItem("rev-" + UUID.randomUUID().toString().substring(0, 8), parcel.id(),
                event.eventId(), parcel.state(), event.type(), transition.reason(), clock.instant());
        reviewQueue.add(item);
        auditLog.record("system", "parcel.sent-to-review", parcel.id(), transition.reason());
        return new EventResult(parcel.id(), event.eventId(), EventOutcome.SENT_TO_REVIEW, parcel.state(), item.id());
    }

    public Parcel find(String parcelId) {
        return parcels.findById(parcelId).orElseThrow(() -> new NotFoundException("parcel", parcelId));
    }

    public List<Parcel> all() {
        return parcels.findAll();
    }

    public List<ReviewItem> reviewQueue() {
        return reviewQueue.pending();
    }
}
