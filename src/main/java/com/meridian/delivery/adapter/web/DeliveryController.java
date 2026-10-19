package com.meridian.delivery.adapter.web;

import com.meridian.delivery.application.DeliveryService;
import com.meridian.delivery.domain.CourierRate;
import com.meridian.delivery.domain.DeliveryOption;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public")
public class DeliveryController {

    private final DeliveryService delivery;

    public DeliveryController(DeliveryService delivery) {
        this.delivery = delivery;
    }

    @GetMapping("/couriers")
    List<CourierRate> publishedRates() {
        return delivery.publishedRates();
    }

    @PostMapping("/quotes")
    List<DeliveryOption> quote(@Valid @RequestBody ParcelRequest parcel) {
        return delivery.compare(parcel.toParcel());
    }

    @PostMapping("/checkout-options")
    List<DeliveryOption> checkoutOptions(@Valid @RequestBody CheckoutOptionsRequest request) {
        return delivery.checkoutOptions(request.parcel().toParcel(), request.shopDefault(),
                request.shopShippingCents(), request.couriers());
    }
}
