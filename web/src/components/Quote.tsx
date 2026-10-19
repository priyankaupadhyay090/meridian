import { useEffect, useState } from "react";
import { api } from "../lib/api";
import type { DeliveryOption, Parcel } from "../lib/types";
import { DeliveryChooser, optionKey } from "./DeliveryChooser";

export function useQuote(parcel: Parcel) {
  const [options, setOptions] = useState<DeliveryOption[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setError(null);
    api
      .quote(parcel)
      .then((result) => active && setOptions(result))
      .catch((failure: Error) => active && setError(failure.message));
    return () => {
      active = false;
    };
  }, [parcel]);

  return { options, error };
}

export function QuoteResult({ options, error, name }: { options: DeliveryOption[] | null; error: string | null; name: string }) {
  const [selected, setSelected] = useState("");

  useEffect(() => {
    setSelected(options?.length ? optionKey(options[0]) : "");
  }, [options]);

  if (error) return <p className="notice notice-bad">{error}</p>;
  if (!options) return <p className="muted">Loading prices…</p>;
  if (!options.length) {
    return <p className="notice">No courier in the price list takes a parcel this heavy or this large.</p>;
  }
  return <DeliveryChooser name={name} legend="Couriers that take this parcel" options={options} selected={selected} onSelect={setSelected} />;
}
