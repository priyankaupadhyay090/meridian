import { useEffect, useState } from "react";
import { Link, NavLink, Outlet, useLocation } from "react-router-dom";
import { useSession } from "../lib/session";
import { Logo } from "./Logo";

const links = [
  { to: "/compare", label: "Compare couriers" },
  { to: "/contract-check", label: "Contract check" },
  { to: "/library", label: "Library" },
  { to: "/rule-changes", label: "Rule changes" },
  { to: "/checkout", label: "Checkout" },
];

export function Layout() {
  const { pathname, hash } = useLocation();
  const [open, setOpen] = useState(false);
  const { account, signOut } = useSession();

  useEffect(() => {
    setOpen(false);
    if (!hash) {
      window.scrollTo(0, 0);
      return;
    }
    const timer = window.setTimeout(() => document.getElementById(hash.slice(1))?.scrollIntoView(), 50);
    return () => window.clearTimeout(timer);
  }, [pathname, hash]);

  return (
    <>
      <a className="skip" href="#main">
        Skip to content
      </a>
      <header className="site-header">
        <div className="container nav">
          <Logo />
          <nav className="nav-links" aria-label="Main">
            {links.map((link) => (
              <NavLink key={link.to} to={link.to}>
                {link.label}
              </NavLink>
            ))}
          </nav>
          <div className="nav-cta">
            {account ? (
              <>
                <Link to="/my-contracts" className="btn btn-outline btn-small">
                  My contracts
                </Link>
                <button type="button" className="btn btn-outline btn-small" onClick={signOut}>
                  Sign out
                </button>
              </>
            ) : (
              <Link to="/sign-in" className="btn btn-outline btn-small">
                Business sign-in
              </Link>
            )}
            <Link to="/contract-check#try" className="btn btn-primary btn-small">
              Check a contract
            </Link>
          </div>
          <button
            className="burger"
            aria-label={open ? "Close menu" : "Open menu"}
            aria-expanded={open}
            aria-controls="mobile-menu"
            onClick={() => setOpen(!open)}
          >
            <span />
            <span />
            <span />
          </button>
        </div>
        {open && (
          <nav id="mobile-menu" className="mobile-menu" aria-label="Main">
            {links.map((link) => (
              <NavLink key={link.to} to={link.to}>
                {link.label}
              </NavLink>
            ))}
            <Link to={account ? "/my-contracts" : "/sign-in"}>{account ? "My contracts" : "Business sign-in"}</Link>
            {account && (
              <button type="button" className="btn btn-outline" onClick={signOut}>
                Sign out
              </button>
            )}
            <Link to="/contract-check#try" className="btn btn-primary">
              Check a contract
            </Link>
          </nav>
        )}
      </header>
      <main id="main">
        <Outlet />
      </main>
      <footer className="site-footer">
        <div className="container footer-grid">
          <div>
            <Logo />
            <p className="muted footer-claim">
              Shipping for German online shops. Real courier prices, contracts checked against German law, routing
              rules changed only after a test run and a second approval.
            </p>
          </div>
          <div>
            <h2 className="footer-title">Platform</h2>
            <ul>
              {links.map((link) => (
                <li key={link.to}>
                  <Link to={link.to}>{link.label}</Link>
                </li>
              ))}
            </ul>
          </div>
          <div>
            <h2 className="footer-title">Code</h2>
            <ul>
              <li>
                <a href="https://github.com/priyankaupadhyay090/meridian-commerce" target="_blank" rel="noreferrer">
                  Source on GitHub
                </a>
              </li>
              <li>
                <Link to="/#status">What runs today</Link>
              </li>
              <li>
                <Link to="/contract-check#ai">Where AI is used</Link>
              </li>
            </ul>
          </div>
        </div>
        <div className="container footer-bottom">
          <span>Meridian is a portfolio project. The contract check is not legal advice.</span>
          <span>Privacy: public uploads are never stored; a business's contracts are visible only to that business.</span>
          <span>Courier prices from published price lists, each linked to its source.</span>
        </div>
      </footer>
    </>
  );
}
