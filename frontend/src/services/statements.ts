import { apiClient, getApiErrorDetails } from "./api";

export type StatementGenerationMode = "STANDARD" | "CORRECTION";

export type StatementListItem = {
  statementId: string;
  accountId: string;
  periodYearMonth: string;
  artifactVersion: number;
  status: "GENERATED" | "CORRECTED" | "FAILED";
  generatedAtUtc: string;
};

export type StatementListResult = {
  items: StatementListItem[];
  page: number;
  pageSize: number;
  totalItems: number;
  totalPages: number;
};

export type StatementDetail = {
  statementId: string;
  accountId: string;
  periodYearMonth: string;
  artifactVersion: number;
  openingBalance: number;
  closingBalance: number;
  currencyCode: string;
  status: "GENERATED" | "CORRECTED" | "FAILED";
  artifactUri: string;
  generatedAtUtc: string;
};

export type GenerateStatementInput = {
  accountId: string;
  periodYearMonth: string;
  generationMode: StatementGenerationMode;
};

export type GenerateStatementResult = {
  statementId: string;
  generationStatus: "QUEUED" | "PROCESSING";
};

export async function fetchStatements(params: {
  accountId: string;
  periodYearMonth?: string;
  page?: number;
  pageSize?: number;
}): Promise<StatementListResult> {
  if (!params.accountId || params.accountId.trim().length === 0) {
    throw new Error("Select an account to load statements.");
  }

  try {
    const response = await apiClient.get("/statements", {
      params: {
        accountId: params.accountId.trim(),
        periodYearMonth: params.periodYearMonth?.trim() || undefined,
        page: Math.max(1, Math.trunc(params.page ?? 1)),
        pageSize: Math.max(1, Math.min(Math.trunc(params.pageSize ?? 20), 100))
      }
    });

    return mapStatementList(response.data);
  } catch (error) {
    const details = getApiErrorDetails(error);
    if (details.status === 403) {
      throw new Error("This signed-in account is not authorized to view statements for the selected account.");
    }
    if (details.status === 404) {
      throw new Error("The selected account or statements could not be found.");
    }
    throw new Error(details.message);
  }
}

export async function fetchStatement(statementId: string): Promise<StatementDetail> {
  if (!statementId || statementId.trim().length === 0) {
    throw new Error("Select a valid statement to retrieve.");
  }

  try {
    const response = await apiClient.get(`/statements/${encodeURIComponent(statementId.trim())}`);
    return mapStatementDetail(response.data);
  } catch (error) {
    const details = getApiErrorDetails(error);
    if (details.status === 403) {
      throw new Error("This signed-in account is not authorized to access the selected statement.");
    }
    if (details.status === 404) {
      throw new Error("The selected statement could not be found.");
    }
    throw new Error(details.message);
  }
}

export async function generateStatement(input: GenerateStatementInput): Promise<GenerateStatementResult> {
  if (!input.accountId || input.accountId.trim().length === 0) {
    throw new Error("Select an account before generating a statement.");
  }
  if (!input.periodYearMonth || input.periodYearMonth.trim().length === 0) {
    throw new Error("Select a statement period before generating.");
  }

  try {
    const response = await apiClient.post("/statements/generate", {
      accountId: input.accountId.trim(),
      periodYearMonth: input.periodYearMonth.trim(),
      generationMode: input.generationMode
    });

    const data = response.data as Record<string, unknown>;
    return {
      statementId: asString(data.statementId, ""),
      generationStatus: asString(data.generationStatus, "PROCESSING") === "QUEUED" ? "QUEUED" : "PROCESSING"
    };
  } catch (error) {
    const details = getApiErrorDetails(error);
    if (details.status === 403) {
      throw new Error("This signed-in account is not authorized to generate statements for the selected account.");
    }
    if (details.status === 404) {
      throw new Error("The selected account could not be found.");
    }
    throw new Error(details.message);
  }
}

function mapStatementList(payload: unknown): StatementListResult {
  if (!payload || typeof payload !== "object") {
    return {
      items: [],
      page: 1,
      pageSize: 20,
      totalItems: 0,
      totalPages: 1
    };
  }

  const data = payload as Record<string, unknown>;
  const items = Array.isArray(data.items)
    ? data.items
        .map((item) => mapStatementListItem(item))
        .filter((item): item is StatementListItem => item !== null)
    : [];

  return {
    items,
    page: Math.max(1, asNumber(data.page, 1)),
    pageSize: Math.max(1, asNumber(data.pageSize, 20)),
    totalItems: Math.max(0, asNumber(data.totalItems, 0)),
    totalPages: Math.max(1, asNumber(data.totalPages, 1))
  };
}

function mapStatementListItem(payload: unknown): StatementListItem | null {
  if (!payload || typeof payload !== "object") {
    return null;
  }

  const data = payload as Record<string, unknown>;
  const statementId = asString(data.statementId, "");
  if (!statementId) {
    return null;
  }

  return {
    statementId,
    accountId: asString(data.accountId, ""),
    periodYearMonth: asString(data.periodYearMonth, ""),
    artifactVersion: Math.max(1, asNumber(data.artifactVersion, 1)),
    status: asStatementStatus(data.status),
    generatedAtUtc: asString(data.generatedAtUtc, new Date().toISOString())
  };
}

function mapStatementDetail(payload: unknown): StatementDetail {
  if (!payload || typeof payload !== "object") {
    throw new Error("Statement payload is invalid");
  }

  const data = payload as Record<string, unknown>;
  const statementId = asString(data.statementId, "");
  if (!statementId) {
    throw new Error("Statement payload is invalid");
  }

  return {
    statementId,
    accountId: asString(data.accountId, ""),
    periodYearMonth: asString(data.periodYearMonth, ""),
    artifactVersion: Math.max(1, asNumber(data.artifactVersion, 1)),
    openingBalance: asNumber(data.openingBalance, 0),
    closingBalance: asNumber(data.closingBalance, 0),
    currencyCode: asString(data.currencyCode, "USD"),
    status: asStatementStatus(data.status),
    artifactUri: asString(data.artifactUri, ""),
    generatedAtUtc: asString(data.generatedAtUtc, new Date().toISOString())
  };
}

function asStatementStatus(value: unknown): StatementListItem["status"] {
  const normalized = asString(value, "GENERATED");
  if (normalized === "FAILED") {
    return "FAILED";
  }
  if (normalized === "CORRECTED") {
    return "CORRECTED";
  }
  return "GENERATED";
}

function asString(value: unknown, fallback: string): string {
  return typeof value === "string" && value.trim().length > 0 ? value : fallback;
}

function asNumber(value: unknown, fallback: number): number {
  if (typeof value === "number") {
    return value;
  }
  if (typeof value === "string") {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : fallback;
  }
  return fallback;
}
