import { useRef, useState, type DragEvent } from "react";
import { Link } from "react-router-dom";
import { api } from "../lib/api";
import type { ExtractedDocument } from "../lib/types";

const accept = ".pdf,.docx,.odt,.txt,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.oasis.opendocument.text,text/plain";
const maxBytes = 5 * 1024 * 1024;

export function ContractUpload({ onRead }: { onRead: (document: ExtractedDocument) => void }) {
  const input = useRef<HTMLInputElement>(null);
  const [reading, setReading] = useState(false);
  const [dragging, setDragging] = useState(false);
  const [read, setRead] = useState<ExtractedDocument | null>(null);
  const [error, setError] = useState<string | null>(null);


  async function upload(file: File | undefined) {
    if (!file) return;
    setError(null);
    setRead(null);
    if (file.size > maxBytes) {
      setError("The file is larger than 5 MB.");
      return;
    }
    setReading(true);
    try {
      const document = await api.readDocument(file);
      setRead(document);
      onRead(document);

    } catch (failure) {
      setError((failure as Error).message);
    } finally {
      setReading(false);
      if (input.current) input.current.value = "";
    }
  }

  function drop(event: DragEvent) {
    event.preventDefault();
    setDragging(false);
    upload(event.dataTransfer.files[0]);
  }

  return (
    <div className="upload-block">
      <label
        className={`upload${dragging ? " is-dragging" : ""}`}
        onDragOver={(event) => {
          event.preventDefault();
          setDragging(true);
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={drop}
      >
        <input
          ref={input}
          type="file"
          accept={accept}
          className="visually-hidden"
          onChange={(event) => upload(event.target.files?.[0])}
        />
        <span className="upload-title">{reading ? "Reading the file…" : "Upload a contract file"}</span>
        <span className="small muted">PDF, Word (.docx), OpenDocument (.odt) or text, up to 5 MB. Drop it here or click.</span>
      </label>
      <p className="small privacy-line">
        Nothing you upload here is stored: the file is read once and forgotten. To keep contracts, use{" "}
        <Link to="/my-contracts">My contracts</Link>.
      </p>
      {read && (
        <p className="notice" role="status">
          Read {read.characters.toLocaleString("en-GB")} characters from <strong>{read.fileName}</strong> ({read.format}).
          Check the text below, then press Check contract.
        </p>
      )}
      {error && (
        <p className="notice notice-bad" role="alert">
          {error}
        </p>
      )}
      <p className="small muted">
        No file at hand? Download a sample:{" "}
        <a href="/samples/sample-contract.pdf" download>
          PDF
        </a>
        ,{" "}
        <a href="/samples/sample-contract.docx" download>
          Word
        </a>
        , German{" "}
        <a href="/samples/muster-vertrag.pdf" download>
          PDF
        </a>
        ,{" "}
        <a href="/samples/muster-vertrag.docx" download>
          Word
        </a>
        .
      </p>
    </div>
  );
}
