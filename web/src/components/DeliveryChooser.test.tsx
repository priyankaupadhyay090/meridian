import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import type { DeliveryOption } from "../lib/types";
import { DeliveryChooser, optionKey } from "./DeliveryChooser";

const options: DeliveryOption[] = [
  {
    courier: "HERMES",
    service: "S-Paket",
    priceCents: 0,
    deliveryDaysMin: 2,
    deliveryDaysMax: 3,
    deliveryTimeBasis: "ESTIMATE",
    priceSource: "price set by the shop",
    priceSourceKind: null,
    labels: ["SHOP_DEFAULT", "CHEAPEST"],
  },
  {
    courier: "DHL",
    service: "Päckchen S",
    priceCents: 419,
    deliveryDaysMin: 1,
    deliveryDaysMax: 2,
    deliveryTimeBasis: "ESTIMATE",
    priceSource: "https://example.org/dhl",
    priceSourceKind: "SECONDARY",
    labels: ["FASTEST"],
  },
];

function Chooser() {
  const [selected, setSelected] = useState(optionKey(options[0]));
  return <DeliveryChooser name="test" legend="Delivery" options={options} selected={selected} onSelect={setSelected} />;
}

describe("delivery choice", () => {
  it("starts with the shop's own offer and lets the customer pick another courier", async () => {
    render(<Chooser />);

    expect(screen.getByRole("radio", { name: /Hermes/ })).toBeChecked();
    expect(screen.getByText("Free")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("radio", { name: /DHL/ }));

    expect(screen.getByRole("radio", { name: /DHL/ })).toBeChecked();
  });

  it("links a published price to its source and names the shop's own price", () => {
    render(<Chooser />);

    expect(screen.getByRole("link", { name: /published price, secondary source/ })).toHaveAttribute(
      "href",
      "https://example.org/dhl",
    );
    expect(screen.getByText(/price set by the shop/)).toBeInTheDocument();
  });
});
