import { courierNames, delivery, money } from "../lib/format";
import type { DeliveryOption } from "../lib/types";

const labelText = { CHEAPEST: "Cheapest", FASTEST: "Fastest", SHOP_DEFAULT: "Shop's choice" } as const;

export function optionKey(option: DeliveryOption) {
  return `${option.courier}/${option.service}`;
}

export function DeliveryChooser({
  name,
  legend,
  options,
  selected,
  onSelect,
}: {
  name: string;
  legend: string;
  options: DeliveryOption[];
  selected: string;
  onSelect: (key: string) => void;
}) {
  return (
    <fieldset className="delivery">
      <legend className="small muted">{legend}</legend>
      {options.map((option) => {
        const key = optionKey(option);
        const fromShop = option.priceSourceKind === null;
        return (
          <label key={key} className={`delivery-option${selected === key ? " is-selected" : ""}`}>
            <input type="radio" name={name} value={key} checked={selected === key} onChange={() => onSelect(key)} />
            <span className="delivery-main">
              <span className="delivery-name">
                <strong>{courierNames[option.courier]}</strong> <span className="muted">{option.service}</span>
                {option.labels.map((label) => (
                  <span key={label} className={`tag tag-${label.toLowerCase()}`}>
                    {labelText[label]}
                  </span>
                ))}
              </span>
              <span className="small muted">
                {delivery(option.deliveryDaysMin, option.deliveryDaysMax)}
                {option.deliveryTimeBasis === "ESTIMATE" && " (estimate)"} ·{" "}
                {fromShop ? (
                  "price set by the shop"
                ) : (
                  <a href={option.priceSource} target="_blank" rel="noreferrer">
                    {option.priceSourceKind === "OFFICIAL" ? "official price list" : "published price, secondary source"}
                  </a>
                )}
              </span>
            </span>
            <span className="delivery-price">{option.priceCents === 0 ? "Free" : money(option.priceCents)}</span>
          </label>
        );
      })}
    </fieldset>
  );
}
