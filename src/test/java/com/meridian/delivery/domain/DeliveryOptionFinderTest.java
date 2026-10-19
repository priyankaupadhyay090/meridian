package com.meridian.delivery.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryOptionFinderTest {

    private static final LocalDate CHECKED = LocalDate.of(2026, 10, 10);
    private static final List<CourierRate> RATES = List.of(
            byDimensions(Courier.DHL, "Päckchen S", 2_000, 35, 25, 10, 419, 1, 3),
            byDimensions(Courier.DHL, "Paket 5 kg", 5_000, 120, 60, 60, 769, 1, 2),
            bySideSum(Courier.HERMES, "S-Paket", 25_000, 50, 579, 2, 3),
            bySideSum(Courier.HERMES, "M-Paket", 25_000, 80, 699, 2, 3),
            bySideSum(Courier.GLS, "XS", 31_500, 35, 459, 1, 2));

    private final DeliveryOptionFinder finder = new DeliveryOptionFinder();

    private static CourierRate byDimensions(Courier courier, String service, int grams, int l, int w, int h, long price,
                                            int min, int max) {
        return new CourierRate(courier, service, grams, SizeRule.MAX_DIMENSIONS, l, w, h, null, price, min, max,
                DeliveryTimeBasis.ESTIMATE, "https://example.org/" + courier, SourceKind.SECONDARY, CHECKED, "");
    }

    private static CourierRate bySideSum(Courier courier, String service, int grams, int sum, long price, int min,
                                         int max) {
        return new CourierRate(courier, service, grams, SizeRule.LONGEST_PLUS_SHORTEST, null, null, null, sum, price,
                min, max, DeliveryTimeBasis.ESTIMATE, "https://example.org/" + courier, SourceKind.OFFICIAL, CHECKED, "");
    }

    @Test
    void eachCourierOffersItsCheapestFittingServiceAndACourierWithoutAFittingSizeIsLeftOut() {
        Parcel headphones = new Parcel(900, 30, 20, 10);

        List<DeliveryOption> options = finder.options(headphones, Courier.HERMES, 590,
                Set.of(Courier.HERMES, Courier.DHL, Courier.GLS), RATES);

        assertThat(options).extracting(o -> o.courier() + " " + o.service() + " " + o.priceCents())
                .containsExactly("DHL Päckchen S 419", "HERMES S-Paket 590");
    }

    @Test
    void largerParcelMovesToTheNextSizeOrDropsACourier() {
        Parcel backpack = new Parcel(1_400, 45, 30, 12);

        List<DeliveryOption> options = finder.options(backpack, null, 0, Set.of(Courier.DHL, Courier.HERMES, Courier.GLS),
                RATES);

        assertThat(options).extracting(o -> o.courier() + " " + o.service())
                .containsExactly("HERMES M-Paket", "DHL Paket 5 kg");
    }

    @Test
    void shopDefaultUsesTheShopsOwnPriceAndLabelsMarkCheapestAndFastest() {
        Parcel small = new Parcel(500, 20, 15, 5);

        List<DeliveryOption> options = finder.options(small, Courier.HERMES, 0, Set.of(Courier.HERMES, Courier.GLS), RATES);

        DeliveryOption hermes = options.stream().filter(o -> o.courier() == Courier.HERMES).findFirst().orElseThrow();
        DeliveryOption gls = options.stream().filter(o -> o.courier() == Courier.GLS).findFirst().orElseThrow();
        assertThat(hermes.priceCents()).isZero();
        assertThat(hermes.priceSource()).isEqualTo(DeliveryOptionFinder.SHOP_PRICE_SOURCE);
        assertThat(hermes.labels()).contains(OptionLabel.SHOP_DEFAULT, OptionLabel.CHEAPEST);
        assertThat(gls.labels()).containsExactly(OptionLabel.FASTEST);
    }

    @Test
    void parcelTooHeavyForEveryCourierHasNoOptions() {
        assertThat(finder.options(new Parcel(40_000, 50, 40, 40), null, 0, Set.of(Courier.DHL, Courier.HERMES), RATES))
                .isEmpty();
    }
}
