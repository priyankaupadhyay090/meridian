import { createContext, useContext, useState, type ReactNode } from "react";
import { business } from "./api";
import type { BusinessAccount } from "./types";

const KEY = "meridian.business";

interface Session {
  account: BusinessAccount | null;
  credentials: string | null;
  signIn: (user: string, password: string) => Promise<void>;
  signOut: () => void;
}

const SessionContext = createContext<Session | null>(null);

function stored(): { account: BusinessAccount; credentials: string } | null {
  try {
    const value = sessionStorage.getItem(KEY);
    return value ? JSON.parse(value) : null;
  } catch {
    return null;
  }
}

export function SessionProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState(stored);

  async function signIn(user: string, password: string) {
    const credentials = btoa(`${user}:${password}`);
    const account = await business.me(credentials);
    const next = { account, credentials };
    try {
      sessionStorage.setItem(KEY, JSON.stringify(next));
    } catch {
      return setState(next);
    }
    setState(next);
  }

  function signOut() {
    try {
      sessionStorage.removeItem(KEY);
    } catch {
      return setState(null);
    }
    setState(null);
  }

  return (
    <SessionContext.Provider value={{ account: state?.account ?? null, credentials: state?.credentials ?? null, signIn, signOut }}>
      {children}
    </SessionContext.Provider>
  );
}

export function useSession() {
  const session = useContext(SessionContext);
  if (!session) throw new Error("useSession outside SessionProvider");
  return session;
}
