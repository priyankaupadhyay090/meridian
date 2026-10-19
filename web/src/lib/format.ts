import type { CourierRate, RuleProposal } from "./types";

const euro = new Intl.NumberFormat("de-DE", { style: "currency", currency: "EUR" });

export function money(cents: number): string {
  return euro.format(cents / 100);
}

export function delivery(min: number, max: number): string {
  const days = min === max ? `${max}` : `${min}–${max}`;
  return `${days} working ${max === 1 ? "day" : "days"}`;
}

export const courierNames = {
  DHL: "DHL",
  HERMES: "Hermes",
  DPD: "DPD",
  GLS: "GLS",
} as const;

export function grams(weight: number): string {
  return weight >= 1000 ? `${(weight / 1000).toLocaleString("de-DE")} kg` : `${weight} g`;
}

export function sizeLimit(rate: CourierRate): string {
  if (rate.sizeRule === "LONGEST_PLUS_SHORTEST") {
    return `longest + shortest side ≤ ${rate.maxLongestPlusShortestCm} cm`;
  }
  return `≤ ${rate.maxLengthCm} × ${rate.maxWidthCm} × ${rate.maxHeightCm} cm`;
}

export function date(iso: string): string {
  return new Date(`${iso}T12:00:00Z`).toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" });
}

export function duration(hours: number): string {
  return hours % 24 === 0 ? `${hours / 24} ${hours === 24 ? "day" : "days"}` : `${hours} hours`;
}

export function weightBand(band: RuleProposal["weightBand"]): string {
  if (!band) return "any weight";
  if (band.maxGrams >= 2_000_000_000) return `over ${grams(band.minGrams)}`;
  if (band.minGrams === 0) return `under ${grams(band.maxGrams)}`;
  return `${grams(band.minGrams)} to under ${grams(band.maxGrams)}`;
}

export function carrierService(service: string): string {
  const [courier, ...rest] = service.split("_");
  const name = courierNames[courier as keyof typeof courierNames] ?? courier;
  const product = rest.join(" ").toLowerCase().replace("paeckchen", "päckchen");
  return `${name} ${product.charAt(0).toUpperCase()}${product.slice(1)}`;
}
