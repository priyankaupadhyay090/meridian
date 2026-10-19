import { useState, type FormEvent } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import { useSession } from "../lib/session";

export function SignInPage() {
  const { account, signIn } = useSession();
  const navigate = useNavigate();
  const [user, setUser] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (account) return <Navigate to="/my-contracts" replace />;

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await signIn(user.trim(), password);
      navigate("/my-contracts");
    } catch {
      setError("That user name and password do not match a business account.");
      setBusy(false);
    }
  }

  return (
    <section className="page-hero">
      <div className="container narrow">
        <p className="eyebrow">Business sign-in</p>
        <h1>Sign in to your contracts</h1>
        <p className="lead">Each business sees only its own contracts.</p>
        <form className="panel sign-in" onSubmit={submit}>
          <label className="field">
            <span className="small muted">User name</span>
            <input value={user} autoComplete="username" required onChange={(event) => setUser(event.target.value)} />
          </label>
          <label className="field">
            <span className="small muted">Password</span>
            <input
              type="password"
              value={password}
              autoComplete="current-password"
              required
              onChange={(event) => setPassword(event.target.value)}
            />
          </label>
          <button type="submit" className="btn btn-primary" disabled={busy}>
            {busy ? "Signing in…" : "Sign in"}
          </button>
          {error && (
            <p className="notice notice-bad" role="alert">
              {error}
            </p>
          )}
          <p className="small muted">
            Demo accounts: <code>nordlicht</code> or <code>weserkontor</code>, password <code>demo</code>. Two sample
            businesses, so you can see that one cannot see the other's files.
          </p>
        </form>
      </div>
    </section>
  );
}
