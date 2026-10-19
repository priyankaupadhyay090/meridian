import { Link } from "react-router-dom";
import { parcelPresets } from "../components/ParcelForm";
import { useQuote } from "../components/Quote";
import { api } from "../lib/api";
import { courierNames, delivery, money } from "../lib/format";
import { useLoad } from "../lib/useLoad";

const headphones = parcelPresets[1].parcel;

const features = [
  {
    to: "/compare",
    eyebrow: "Compare",
    title: "Real prices for every parcel",
    text: "Each courier measures parcels its own way. Meridian applies each courier's rule, picks the cheapest service that fits and links every price to its source.",
    link: "Compare couriers",
  },
  {
    to: "/contract-check",
    eyebrow: "Contract check",
    title: "Contracts checked against German law",
    text: "Upload a courier contract as PDF or Word. Routing clauses become draft rules, and a term that breaks the HGB or the PostG stops the draft.",
    link: "Check a contract",
  },
  {
    to: "/rule-changes",
    eyebrow: "Rule changes",
    title: "A test run and a second person",
    text: "A change to the routing rules runs on recent parcels first and needs a second approver before it goes live in one step.",
    link: "Try a test run",
  },
  {
    to: "/checkout",
    eyebrow: "Checkout",
    title: "Customers choose how it travels",
    text: "The shop's own offer next to the cheapest and the fastest courier, with prices that link to their source.",
    link: "Open the preview",
  },
];

const today = [
  { title: "Courier comparison", to: "/compare" },
  { title: "Checkout choice", to: "/checkout" },
  { title: "Contract check", to: "/contract-check" },
  { title: "Private contract storage", to: "/library" },
  { title: "Safe rule changes", to: "/rule-changes" },
  { title: "Parcel events on Kafka", to: null },
  { title: "Live monitoring", to: null },
];

const nextPhase = [
  { title: "Delivery promise check", text: "Promised vs. measured" },
  { title: "CO₂ per option", text: "Emissions next to price" },
  { title: "Smart parcel", text: "Best box and split" },
  { title: "Claims helper", text: "Legal deadlines on time" },
];

function LiveQuote() {
  const { options, error } = useQuote(headphones);
  return (
    <div className="hero-card panel" aria-live="polite">
      <p className="small muted">Live comparison · Headphones, 900 g, 30 × 20 × 10 cm</p>
      {error && <p className="notice notice-bad">{error}</p>}
      {!options && !error && <p className="muted">Loading prices…</p>}
      <ul className="mini-options">
        {options?.map((option) => (
          <li key={option.courier + option.service}>
            <span>
              <strong>{courierNames[option.courier]}</strong> <span className="muted">{option.service}</span>
              {option.labels.includes("CHEAPEST") && <span className="tag tag-cheapest">Cheapest</span>}
              {option.labels.includes("FASTEST") && <span className="tag tag-fastest">Fastest</span>}
              <span className="small muted block">{delivery(option.deliveryDaysMin, option.deliveryDaysMax)}</span>
            </span>
            <span className="price">{money(option.priceCents)}</span>
          </li>
        ))}
      </ul>
      <Link to="/compare" className="text-link">
        Compare your own parcel →
      </Link>
    </div>
  );
}

export function HomePage() {
  const rates = useLoad(api.couriers, []);

  return (
    <>
      <section className="hero">
        <div className="container hero-grid">
          <div>
            <p className="eyebrow">Shipping for German online shops</p>
            <h1>
              Every parcel to the right courier, <span className="accent">on terms that hold.</span>
            </h1>
            <p className="lead">
              Meridian compares real DHL, Hermes, DPD and GLS prices for each parcel, reads courier contracts and checks
              them against German law, and changes routing rules only after a test run and a second approval.
            </p>
            <div className="btn-row">
              <Link to="/compare" className="btn btn-primary">
                Compare couriers
              </Link>
              <Link to="/contract-check" className="btn btn-outline">
                See the contract check
              </Link>
            </div>
          </div>
          <LiveQuote />
        </div>
      </section>

      <section className="facts" aria-label="Meridian in numbers">
        <div className="container facts-row">
          <div>
            <strong>4</strong>
            <span>couriers compared</span>
          </div>
          <div>
            <strong>{rates.data?.length ?? "–"}</strong>
            <span>published prices, each with its source</span>
          </div>
          <div>
            <strong>2</strong>
            <span>laws checked: HGB and PostG</span>
          </div>
          <div>
            <strong>2</strong>
            <span>people behind every rule change</span>
          </div>
        </div>
      </section>

      <section className="section">
        <div className="container">
          <div className="section-head">
            <p className="eyebrow">What it does</p>
            <h2>
              The layer between a shop <span className="accent">and its couriers</span>
            </h2>
          </div>
          <div className="features">
            {features.map((feature) => (
              <Link key={feature.to} to={feature.to} className="feature">
                <span className="eyebrow">{feature.eyebrow}</span>
                <h3>{feature.title}</h3>
                <p className="muted">{feature.text}</p>
                <span className="text-link">{feature.link} →</span>
              </Link>
            ))}
          </div>
        </div>
      </section>

      <section className="section" id="status">
        <div className="container">
          <div className="section-head">
            <p className="eyebrow">Status</p>
            <h2>What runs today and what comes next</h2>
          </div>
          <h3 className="tiles-title">Today</h3>
          <ul className="tiles">
            {today.map((item) => (
              <li key={item.title}>
                {item.to ? (
                  <Link to={item.to} className="tile-box">
                    <span className="tile-check" aria-hidden="true" />
                    {item.title}
                  </Link>
                ) : (
                  <span className="tile-box">
                    <span className="tile-check" aria-hidden="true" />
                    {item.title}
                  </span>
                )}
              </li>
            ))}
          </ul>

          <h3 className="tiles-title next-head">Next phase</h3>
          <ul className="tiles next">
            {nextPhase.map((item) => (
              <li key={item.title} className="tile-box tile-next">
                <span className="tag-next">Next</span>
                <strong>{item.title}</strong>
                <span className="small muted">{item.text}</span>
              </li>
            ))}
          </ul>
        </div>
      </section>

      <section className="section">
        <div className="container">
          <div className="closing panel">
            <div>
              <h2>Try it with your own parcel or contract</h2>
              <p className="muted">No account needed. Nothing you enter is stored.</p>
            </div>
            <div className="btn-row">
              <Link to="/compare" className="btn btn-primary">
                Compare couriers
              </Link>
              <Link to="/contract-check#try" className="btn btn-outline">
                Check a contract
              </Link>
            </div>
          </div>
        </div>
      </section>
    </>
  );
}
