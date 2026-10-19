import { useEffect, useState } from "react";
import { api } from "../lib/api";
import { carrierService, weightBand } from "../lib/format";
import type { RoutingRule, TestRunReport } from "../lib/types";
import { useLoad } from "../lib/useLoad";

const levelText = { ECONOMY: "economy", STANDARD: "standard", EXPRESS: "express" } as const;

export function ruleText(rule: RoutingRule) {
  const parts = [`${rule.match.destinationPrefix}…`];
  if (rule.match.serviceLevel) parts.push(levelText[rule.match.serviceLevel]);
  if (rule.match.weightBand) parts.push(weightBand(rule.match.weightBand));
  return `${parts.join(", ")} → ${carrierService(rule.carrierService)}`;
}

function service(value: string | null) {
  return value ? carrierService(value) : "no courier";
}

export function RuleSandbox() {
  const examples = useLoad(api.ruleChangeExamples, []);
  const rules = useLoad(api.rules, []);
  const [selectedId, setSelectedId] = useState("");
  const [report, setReport] = useState<TestRunReport | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [running, setRunning] = useState(false);

  useEffect(() => {
    if (examples.data?.length && !selectedId) setSelectedId(examples.data[0].id);
  }, [examples.data]);

  const selected = examples.data?.find((example) => example.id === selectedId);

  function choose(id: string) {
    setSelectedId(id);
    setReport(null);
    setError(null);
  }

  async function run() {
    if (!selected) return;
    setRunning(true);
    setError(null);
    setReport(null);
    try {
      setReport(await api.testRun(selected));
    } catch (failure) {
      setError((failure as Error).message);
    } finally {
      setRunning(false);
    }
  }

  return (
    <div className="try-grid">
      <div className="panel try-form">
        {examples.data && (
          <div className="presets" role="group" aria-label="Sample changes">
            {examples.data.map((example) => (
              <button
                key={example.id}
                type="button"
                className={`chip${selectedId === example.id ? " is-active" : ""}`}
                onClick={() => choose(example.id)}
              >
                {example.title}
              </button>
            ))}
          </div>
        )}
        {examples.error && <p className="notice notice-bad">{examples.error}</p>}
        {selected && (
          <>
            <p>{selected.description}</p>
            <ul className="change-list">
              {selected.add.map((rule) => (
                <li key={rule.id}>
                  <span className="change-mark add">Add</span>
                  <span>
                    {ruleText(rule)} <span className="small muted">priority {rule.priority}</span>
                  </span>
                </li>
              ))}
              {selected.remove.map((id) => {
                const rule = rules.data?.rules.find((candidate) => candidate.id === id);
                return (
                  <li key={id}>
                    <span className="change-mark remove">Remove</span>
                    <span>{rule ? ruleText(rule) : id}</span>
                  </li>
                );
              })}
            </ul>
            <button type="button" className="btn btn-primary" onClick={run} disabled={running}>
              {running ? "Running…" : "Run the test run"}
            </button>
          </>
        )}
        <p className="small muted">
          Sample changes and 120 sample shipments, written for this demo. The test run never changes the live rules.
        </p>
      </div>

      <div className="panel try-result" aria-live="polite">
        {error && (
          <div className="result">
            <div className="verdict verdict-blocked">
              <strong>Refused</strong>
              <span>{error.replace("the rules are not valid: ", "The rules are not valid: ")}.</span>
            </div>
            <p className="muted">Invalid rules never reach a test run, so nothing could be compared.</p>
          </div>
        )}
        {report && (
          <div className="result">
            <div className={`verdict ${report.passed && report.unrouted === 0 ? "verdict-ok" : "verdict-blocked"}`}>
              <strong>{!report.passed ? "Failed" : report.unrouted > 0 ? "Passed, with a warning" : "Passed"}</strong>
              <span>
                {!report.passed
                  ? "Two rules clash. The change cannot be submitted."
                  : report.unrouted > 0
                    ? "The rules are consistent, but some parcels would have no courier. An approver should reject this."
                    : "Can be submitted. A second person still has to approve it."}
              </span>
            </div>
            <dl className="report-numbers">
              <div>
                <dt>Parcels checked</dt>
                <dd>{report.evaluated}</dd>
              </div>
              <div>
                <dt>Would switch courier</dt>
                <dd>{report.changed}</dd>
              </div>
              <div>
                <dt>No courier any more</dt>
                <dd>{report.unrouted}</dd>
              </div>
              <div>
                <dt>Conflicts</dt>
                <dd>{report.conflicts.length}</dd>
              </div>
            </dl>
            {report.conflicts.map((conflict) => (
              <p key={conflict.firstRuleId + conflict.secondRuleId} className="notice notice-bad">
                {conflict.message}.
              </p>
            ))}
            {report.examples.length > 0 && (
              <>
                <h3 className="result-title">Examples of parcels that move</h3>
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th scope="col">Shipment</th>
                        <th scope="col">Before</th>
                        <th scope="col">After</th>
                      </tr>
                    </thead>
                    <tbody>
                      {report.examples.map((difference) => (
                        <tr key={difference.shipmentId}>
                          <td>{difference.shipmentId}</td>
                          <td>{service(difference.before)}</td>
                          <td className={difference.after ? "" : "text-bad"}>{service(difference.after)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </>
            )}
          </div>
        )}
        {!report && !error && (
          <div className="empty">
            <h3>The report shows up here</h3>
            <p className="muted">How many parcels would switch courier, which ones, and whether any parcel is left without a courier.</p>
          </div>
        )}
      </div>
    </div>
  );
}

export function LiveRules() {
  const rules = useLoad(api.rules, []);
  if (!rules.data) return null;
  return (
    <details className="panel live-rules">
      <summary>
        Live rules: version {rules.data.version}, {rules.data.rules.length} rules
      </summary>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th scope="col">Rule</th>
              <th scope="col">Parcels</th>
              <th scope="col" className="num">
                Priority
              </th>
            </tr>
          </thead>
          <tbody>
            {rules.data.rules.map((rule) => (
              <tr key={rule.id}>
                <td className="nowrap">{rule.id}</td>
                <td>{ruleText(rule)}</td>
                <td className="num">{rule.priority}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </details>
  );
}
