import { Link } from "react-router-dom";
import { LiveRules, RuleSandbox } from "../components/RuleSandbox";

const steps = [
  {
    title: "Draft",
    who: "Operator",
    text: "An operator writes the new rule set, or a contract check creates the draft from a courier's contract. The rules are validated first: a rule without a valid postcode prefix, with a broken weight band or with a priority out of range is refused.",
  },
  {
    title: "Test run",
    who: "Operator",
    text: "The draft is run against recent parcels next to the live rules. Meridian counts how many parcels would switch courier, lists examples, and finds parcels that no rule would route any more.",
  },
  {
    title: "Submit",
    who: "Operator",
    text: "Only a draft with a passing test run can be submitted. Two rules that match the same parcels with the same priority count as a conflict and fail the run.",
  },
  {
    title: "Second approval",
    who: "Approver",
    text: "A different person approves or rejects it with a comment. The author cannot approve their own change, whatever their role.",
  },
  {
    title: "Live at once",
    who: "System",
    text: "The new rule set replaces the old one in one step, never half. If someone else changed the live rules in the meantime, activation is refused and the change has to be drafted again on the current rules.",
  },
  {
    title: "Roll back",
    who: "Approver",
    text: "The previous rule set can be brought back in one step. Every step, from draft to rollback, is written to the audit log with who did it and when.",
  },
];

const principles = [
  {
    title: "Four eyes",
    text: "The person who writes a change never approves it. This is the same control banks use for configuration that moves money.",
  },
  {
    title: "See the effect before it happens",
    text: "A wrong rule can send thousands of parcels to a courier that is too expensive or cannot take their size. The test run shows that on recent parcels first.",
  },
  {
    title: "All or nothing",
    text: "A rule set is versioned. Activation swaps the whole version, so a parcel is never routed by a mix of old and new rules.",
  },
  {
    title: "Roles, not trust",
    text: "Viewers read, operators draft and test, approvers decide. The API checks the role on every call.",
  },
];

export function RuleChangesPage() {
  return (
    <>
      <section className="page-hero">
        <div className="container narrow">
          <p className="eyebrow">Rule changes</p>
          <h1>Routing rules change the way a bank changes its configuration</h1>
          <p className="lead">
            Routing rules decide which courier takes which parcel, by postcode, weight, service level and customer.
            Changing them is risky, so every change gets a test run on recent parcels and a second person's approval
            before it goes live.
          </p>
        </div>
      </section>

      <section className="section band" id="try">
        <div className="container">
          <div className="section-head">
            <p className="eyebrow">Try it</p>
            <h2>Run a test run on sample changes</h2>
            <p className="lead">
              Pick a change and run it against the live rules on 120 sample shipments. You see what an approver sees:
              how many parcels would switch courier, which ones, and whether any parcel is left without a courier.
            </p>
          </div>
          <RuleSandbox />
          <LiveRules />
        </div>
      </section>

      <section className="section">
        <div className="container">
          <div className="section-head">
            <p className="eyebrow">How it works</p>
            <h2>Six steps from draft to live</h2>
          </div>
          <ol className="steps">
            {steps.map((step, index) => (
              <li key={step.title}>
                <span className="step-number">{index + 1}</span>
                <h3>
                  {step.title} <span className="who">{step.who}</span>
                </h3>
                <p className="muted">{step.text}</p>
              </li>
            ))}
          </ol>
        </div>
      </section>

      <section className="section band">
        <div className="container">
          <div className="section-head">
            <p className="eyebrow">Principles</p>
            <h2>Why it works this way</h2>
          </div>
          <div className="principles two">
            {principles.map((principle) => (
              <div key={principle.title}>
                <h3>{principle.title}</h3>
                <p className="muted">{principle.text}</p>
              </div>
            ))}
          </div>
          <p className="small muted fine-print">
            The full flow runs today in the console API with four demo users (two operators, one approver, one viewer).
            The console screens are planned. New rules often start as a <Link to="/contract-check">contract check</Link>.
          </p>
        </div>
      </section>
    </>
  );
}
