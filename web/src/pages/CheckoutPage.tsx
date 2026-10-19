import { useEffect, useState } from "react";
import { DeliveryChooser, optionKey } from "../components/DeliveryChooser";
import { parcelPresets } from "../components/ParcelForm";
import { api } from "../lib/api";
import { courierNames, money } from "../lib/format";
import type { Courier, DeliveryOption } from "../lib/types";

const allCouriers: Courier[] = ["DHL", "HERMES", "DPD", "GLS"];

export function CheckoutPage() {
  const [presetName, setPresetName] = useState(parcelPresets[1].name);
  const [couriers, setCouriers] = useState<Courier[]>(["DHL", "HERMES", "GLS"]);
  const [shopDefault, setShopDefault] = useState<Courier>("HERMES");
  const [shopPrice, setShopPrice] = useState(0);
  const [options, setOptions] = useState<DeliveryOption[] | null>(null);
  const [selected, setSelected] = useState("");
  const [error, setError] = useState<string | null>(null);

  const parcel = parcelPresets.find((preset) => preset.name === presetName)!.parcel;

  useEffect(() => {
    if (!couriers.includes(shopDefault)) {
      setShopDefault(couriers[0]);
      return;
    }
    let active = true;
    setError(null);
    api
      .checkoutOptions(parcel, shopDefault, shopPrice, couriers)
      .then((result) => {
        if (!active) return;
        setOptions(result);
        const preferred = result.find((option) => option.labels.includes("SHOP_DEFAULT")) ?? result[0];
        setSelected(preferred ? optionKey(preferred) : "");
      })
      .catch((failure: Error) => active && setError(failure.message));
    return () => {
      active = false;
    };
  }, [presetName, couriers, shopDefault, shopPrice]);

  function toggle(courier: Courier) {
    if (couriers.includes(courier)) {
      if (couriers.length > 1) setCouriers(couriers.filter((candidate) => candidate !== courier));
    } else {
      setCouriers(allCouriers.filter((candidate) => candidate === courier || couriers.includes(candidate)));
    }
  }

  const chosen = options?.find((option) => optionKey(option) === selected);

  return (
    <>
      <section className="page-hero">
        <div className="container narrow">
          <p className="eyebrow">Checkout</p>
          <h1>What a shop's customers see at checkout</h1>
          <p className="lead">
            The shop decides which couriers it can hand parcels to, which one it uses by default and what it charges
            for it. Its customers then choose: the shop's own offer, the cheapest courier or the fastest one.
          </p>
        </div>
      </section>

      <section className="section-tight">
        <div className="container checkout-grid">
          <div className="panel">
            <h2 className="h3">Shop settings</h2>
            <fieldset className="setting">
              <legend className="small muted">Couriers the shop works with</legend>
              <div className="toggles">
                {allCouriers.map((courier) => (
                  <label key={courier} className="toggle-row">
                    <span>{courierNames[courier]}</span>
                    <input
                      type="checkbox"
                      role="switch"
                      className="switch"
                      checked={couriers.includes(courier)}
                      onChange={() => toggle(courier)}
                    />
                  </label>
                ))}
              </div>
            </fieldset>
            <div className="field-row">
              <label className="field">
                <span className="small muted">Default courier</span>
                <select value={shopDefault} onChange={(event) => setShopDefault(event.target.value as Courier)}>
                  {couriers.map((courier) => (
                    <option key={courier} value={courier}>
                      {courierNames[courier]}
                    </option>
                  ))}
                </select>
              </label>
              <label className="field">
                <span className="small muted">Shop's price for it</span>
                <select value={shopPrice} onChange={(event) => setShopPrice(Number(event.target.value))}>
                  <option value={0}>Free shipping</option>
                  <option value={395}>{money(395)}</option>
                  <option value={490}>{money(490)}</option>
                </select>
              </label>
            </div>
            <label className="field">
              <span className="small muted">Order</span>
              <select value={presetName} onChange={(event) => setPresetName(event.target.value)}>
                {parcelPresets.map((preset) => (
                  <option key={preset.name} value={preset.name}>
                    {preset.name}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="panel checkout-preview" aria-live="polite">
            <p className="small muted preview-label">Preview of the shop's checkout</p>
            <h2 className="h3">Delivery</h2>
            {error && <p className="notice notice-bad">{error}</p>}
            {options && options.length === 0 && (
              <p className="notice">None of the shop's couriers takes this parcel. The shop needs another courier.</p>
            )}
            {options && options.length > 0 && (
              <DeliveryChooser
                name="checkout"
                legend="Choose how your order is delivered"
                options={options}
                selected={selected}
                onSelect={setSelected}
              />
            )}
            {chosen && (
              <p className="checkout-total">
                <span>Shipping</span>
                <strong>{chosen.priceCents === 0 ? "Free" : money(chosen.priceCents)}</strong>
              </p>
            )}
          </div>
        </div>
      </section>

      <section className="section">
        <div className="container">
          <div className="principles two">
            <div>
              <h3>The shop's offer stays the shop's</h3>
              <p className="muted">
                The default courier is shown at the shop's own price, often free. Other couriers are shown at their
                published price, so the customer can see what paying for speed or a cheaper service means.
              </p>
            </div>
            <div>
              <h3>Only couriers that take the parcel</h3>
              <p className="muted">
                Each courier's weight and size limits are checked for this order. A courier that cannot take it is
                not shown.
              </p>
            </div>
          </div>
        </div>
      </section>
    </>
  );
}
