package com.meridian.delivery.application;

import com.meridian.delivery.domain.CourierRate;

import java.util.List;

public interface CourierRateRepository {

    List<CourierRate> rates();
}
