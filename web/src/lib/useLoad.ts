import { useEffect, useState } from "react";

export interface Loaded<T> {
  data: T | null;
  error: string | null;
  loading: boolean;
}

export function useLoad<T>(load: () => Promise<T>, deps: unknown[]): Loaded<T> {
  const [state, setState] = useState<Loaded<T>>({ data: null, error: null, loading: true });

  useEffect(() => {
    let active = true;
    setState((previous) => ({ ...previous, loading: true, error: null }));
    load()
      .then((data) => active && setState({ data, error: null, loading: false }))
      .catch((error: Error) => active && setState({ data: null, error: error.message, loading: false }));
    return () => {
      active = false;
    };
  }, deps);

  return state;
}
