import { Link } from "react-router-dom";
import { useSession } from "../lib/session";

const officialTerms = [
  { courier: "DHL", title: "AGB DHL Paket / Express National", href: "https://www.dhl.de/agb", note: "Overview of DHL's current business terms" },
  { courier: "DPD", title: "Allgemeine Geschäftsbedingungen DPD", href: "https://www.dpd.com/de/de/agb/", note: "DPD Deutschland's current terms" },
  { courier: "GLS", title: "AGB GLS Germany 2025", href: "https://www.gls-pakete.de/downloads/AGB_GLS_Germany_2025-01_DE_2024-10-25_FINAL-WEB.pdf", note: "PDF for business customers" },
  { courier: "Hermes", title: "AGB Hermes Privatpaketservice", href: "https://www.myhermes.de/content/pdf/hg_agb_privatpaketservice.pdf", note: "PDF for private customers" },
];

const samples = [
  { title: "Sample contract (English)", note: "Written for this demo · gives Blocked", files: [["PDF", "/samples/sample-contract.pdf"], ["Word", "/samples/sample-contract.docx"]] },
  { title: "Muster-Vertrag (Deutsch)", note: "Written for this demo · hides an instruction", files: [["PDF", "/samples/muster-vertrag.pdf"], ["Word", "/samples/muster-vertrag.docx"]] },
];

export function LibraryPage() {
  const { account } = useSession();
  return (
    <>
      <section className="page-hero">
        <div className="container narrow">
          <p className="eyebrow">Contract library</p>
          <h1>Courier terms to read and check</h1>
          <p className="lead">The couriers' official terms, sample contracts to try, and a private place for your own.</p>
        </div>
      </section>

      <section className="section-tight">
        <div className="container">
          <div className="section-head">
            <h2>Official courier terms</h2>
            <p className="lead">
              Linked on the couriers' own websites. Meridian links to them instead of copying them, because they are the
              couriers' texts.
            </p>
          </div>
          <ul className="library">
            {officialTerms.map((terms) => (
              <li key={terms.courier} className="panel library-item">
                <div>
                  <h3 className="h3">{terms.title}</h3>
                  <p className="small muted">
                    {terms.courier} · {terms.note}
                  </p>
                </div>
                <div className="library-actions">
                  <a className="btn btn-outline btn-small" href={terms.href} target="_blank" rel="noreferrer">
                    Open on {terms.courier}'s site
                  </a>
                </div>
              </li>
            ))}
          </ul>
        </div>
      </section>

      <section className="section-tight">
        <div className="container">
          <div className="section-head">
            <h2>Samples to try</h2>
          </div>
          <ul className="library">
            {samples.map((sample) => (
              <li key={sample.title} className="panel library-item">
                <div>
                  <h3 className="h3">{sample.title}</h3>
                  <p className="small muted">{sample.note}</p>
                </div>
                <div className="library-actions">
                  {sample.files.map(([label, href]) => (
                    <a key={href} className="btn btn-outline btn-small" href={href} download>
                      {label}
                    </a>
                  ))}
                </div>
              </li>
            ))}
          </ul>
        </div>
      </section>

      <section className="section-tight">
        <div className="container">
          <div className="panel private-cta">
            <div>
              <h2 className="h3">Your own contracts stay private</h2>
              <p className="muted">
                A business keeps its contracts in private storage. No other business can see them, and every look by
                Meridian's internal team is written to the audit log.
              </p>
            </div>
            <Link to={account ? "/my-contracts" : "/sign-in"} className="btn btn-primary">
              {account ? "Open my contracts" : "Sign in as a business"}
            </Link>
          </div>
        </div>
      </section>
    </>
  );
}
