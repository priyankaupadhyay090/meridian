import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ParcelForm, parcelPresets } from "../components/ParcelForm";
import { QuoteResult, useQuote } from "../components/Quote";
import { api } from "../lib/api";
import { courierNames, date, delivery, grams, money, sizeLimit } from "../lib/format";
import type { Courier, CourierRate, Parcel } from "../lib/types";
import { useLoad } from "../lib/useLoad";

export function ComparePage() {
  const [parcel, setParcel] = useState<Parcel>(parcelPresets[1].parcel);
  const quote = useQuote(parcel);
  const rates = useLoad(api.couriers, []);

  const byCourier = useMemo(() => {
    const groups = new Map<Courier, CourierRate[]>();
    for (const rate of rates.data ?? []) {
      groups.set(rate.courier, [...(groups.get(rate.courier) ?? []), rate]);
    }
    return [...groups.entries()];
  }, [rates.data]);

  return (
    <>
      <section className="page-hero">
        <div className="container narrow">
          <p className="eyebrow">Compare couriers</p>
          <h1>One parcel, every courier, real prices</h1>
          <p className="lead">
            Enter weight and size. Meridian checks each courier's published size and weight limits, picks the
            cheapest service that fits, and shows price and delivery time side by side.
          </p>
        </div>
      </section>

      <section className="section-tight">
        <div className="container compare-grid">
          <div className="panel">
            <h2 className="h3">Your parcel</h2>
            <ParcelForm initial={parcel} onSubmit={setParcel} />
          </div>
          <div aria-live="polite">
            <QuoteResult name="compare" options={quote.options} error={quote.error} />
            <p className="small muted fine-print">
              Prices are private-customer prices from the couriers' published lists. Contract prices for shops are
              usually lower. Delivery times are estimates until measured delivery times are available.
            </p>
          </div>
        </div>
      </section>

      <section className="section">
        <div className="container">
          <div className="section-head">
            <p className="eyebrow">Price list</p>
            <h2>Every price, with where it comes from</h2>
          </div>
          {rates.error && <p className="notice notice-bad">{rates.error}</p>}
          <div className="rate-groups">
            {byCourier.map(([courier, list]) => (
              <div key={courier} className="panel rate-group">
                <h3>{courierNames[courier]}</h3>
                <p className="small muted">
                  {list[0].sizeRule === "LONGEST_PLUS_SHORTEST"
                    ? "Size measured as longest plus shortest side."
                    : "Size measured as length × width × height."}{" "}
                  <a href={list[0].source} target="_blank" rel="noreferrer">
                    {list[0].sourceKind === "OFFICIAL" ? "Official price list" : "Secondary source"}
                  </a>
                  , checked {date(list[0].checkedOn)}.
                </p>
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th scope="col">Service</th>
                        <th scope="col">Up to</th>
                        <th scope="col">Size</th>
                        <th scope="col">Delivery</th>
                        <th scope="col" className="num">
                          Price
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {list.map((rate) => (
                        <tr key={rate.service}>
                          <td>{rate.service}</td>
                          <td className="nowrap">{grams(rate.maxWeightGrams)}</td>
                          <td>{sizeLimit(rate)}</td>
                          <td className="nowrap">{delivery(rate.deliveryDaysMin, rate.deliveryDaysMax)}</td>
                          <td className="num nowrap">{money(rate.priceCents)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            ))}
          </div>
          <p className="small muted fine-print">
            Want to see what a shop's customers get at checkout? <Link to="/checkout">Open the checkout preview</Link>.
          </p>
        </div>
      </section>
    </>
  );
}
