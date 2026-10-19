import { carrierService, delivery, duration, grams, money, weightBand } from "./format";

describe("format", () => {
  it("shows euros the German way", () => {
    expect(money(419)).toMatch(/^4,19\s€$/);
  });

  it("writes delivery as working days", () => {
    expect(delivery(1, 1)).toBe("1 working day");
    expect(delivery(2, 4)).toBe("2–4 working days");
  });

  it("writes weights and notice periods in plain words", () => {
    expect(grams(900)).toBe("900 g");
    expect(grams(31500)).toBe("31,5 kg");
    expect(duration(72)).toBe("3 days");
    expect(duration(30)).toBe("30 hours");
  });

  it("describes weight bands and carrier services", () => {
    expect(weightBand(null)).toBe("any weight");
    expect(weightBand({ minGrams: 20000, maxGrams: 31500 })).toBe("20 kg to under 31,5 kg");
    expect(weightBand({ minGrams: 31500, maxGrams: 2147483647 })).toBe("over 31,5 kg");
    expect(carrierService("DHL_PAECKCHEN")).toBe("DHL Päckchen");
    expect(carrierService("DPD_CLASSIC")).toBe("DPD Classic");
  });
});
