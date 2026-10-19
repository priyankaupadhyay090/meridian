import { render, screen } from "@testing-library/react";
import type { ContractCheck } from "../lib/types";
import { ContractResult } from "./ContractResult";

const blocked: ContractCheck = {
  context: { senderType: "BUSINESS", termsBasis: "STANDARD_TERMS", checkedOn: "2026-10-12" },
  accepted: [
    {
      destinationPrefix: "10",
      serviceLevel: "EXPRESS",
      weightBand: null,
      customerTier: null,
      priority: 100,
      carrierService: "DPD_EXPRESS",
      sourceQuote: "Express parcels to postcodes starting with 10 are handled by DPD Express.",
    },
  ],
  rejected: [],
  unresolved: [
    {
      clause: { reference: "3.2", text: "If volume exceeds 500 parcels, postcodes starting with 11 move to DPD Classic." },
      reason: "depends on a condition (\"if\") that a routing rule cannot express",
    },
  ],
  unrecognised: [],
  terms: {
    liabilityCap: { sdrPerKilogram: 1, clause: "Liability is limited to 1 SDR per kilogram." },
    hiddenDamageNotice: { hours: 72, clause: "Hidden damage must be reported within 3 days." },
    delayNotice: null,
    limitation: null,
    delayLiabilityExclusion: null,
    heaviestCoveredGrams: 0,
    heavyParcelLabelling: null,
    twoPersonDelivery: null,
  },
  findings: [
    {
      ruleId: "HGB-449-2-LIABILITY",
      severity: "VIOLATION",
      lawReference: "§ 449 Abs. 2 HGB",
      message: "1.0 SDR per kg is below the corridor of 2 to 40 SDR per kg.",
      clause: "Liability is limited to 1 SDR per kilogram.",
      source: "https://www.gesetze-im-internet.de/hgb/__449.html",
    },
  ],
  canDraft: false,
  noDraftReason: "the contract has terms that conflict with German freight law",
};

describe("contract result", () => {
  it("says the draft is blocked and links each finding to the law", () => {
    render(<ContractResult result={blocked} />);

    expect(screen.getByText("Blocked")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "§ 449 Abs. 2 HGB" })).toHaveAttribute(
      "href",
      "https://www.gesetze-im-internet.de/hgb/__449.html",
    );
    expect(screen.getAllByText("Liability is limited to 1 SDR per kilogram.")[0]).toBeInTheDocument();
  });

  it("shows draft rules, clauses left for a person and the terms it read", () => {
    render(<ContractResult result={blocked} />);

    expect(screen.getByText("DPD Express")).toBeInTheDocument();
    expect(screen.getByText(/Not turned into a rule: it depends on a condition/)).toBeInTheDocument();
    expect(screen.getByText("3 days")).toBeInTheDocument();
    expect(screen.getByText("1 SDR per kg")).toBeInTheDocument();
  });
});
