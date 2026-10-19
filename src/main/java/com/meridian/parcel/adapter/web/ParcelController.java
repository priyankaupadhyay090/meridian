package com.meridian.parcel.adapter.web;

import com.meridian.parcel.application.ParcelService;
import com.meridian.parcel.domain.EventResult;
import com.meridian.parcel.domain.Parcel;
import com.meridian.parcel.domain.ReviewItem;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Clock;
import java.util.List;

@RestController
@RequestMapping("/api/console")
public class ParcelController {

    private final ParcelService parcels;
    private final Clock clock;

    public ParcelController(ParcelService parcels, Clock clock) {
        this.parcels = parcels;
        this.clock = clock;
    }

    @PostMapping("/parcels")
    @ResponseStatus(HttpStatus.CREATED)
    Parcel register(@Valid @RequestBody RegisterParcelRequest request, Principal user) {
        return parcels.register(request.id(), request.profile(), user.getName());
    }

    @GetMapping("/parcels")
    List<Parcel> all() {
        return parcels.all();
    }

    @GetMapping("/parcels/{id}")
    Parcel one(@PathVariable String id) {
        return parcels.find(id);
    }

    @PostMapping("/events")
    EventResult apply(@Valid @RequestBody ParcelEventRequest request) {
        return parcels.apply(request.toEvent(clock.instant()));
    }

    @GetMapping("/review-queue")
    List<ReviewItem> reviewQueue() {
        return parcels.reviewQueue();
    }
}
