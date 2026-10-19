package com.meridian.change.application;

import com.meridian.change.domain.SampleShipment;

import java.util.List;

public interface SampleShipmentSource {

    List<SampleShipment> recordedShipments();
}
