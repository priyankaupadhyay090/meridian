import { Link } from "react-router-dom";

export function NotFoundPage() {
  return (
    <section className="page-hero">
      <div className="container narrow">
        <p className="eyebrow">404</p>
        <h1>This page does not exist</h1>
        <p className="lead">
          The link may be old. <Link to="/">Go to the start page</Link>.
        </p>
      </div>
    </section>
  );
}
