import { useEffect, useRef, useState } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import { business } from "../lib/api";
import { date } from "../lib/format";
import { useSession } from "../lib/session";
import type { ContractFileItem } from "../lib/types";

const accept = ".pdf,.docx,.odt,.txt";

function size(bytes: number) {
  return bytes >= 1024 * 1024 ? `${(bytes / 1024 / 1024).toFixed(1)} MB` : `${Math.max(1, Math.round(bytes / 1024))} KB`;
}

export function MyContractsPage() {
  const { account, credentials } = useSession();
  const navigate = useNavigate();
  const input = useRef<HTMLInputElement>(null);
  const [files, setFiles] = useState<ContractFileItem[] | null>(null);
  const [carrier, setCarrier] = useState("");
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function reload() {
    if (!credentials) return;
    try {
      setFiles(await business.contracts(credentials));
    } catch (failure) {
      setError((failure as Error).message);
    }
  }

  useEffect(() => {
    reload();
  }, [credentials]);

  if (!account || !credentials) return <Navigate to="/sign-in" replace />;

  async function upload(file: File | undefined) {
    if (!file || !credentials) return;
    setBusy("upload");
    setError(null);
    try {
      await business.upload(credentials, file, carrier.trim());
      await reload();
    } catch (failure) {
      setError((failure as Error).message);
    } finally {
      setBusy(null);
      if (input.current) input.current.value = "";
    }
  }

  async function download(file: ContractFileItem) {
    if (!credentials) return;
    const blob = await business.download(credentials, file.id);
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = file.fileName;
    link.click();
    URL.revokeObjectURL(url);
  }

  async function check(file: ContractFileItem) {
    if (!credentials) return;
    setBusy(file.id);
    try {
      navigate("/contract-check#try", { state: await business.text(credentials, file.id) });
    } catch (failure) {
      setError((failure as Error).message);
      setBusy(null);
    }
  }

  async function remove(file: ContractFileItem) {
    if (!credentials) return;
    setBusy(file.id);
    try {
      await business.remove(credentials, file.id);
      await reload();
    } finally {
      setBusy(null);
    }
  }

  return (
    <>
      <section className="page-hero">
        <div className="container narrow">
          <p className="eyebrow">My contracts</p>
          <h1>{account.businessName}</h1>
          <p className="lead">Your courier contracts, kept in private storage for your business.</p>
        </div>
      </section>

      <section className="section-tight">
        <div className="container my-contracts">
          <aside className="panel privacy" aria-label="Who can see these files">
            <h2 className="h3">Who can see these files</h2>
            <ul>
              <li>
                <strong>Your business only.</strong> Other businesses on Meridian cannot see, open or even find them.
              </li>
              <li>
                <strong>Meridian's internal team</strong> can open them read-only for support. Every time they do, it is
                written to the audit log.
              </li>
              <li>
                <strong>Delete a file</strong> and it is removed from storage.
              </li>
            </ul>
          </aside>

          <div className="panel upload-panel">
            <h2 className="h3">Upload a contract</h2>
            <label className="field">
              <span className="small muted">Courier (optional)</span>
              <input value={carrier} maxLength={120} onChange={(event) => setCarrier(event.target.value)} />
            </label>
            <label className="upload">
              <input
                ref={input}
                type="file"
                accept={accept}
                className="visually-hidden"
                onChange={(event) => upload(event.target.files?.[0])}
              />
              <span className="upload-title">{busy === "upload" ? "Uploading…" : "Choose a contract file"}</span>
              <span className="small muted">PDF, Word, OpenDocument or text, up to 5 MB.</span>
            </label>
            {error && (
              <p className="notice notice-bad" role="alert">
                {error}
              </p>
            )}
          </div>

          <div className="files">
            <h2 className="h3">Your files</h2>
            {files && files.length === 0 && <p className="muted">No contracts yet.</p>}
            <ul className="library">
              {files?.map((file) => (
                <li key={file.id} className="panel library-item">
                  <div>
                    <h3 className="h3">{file.fileName}</h3>
                    <p className="small muted">
                      {[file.carrierName, file.format, size(file.sizeBytes), `uploaded by ${file.uploadedBy}`, date(file.uploadedAt.slice(0, 10))]
                        .filter(Boolean)
                        .join(" · ")}
                    </p>
                  </div>
                  <div className="library-actions">
                    <button type="button" className="btn btn-outline btn-small" onClick={() => download(file)}>
                      Download
                    </button>
                    <button type="button" className="btn btn-primary btn-small" onClick={() => check(file)} disabled={busy === file.id}>
                      Check this contract
                    </button>
                    <button type="button" className="btn btn-outline btn-small" onClick={() => remove(file)} disabled={busy === file.id}>
                      Delete
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </section>
    </>
  );
}
