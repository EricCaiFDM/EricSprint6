import { beforeEach, describe, expect, it, jest } from "@jest/globals";
import { apiClient } from "./api";
import { fetchStatement, fetchStatements, generateStatement } from "./statements";

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
});
