export type Courier = "DHL" | "HERMES" | "DPD" | "GLS";

export type OptionLabel = "CHEAPEST" | "FASTEST" | "SHOP_DEFAULT";

export interface Parcel {
  weightGrams: number;
  lengthCm: number;
  widthCm: number;
  heightCm: number;
}

export interface DeliveryOption {
  courier: Courier;
  service: string;
  priceCents: number;
  deliveryDaysMin: number;
  deliveryDaysMax: number;
  deliveryTimeBasis: "ESTIMATE" | "MEASURED";
  priceSource: string;
  priceSourceKind: "OFFICIAL" | "SECONDARY" | null;
  labels: OptionLabel[];
}

export interface CourierRate {
  courier: Courier;
  service: string;
  maxWeightGrams: number;
  sizeRule: "MAX_DIMENSIONS" | "LONGEST_PLUS_SHORTEST";
  maxLengthCm: number | null;
  maxWidthCm: number | null;
  maxHeightCm: number | null;
  maxLongestPlusShortestCm: number | null;
  priceCents: number;
  deliveryDaysMin: number;
  deliveryDaysMax: number;
  deliveryTimeBasis: "ESTIMATE" | "MEASURED";
  source: string;
  sourceKind: "OFFICIAL" | "SECONDARY";
  checkedOn: string;
  note: string;
}

export type SenderType = "BUSINESS" | "CONSUMER";

export type TermsBasis = "STANDARD_TERMS" | "INDIVIDUALLY_NEGOTIATED";

export type Severity = "VIOLATION" | "CHECK" | "INFO";

export interface ContractExample {
  id: string;
  title: string;
  carrierName: string;
  senderType: SenderType;
  termsBasis: TermsBasis;
  text: string;
}

export interface Clause {
  reference: string;
  text: string;
}

export interface RuleProposal {
  destinationPrefix: string;
  serviceLevel: "ECONOMY" | "STANDARD" | "EXPRESS" | null;
  weightBand: { minGrams: number; maxGrams: number } | null;
  customerTier: "PREMIUM" | "BUSINESS" | "BASIC" | null;
  priority: number;
  carrierService: string;
  sourceQuote: string;
}

export interface Finding {
  ruleId: string;
  severity: Severity;
  lawReference: string;
  message: string;
  clause: string | null;
  source: string;
}

export interface ContractTerms {
  liabilityCap: { sdrPerKilogram: number; clause: string } | null;
  hiddenDamageNotice: { hours: number; clause: string } | null;
  delayNotice: { hours: number; clause: string } | null;
  limitation: { months: number; clause: string } | null;
  delayLiabilityExclusion: string | null;
  heaviestCoveredGrams: number;
  heavyParcelLabelling: string | null;
  twoPersonDelivery: string | null;
}

export interface ContractCheck {
  context: { senderType: SenderType; termsBasis: TermsBasis; checkedOn: string };
  accepted: RuleProposal[];
  rejected: { proposal: RuleProposal; reasons: string[] }[];
  unresolved: { clause: Clause; reason: string }[];
  unrecognised: Clause[];
  terms: ContractTerms;
  findings: Finding[];
  canDraft: boolean;
  noDraftReason: string | null;
}

export interface ContractCheckRequest {
  carrierName: string;
  text: string;
  senderType: SenderType;
  termsBasis: TermsBasis;
}

export interface RoutingRule {
  id: string;
  match: {
    destinationPrefix: string;
    serviceLevel: "ECONOMY" | "STANDARD" | "EXPRESS" | null;
    weightBand: { minGrams: number; maxGrams: number } | null;
    customerTier: "PREMIUM" | "BUSINESS" | "BASIC" | null;
  };
  priority: number;
  carrierService: string;
}

export interface RuleSet {
  version: number;
  rules: RoutingRule[];
}

export interface RuleChangeExample {
  id: string;
  title: string;
  description: string;
  remove: string[];
  add: RoutingRule[];
}

export interface TestRunReport {
  baseVersion: number;
  evaluated: number;
  changed: number;
  unrouted: number;
  passed: boolean;
  examples: { shipmentId: string; before: string | null; after: string | null }[];
  conflicts: { firstRuleId: string; secondRuleId: string; message: string }[];
}

export interface ExtractedDocument {
  fileName: string;
  format: "PDF" | "Word" | "OpenDocument" | "Text";
  text: string;
  characters: number;
}

export interface ContractFileItem {
  id: string;
  businessId: string;
  fileName: string;
  format: "PDF" | "Word" | "OpenDocument" | "Text";
  sizeBytes: number;
  carrierName: string | null;
  uploadedBy: string;
  uploadedAt: string;
}

export interface BusinessAccount {
  user: string;
  businessId: string;
  businessName: string;
}
