package com.meridian.change.application;

import com.meridian.audit.adapter.persistence.InMemoryAuditLog;
import com.meridian.change.adapter.persistence.InMemoryRuleChangeRepository;
import com.meridian.change.domain.ChangeSource;
import com.meridian.change.domain.ChangeStatus;
import com.meridian.change.domain.ChangeWorkflowException;
import com.meridian.change.domain.DryRunReport;
import com.meridian.change.domain.FourEyesViolationException;
import com.meridian.change.domain.RuleChange;
import com.meridian.change.domain.SampleShipment;
import com.meridian.routing.adapter.persistence.InMemoryRuleSetStore;
import com.meridian.routing.domain.CarrierService;
import com.meridian.routing.domain.CustomerTier;
import com.meridian.routing.domain.InvalidRulesException;
import com.meridian.routing.domain.RoutingRule;
import com.meridian.routing.domain.RuleMatch;
import com.meridian.routing.domain.ServiceLevel;
import com.meridian.routing.domain.ShipmentProfile;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleChangeServiceTest {

    private static final RoutingRule BASE = rule("base", "1", 500, CarrierService.DHL_PAKET);

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-10T09:00:00Z"), ZoneOffset.UTC);
    private final InMemoryRuleSetStore ruleSets = new InMemoryRuleSetStore(List.of(BASE));
    private final InMemoryAuditLog auditLog = new InMemoryAuditLog(clock);
    private final List<SampleShipment> shipments = List.of(
            shipment("s1", "10115"), shipment("s2", "10437"), shipment("s3", "12000"));
    private final RuleChangeService service = new RuleChangeService(new InMemoryRuleChangeRepository(), ruleSets,
            () -> shipments, auditLog, clock);

    private static RoutingRule rule(String id, String prefix, int priority, CarrierService service) {
        return new RoutingRule(id, new RuleMatch(prefix, null, null, null), priority, service);
    }

    private static SampleShipment shipment(String id, String postcode) {
        return new SampleShipment(id, new ShipmentProfile(postcode, ServiceLevel.STANDARD, 1000, CustomerTier.BASIC));
    }

    private RuleChange draftWithSameDayFor10() {
        return service.draft("olivia", List.of(BASE, rule("express-10", "10", 100, CarrierService.GLS_EXPRESS)),
                ChangeSource.MANUAL, null);
    }

    @Test
    void dryRunShowsWhichShipmentsWouldRouteDifferently() {
        RuleChange change = draftWithSameDayFor10();

        DryRunReport report = service.runDryRun(change.id(), "olivia");

        assertThat(report.evaluated()).isEqualTo(3);
        assertThat(report.changed()).isEqualTo(2);
        assertThat(report.examples()).extracting("shipmentId").containsExactly("s1", "s2");
        assertThat(report.passed()).isTrue();
    }

    @Test
    void changeCannotBeSubmittedWithoutADryRun() {
        RuleChange change = draftWithSameDayFor10();

        assertThatThrownBy(() -> service.submit(change.id(), "olivia"))
                .isInstanceOf(ChangeWorkflowException.class)
                .hasMessageContaining("dry run");
    }

    @Test
    void authorCannotApproveTheirOwnChange() {
        RuleChange change = draftWithSameDayFor10();
        service.runDryRun(change.id(), "olivia");
        service.submit(change.id(), "olivia");

        assertThatThrownBy(() -> service.approve(change.id(), "olivia", "looks fine"))
                .isInstanceOf(FourEyesViolationException.class);
        assertThat(ruleSets.current().version()).isEqualTo(1);
    }

    @Test
    void approvalBySecondPersonActivatesANewVersion() {
        RuleChange change = draftWithSameDayFor10();
        service.runDryRun(change.id(), "olivia");
        service.submit(change.id(), "olivia");

        RuleChange approved = service.approve(change.id(), "anna", "matches the contract");

        assertThat(approved.status()).isEqualTo(ChangeStatus.ACTIVE);
        assertThat(approved.activatedVersion()).isEqualTo(2);
        assertThat(ruleSets.current().rules()).extracting(RoutingRule::id).contains("express-10");
        assertThat(auditLog.entries()).extracting("action").contains("change.approved", "change.submitted");
    }

    @Test
    void draftThatIsNotPendingIsNeverActivated() {
        RuleChange change = draftWithSameDayFor10();

        assertThatThrownBy(() -> service.approve(change.id(), "anna", "early"))
                .isInstanceOf(ChangeWorkflowException.class);
        assertThat(ruleSets.current().version()).isEqualTo(1);
    }

    @Test
    void conflictingRulesFailTheDryRunAndCannotBeSubmitted() {
        RuleChange change = service.draft("olivia", List.of(BASE, rule("a", "10", 100, CarrierService.GLS_EXPRESS),
                rule("b", "10", 100, CarrierService.DPD_CLASSIC)), ChangeSource.MANUAL, null);

        DryRunReport report = service.runDryRun(change.id(), "olivia");

        assertThat(report.passed()).isFalse();
        assertThatThrownBy(() -> service.submit(change.id(), "olivia")).hasMessageContaining("conflicts");
    }

    @Test
    void invalidRulesAreRejectedWhenDrafted() {
        List<RoutingRule> rules = new ArrayList<>(List.of(BASE));
        rules.add(rule("bad", "ten", 100, CarrierService.DHL_PAKET));

        assertThatThrownBy(() -> service.draft("olivia", rules, ChangeSource.MANUAL, null))
                .isInstanceOf(InvalidRulesException.class);
    }

    @Test
    void changeDraftedAgainstAnOlderVersionCannotBeActivated() {
        RuleChange first = draftWithSameDayFor10();
        RuleChange second = service.draft("omar", List.of(BASE, rule("express", "12", 100, CarrierService.DPD_EXPRESS)),
                ChangeSource.MANUAL, null);
        service.runDryRun(first.id(), "olivia");
        service.submit(first.id(), "olivia");
        service.approve(first.id(), "anna", "ok");

        assertThatThrownBy(() -> service.runDryRun(second.id(), "omar"))
                .isInstanceOf(ChangeWorkflowException.class)
                .hasMessageContaining("version");
    }

    @Test
    void rollbackRestoresThePreviousRulesAsANewVersion() {
        RuleChange change = draftWithSameDayFor10();
        service.runDryRun(change.id(), "olivia");
        service.submit(change.id(), "olivia");
        service.approve(change.id(), "anna", "ok");

        var restored = service.rollback("anna");

        assertThat(restored.version()).isEqualTo(3);
        assertThat(restored.rules()).containsExactly(BASE);
    }
}
