import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { api } from "../lib/api";
import { RuleSandbox } from "./RuleSandbox";

vi.mock("../lib/api", () => ({
  api: {
    rules: vi.fn(),
    ruleChangeExamples: vi.fn(),
    testRun: vi.fn(),
  },
}));

const gap = {
  id: "gap",
  title: "A region left without a courier",
  description: "The rule for postcodes 4… is removed by mistake.",
  remove: ["r-4-standard"],
  add: [],
};

describe("rule change test run", () => {
  beforeEach(() => {
    vi.mocked(api.rules).mockResolvedValue({
      version: 1,
      rules: [
        {
          id: "r-4-standard",
          match: { destinationPrefix: "4", serviceLevel: null, weightBand: null, customerTier: null },
          priority: 500,
          carrierService: "DHL_PAKET",
        },
      ],
    });
    vi.mocked(api.ruleChangeExamples).mockResolvedValue([gap]);
    vi.mocked(api.testRun).mockResolvedValue({
      baseVersion: 1,
      evaluated: 120,
      changed: 12,
      unrouted: 12,
      passed: true,
      examples: [{ shipmentId: "s-014", before: "DHL_PAKET", after: null }],
      conflicts: [],
    });
  });

  it("shows the change and warns when parcels would be left without a courier", async () => {
    render(<RuleSandbox />);

    expect(await screen.findByText("4… → DHL Paket")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Run the test run" }));

    expect(await screen.findByText("Passed, with a warning")).toBeInTheDocument();
    expect(screen.getByText("no courier")).toBeInTheDocument();
    expect(api.testRun).toHaveBeenCalledWith(gap);
  });
});
