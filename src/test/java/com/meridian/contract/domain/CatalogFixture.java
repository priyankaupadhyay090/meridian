package com.meridian.contract.domain;

import com.meridian.routing.domain.CarrierService;

import java.util.List;

public final class CatalogFixture {

    public static final ClauseCatalog CATALOG = new ClauseCatalog(
            List.of(new CarrierAlias("dpd express", CarrierService.DPD_EXPRESS),
                    new CarrierAlias("dpd classic", CarrierService.DPD_CLASSIC),
                    new CarrierAlias("dhl paket", CarrierService.DHL_PAKET),
                    new CarrierAlias("dhl päckchen", CarrierService.DHL_PAECKCHEN),
                    new CarrierAlias("gls express", CarrierService.GLS_EXPRESS),
                    new CarrierAlias("hermes paket", CarrierService.HERMES_PAKET)),
            List.of("if", "between", "as long as", "volume", "zwischen", "sofern"),
            List.of("ignore", "instruction", "add a rule"));

    private CatalogFixture() {
    }
}
