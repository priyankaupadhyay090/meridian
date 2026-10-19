import type {
  ContractCheck,
  ContractCheckRequest,
  ContractExample,
  Courier,
  CourierRate,
  ExtractedDocument,
  BusinessAccount,
  ContractFileItem,
  DeliveryOption,
  Parcel,
  RuleChangeExample,
  RuleSet,
  TestRunReport,
} from "./types";

export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (typeof init.body === "string") {
    headers.set("Content-Type", "application/json");
  }
  const response = await fetch(path, { ...init, headers });
  if (!response.ok) {
    const problem = await response.json().catch(() => null);
    throw new ApiError(response.status, problem?.detail ?? problem?.title ?? `Request failed (${response.status})`);
  }
  return response.json() as Promise<T>;
}

function authorised(credentials: string, init: RequestInit = {}): RequestInit {
  const headers = new Headers(init.headers);
  headers.set("Authorization", `Basic ${credentials}`);
  return { ...init, headers };
}

export const business = {
  me: (credentials: string) => request<BusinessAccount>("/api/business/me", authorised(credentials)),
  contracts: (credentials: string) => request<ContractFileItem[]>("/api/business/contracts", authorised(credentials)),
  upload: (credentials: string, file: File, carrierName: string) => {
    const body = new FormData();
    body.append("file", file);
    if (carrierName) body.append("carrierName", carrierName);
    return request<ContractFileItem>("/api/business/contracts", authorised(credentials, { method: "POST", body }));
  },
  text: (credentials: string, id: string) =>
    request<{ fileName: string; carrierName: string; text: string }>(
      `/api/business/contracts/${encodeURIComponent(id)}/text`,
      authorised(credentials),
    ),
  download: async (credentials: string, id: string) => {
    const response = await fetch(`/api/business/contracts/${encodeURIComponent(id)}/file`, authorised(credentials));
    if (!response.ok) throw new ApiError(response.status, `Download failed (${response.status})`);
    return response.blob();
  },
  remove: async (credentials: string, id: string) => {
    const response = await fetch(`/api/business/contracts/${encodeURIComponent(id)}`, authorised(credentials, { method: "DELETE" }));
    if (!response.ok) throw new ApiError(response.status, `Delete failed (${response.status})`);
  },
};

const post = <T>(path: string, body: unknown) => request<T>(path, { method: "POST", body: JSON.stringify(body) });

export const api = {
  couriers: () => request<CourierRate[]>("/api/public/couriers"),
  quote: (parcel: Parcel) => post<DeliveryOption[]>("/api/public/quotes", parcel),
  checkoutOptions: (parcel: Parcel, shopDefault: Courier | null, shopShippingCents: number, couriers: Courier[]) =>
    post<DeliveryOption[]>("/api/public/checkout-options", { parcel, shopDefault, shopShippingCents, couriers }),
  contractExamples: () => request<ContractExample[]>("/api/public/contract-checks/examples"),
  checkContract: (contract: ContractCheckRequest) => post<ContractCheck>("/api/public/contract-checks", contract),
  readDocument: (file: File) => {
    const body = new FormData();
    body.append("file", file);
    return request<ExtractedDocument>("/api/public/contract-checks/documents", { method: "POST", body });
  },
  rules: () => request<RuleSet>("/api/public/rules"),
  ruleChangeExamples: () => request<RuleChangeExample[]>("/api/public/rule-changes/examples"),
  testRun: (change: Pick<RuleChangeExample, "remove" | "add">) =>
    post<TestRunReport>("/api/public/rule-changes/test-run", { remove: change.remove, add: change.add }),
};
