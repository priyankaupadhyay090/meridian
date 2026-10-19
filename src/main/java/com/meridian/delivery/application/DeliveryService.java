package com.meridian.delivery.application;

import com.meridian.delivery.domain.Courier;
import com.meridian.delivery.domain.CourierRate;
import com.meridian.delivery.domain.DeliveryOption;
import com.meridian.delivery.domain.DeliveryOptionFinder;
import com.meridian.delivery.domain.Parcel;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class DeliveryService {

    private final CourierRateRepository rates;
    private final DeliveryOptionFinder finder = new DeliveryOptionFinder();

    public DeliveryService(CourierRateRepository rates) {
        this.rates = rates;
    }

    public List<DeliveryOption> compare(Parcel parcel) {
        return finder.options(parcel, null, 0, EnumSet.allOf(Courier.class), rates.rates());
    }

    public List<DeliveryOption> checkoutOptions(Parcel parcel, Courier shopDefault, long shopShippingCents,
                                                Set<Courier> shopCouriers) {
        return finder.options(parcel, shopDefault, shopShippingCents, shopCouriers, rates.rates());
    }

    public List<CourierRate> publishedRates() {
        return rates.rates();
    }
}
