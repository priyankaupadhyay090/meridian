import { carrierService, duration, grams, weightBand } from "../lib/format";
import type { ContractCheck, Severity } from "../lib/types";

export const severityText: Record<Severity, string> = {
  VIOLATION: "Violation",
  CHECK: "Needs a check",
  INFO: "Noted",
};

const levelText = { ECONOMY: "Economy", STANDARD: "Standard", EXPRESS: "Express" } as const;

export function SeverityBadge({ severity }: { severity: Severity }) {
  return <span className={`badge badge-${severity.toLowerCase()}`}>{severityText[severity]}</span>;
}

function Verdict({ result }: { result: ContractCheck }) {
  const violations = result.findings.filter((finding) => finding.severity === "VIOLATION").length;
  if (violations > 0) {
    return (
      <div className="verdict verdict-blocked">
        <strong>Blocked</strong>
        <span>
          {violations} {violations === 1 ? "term conflicts" : "terms conflict"} with German freight law. No draft until
          the contract is renegotiated or a lawyer has looked at it.
        </span>
      </div>
    );
  }
  if (result.canDraft) {
    return (
      <div className="verdict verdict-ok">
        <strong>Can become a draft</strong>
        <span>
          {result.accepted.length} routing {result.accepted.length === 1 ? "rule" : "rules"} would go into a draft. A test
          run and a second person still decide before anything goes live.
        </span>
      </div>
    );
  }
  return (
    <div className="verdict verdict-neutral">
      <strong>Nothing to draft</strong>
      <span>
        No term breaks the rules Meridian checks, but no clause names postcodes and a courier service, so there is no
        routing rule to draft.
      </span>
    </div>
  );
}

export function ContractResult({ result }: { result: ContractCheck }) {
  const { terms } = result;
  const termRows = [
    terms.liabilityCap && ["Liability cap", `${terms.liabilityCap.sdrPerKilogram.toLocaleString("en-GB")} SDR per kg`],
    terms.hiddenDamageNotice && ["Report hidden damage within", duration(terms.hiddenDamageNotice.hours)],
    terms.delayNotice && ["Report late delivery within", duration(terms.delayNotice.hours)],
    terms.limitation && ["Claims expire after", `${terms.limitation.months} months`],
    terms.delayLiabilityExclusion && ["Late delivery", "liability excluded"],
    terms.heaviestCoveredGrams > 0 && ["Heaviest parcel covered", grams(terms.heaviestCoveredGrams)],
  ].filter(Boolean) as [string, string][];

  return (
    <div className="result" aria-live="polite">
      <Verdict result={result} />

      <h3 className="result-title">Legal findings</h3>
      {result.findings.length === 0 ? (
        <p className="muted">No term in this text conflicts with the rules Meridian checks.</p>
      ) : (
        <ul className="findings">
          {result.findings.map((finding) => (
            <li key={finding.ruleId} className={`finding finding-${finding.severity.toLowerCase()}`}>
              <div className="finding-head">
                <SeverityBadge severity={finding.severity} />
                <a href={finding.source} target="_blank" rel="noreferrer" className="law-ref">
                  {finding.lawReference}
                </a>
              </div>
              <p>{finding.message}</p>
              {finding.clause && <blockquote>{finding.clause}</blockquote>}
            </li>
          ))}
        </ul>
      )}

      <h3 className="result-title">Draft routing rules</h3>
      {result.accepted.length === 0 ? (
        <p className="muted">No clause could be turned into a routing rule.</p>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th scope="col">Postcodes</th>
                <th scope="col">Parcels</th>
                <th scope="col">Weight</th>
                <th scope="col">Courier service</th>
                <th scope="col">From the clause</th>
              </tr>
            </thead>
            <tbody>
              {result.accepted.map((rule) => (
                <tr key={rule.sourceQuote}>
                  <td className="nowrap">{rule.destinationPrefix}…</td>
                  <td>{rule.serviceLevel ? levelText[rule.serviceLevel] : "all"}</td>
                  <td>{weightBand(rule.weightBand)}</td>
                  <td className="nowrap">{carrierService(rule.carrierService)}</td>
                  <td className="quote-cell">{rule.sourceQuote}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {(result.unresolved.length > 0 || result.rejected.length > 0) && (
        <>
          <h3 className="result-title">Left for a person</h3>
          <ul className="handoff">
            {result.unresolved.map((item) => (
              <li key={item.clause.reference + item.clause.text}>
                <span className="ref">{item.clause.reference}</span>
                <span>
                  <span className="handoff-text">{item.clause.text}</span>
                  <span className="small muted">Not turned into a rule: it {item.reason}.</span>
                </span>
              </li>
            ))}
            {result.rejected.map((item) => (
              <li key={item.proposal.sourceQuote}>
                <span className="ref">rule</span>
                <span>
                  <span className="handoff-text">{item.proposal.sourceQuote}</span>
                  <span className="small muted">Read, but invalid: {item.reasons.join("; ")}.</span>
                </span>
              </li>
            ))}
          </ul>
        </>
      )}

      {termRows.length > 0 && (
        <>
          <h3 className="result-title">Terms Meridian read</h3>
          <dl className="terms">
            {termRows.map(([label, value]) => (
              <div key={label}>
                <dt>{label}</dt>
                <dd>{value}</dd>
              </div>
            ))}
          </dl>
        </>
      )}

      {result.unrecognised.length > 0 && (
        <p className="small muted">
          {result.unrecognised.length} {result.unrecognised.length === 1 ? "clause was" : "clauses were"} neither a
          routing clause nor a term Meridian checks, and {result.unrecognised.length === 1 ? "was" : "were"} left as
          they are.
        </p>
      )}
    </div>
  );
}
