import { beforeEach, describe, expect, it, jest } from "@jest/globals";
import { apiClient } from "./api";
import * as transactions from "./transactions";
import { fetchStatement, fetchStatementPdf, fetchStatementTransactions, fetchStatements, generateStatement } from "./statements";

describe("statements service", () => {
  beforeEach(() => {
    jest.restoreAllMocks();
  });

  it("maps statement list response", async () => {
    jest.spyOn(apiClient, "get").mockResolvedValue({
      data: {
        items: [
          {
            statementId: "stmt-1",
            accountId: "acc-1",
            periodYearMonth: "2026-06",
            artifactVersion: 2,
            status: "CORRECTED",
            generatedAtUtc: "2026-06-30T00:05:00Z"
          }
        ],
        page: 1,
        pageSize: 20,
        totalItems: 1,
        totalPages: 1
      }
    } as never);

    const result = await fetchStatements({
      accountId: "acc-1",
      periodYearMonth: "2026-06",
      page: 1,
      pageSize: 20
    });

    expect(result.items).toHaveLength(1);
    expect(result.items[0]).toMatchObject({
      statementId: "stmt-1",
      status: "CORRECTED"
    });
    expect(result.totalItems).toBe(1);
  });

  it("returns user-friendly message when list access is forbidden", async () => {
    jest.spyOn(apiClient, "get").mockRejectedValue({
      response: {
        status: 403,
        data: {
          code: "STATEMENT_FORBIDDEN",
          message: "Forbidden"
        }
      }
    });

    await expect(fetchStatements({ accountId: "acc-1" })).rejects.toThrow(
      "This signed-in account is not authorized to view statements for the selected account."
    );
  });

  it("maps statement detail payload", async () => {
    jest.spyOn(apiClient, "get").mockResolvedValue({
      data: {
        statementId: "stmt-2",
        accountId: "acc-2",
        periodYearMonth: "2026-05",
        artifactVersion: 1,
        openingBalance: "15.25",
        closingBalance: "40.50",
        currencyCode: "USD",
        status: "GENERATED",
        artifactUri: "/statements/stmt-2/artifact/v1.pdf",
        generatedAtUtc: "2026-06-01T00:05:00Z"
      }
    } as never);

    const result = await fetchStatement("stmt-2");

    expect(result.statementId).toBe("stmt-2");
    expect(result.openingBalance).toBeCloseTo(15.25);
    expect(result.closingBalance).toBeCloseTo(40.5);
  });

  it("posts generate request and maps accepted response", async () => {
    const postMock = jest.spyOn(apiClient, "post").mockResolvedValue({
      data: {
        statementId: "stmt-3",
        generationStatus: "PROCESSING"
      }
    } as never);

    const result = await generateStatement({
      accountId: "acc-3",
      periodYearMonth: "2026-06",
      generationMode: "STANDARD"
    });

    expect(postMock).toHaveBeenCalledWith("/statements/generate", {
      accountId: "acc-3",
      periodYearMonth: "2026-06",
      generationMode: "STANDARD"
    });
    expect(result).toEqual({
      statementId: "stmt-3",
      generationStatus: "PROCESSING"
    });
  });

  it("collects all statement-period transactions across pages", async () => {
    const historySpy = jest.spyOn(transactions, "fetchTransactionHistory")
      .mockResolvedValueOnce({
        items: [
          {
            transactionId: "txn-1",
            transactionType: "DEPOSIT",
            bookedAt: "2026-06-10T12:00:00Z",
            description: "Deposit",
            category: "Deposit",
            amount: 50,
            currency: "USD",
            direction: "CREDIT",
            status: "Completed"
          }
        ],
        page: 1,
        pageSize: 100,
        totalItems: 2,
        totalPages: 2
      })
      .mockResolvedValueOnce({
        items: [
          {
            transactionId: "txn-2",
            transactionType: "TRANSFER_DEBIT",
            bookedAt: "2026-06-05T09:30:00Z",
            description: "Transfer sent",
            category: "Transfer",
            amount: 20,
            currency: "USD",
            direction: "DEBIT",
            status: "Completed"
          }
        ],
        page: 2,
        pageSize: 100,
        totalItems: 2,
        totalPages: 2
      });

    const items = await fetchStatementTransactions({
      accountId: "acc-4",
      periodYearMonth: "2026-06"
    });

    expect(historySpy).toHaveBeenNthCalledWith(1, {
      scopeType: "ACCOUNT",
      scopeId: "acc-4",
      startDate: "2026-06-01",
      endDate: "2026-06-30",
      page: 1,
      pageSize: 100
    });

    expect(historySpy).toHaveBeenNthCalledWith(2, {
      scopeType: "ACCOUNT",
      scopeId: "acc-4",
      startDate: "2026-06-01",
      endDate: "2026-06-30",
      page: 2,
      pageSize: 100
    });

    expect(items).toHaveLength(2);
    expect(items[0].transactionId).toBe("txn-1");
    expect(items[1].transactionId).toBe("txn-2");
  });

  it("rejects invalid statement period format", async () => {
    await expect(
      fetchStatementTransactions({
        accountId: "acc-4",
        periodYearMonth: "2026/06"
      })
    ).rejects.toThrow("Statement period is invalid. Expected YYYY-MM.");
  });

  it("downloads statement PDF artifact metadata and blob", async () => {
    const getMock = jest.spyOn(apiClient, "get").mockResolvedValue({
      data: new Blob(["%PDF-1.4"], { type: "application/pdf" }),
      headers: {
        "content-disposition": "attachment; filename=\"statement-2026-06-v1.pdf\""
      }
    } as never);

    const result = await fetchStatementPdf({
      statementId: "stmt-2",
      artifactVersion: 1,
      artifactUri: "/statements/stmt-2/artifact/v1.pdf",
      periodYearMonth: "2026-06"
    });

    expect(getMock).toHaveBeenCalledWith("/statements/stmt-2/artifact/v1.pdf", {
      responseType: "blob"
    });
    expect(result.fileName).toBe("statement-2026-06-v1.pdf");
    expect(result.blob).toBeInstanceOf(Blob);
  });

  it("builds fallback artifact path when artifactUri is missing", async () => {
    const getMock = jest.spyOn(apiClient, "get").mockResolvedValue({
      data: new Blob(["%PDF-1.4"], { type: "application/pdf" }),
      headers: {}
    } as never);

    const result = await fetchStatementPdf({
      statementId: "stmt-3",
      artifactVersion: 2,
      artifactUri: "",
      periodYearMonth: "2026-06"
    });

    expect(getMock).toHaveBeenCalledWith("/statements/stmt-3/artifact/v2.pdf", {
      responseType: "blob"
    });
    expect(result.fileName).toBe("statement-2026-06-v2.pdf");
  });

  it("returns a friendly message when statement PDF artifact is missing", async () => {
    jest.spyOn(apiClient, "get").mockRejectedValue({
      response: {
        status: 404,
        data: {
          code: "STATEMENT_NOT_FOUND",
          message: "Artifact not found"
        }
      }
    });

    await expect(
      fetchStatementPdf({
        statementId: "stmt-9",
        artifactVersion: 1,
        artifactUri: "/statements/stmt-9/artifact/v1.pdf"
      })
    ).rejects.toThrow("The statement PDF artifact is not available for download yet.");
  });
});
